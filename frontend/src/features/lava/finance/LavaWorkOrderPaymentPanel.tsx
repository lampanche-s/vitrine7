import {
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Check,
  Eye,
  X,
} from "lucide-react";

import {
  AnimatedModal,
  DropdownField,
  EmptyState,
  InfoField,
  PremiumCard,
  ReceiptPdfModal,
  SectionTitle,
  TextField,
  useToast,
} from "../../../components/ui";

import type {
  LavaPaymentMethod,
  LavaWorkOrderPaymentInput,
  LavaWorkOrderPaymentMode,
  LavaWorkOrderPaymentResult,
} from "../../../entities/payment";

import type {
  LavaProgressVehicle,
} from "../../../entities/work-order";

import {
  formatCurrencyInput,
  currencyInputToNumber,
} from "../../../shared/formatters/currencyInput";

import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";
import {
  downloadReceiptPdf,
  type ReceiptDocument,
} from "../../../shared/receipts/receiptDocument";
import {
  loadOfficialReceipt,
} from "../../../shared/receipts/officialReceipt";
import {
  useReceiptViewer,
} from "../../../shared/receipts/useReceiptViewer";
import {
  TerminalPaymentModal,
  useTerminalPaymentFlow,
} from "../../payment-terminal";

const paymentMethods: LavaPaymentMethod[] = [
  "Crédito",
  "Débito",
  "Pix",
  "Dinheiro",
];

const paymentModes: Array<{
  label: string;
  value: LavaWorkOrderPaymentMode;
}> = [
  {
    label: "Terminal",
    value: "terminal",
  },
  {
    label: "Dinheiro",
    value: "cash",
  },
  {
    label: "Manual",
    value: "manual",
  },
];

const GENERAL_RECEIPT_DOCUMENT = "Recibo geral";

export function LavaWorkOrderPaymentPanel({
  workOrders,
  onPay,
}: {
  workOrders: LavaProgressVehicle[];
  onPay: (
    input: LavaWorkOrderPaymentInput
  ) => Promise<LavaWorkOrderPaymentResult | null>;
}) {
  const [selectedWorkOrderId, setSelectedWorkOrderId] =
    useState<number | null>(null);
  const [method, setMethod] =
    useState<LavaPaymentMethod>("Crédito");
  const [mode, setMode] =
    useState<LavaWorkOrderPaymentMode>("terminal");
  const [cashReceived, setCashReceived] =
    useState("");
  const [manualReason, setManualReason] =
    useState("");
  const [message, setMessage] = useState("");
  const [isSubmitting, setIsSubmitting] =
    useState(false);
  const [currentReceipt, setCurrentReceipt] =
    useState<ReceiptDocument | null>(null);
  const lastOpenedReceiptKeyRef = useRef<string | null>(null);
  const terminalPayment =
    useTerminalPaymentFlow<LavaWorkOrderPaymentResult | null>();
  const receiptViewer = useReceiptViewer();
  const {
    showErrorToast,
  } = useToast();

  const selectedWorkOrder = useMemo(
    () =>
      workOrders.find(
        (workOrder) =>
          workOrder.id === selectedWorkOrderId
      ) ?? null,
    [
      selectedWorkOrderId,
      workOrders,
    ]
  );

  const payableWorkOrders = workOrders.filter(
    (workOrder) =>
      workOrder.paymentStatus !== "APPROVED"
  );

  const amount =
    selectedWorkOrder?.amount ?? 0;
  const cashReceivedAmount =
    currencyInputToNumber(cashReceived);
  const cashChange = Math.max(
    0,
    cashReceivedAmount - amount
  );
  const isPaid =
    selectedWorkOrder?.paymentStatus === "APPROVED" ||
    Boolean(selectedWorkOrder?.paidAt);
  const canSubmit =
    Boolean(selectedWorkOrder) &&
    !isSubmitting &&
    !isPaid &&
    (
      mode !== "cash" ||
      cashReceivedAmount >= amount
    ) &&
    (
      mode !== "manual" ||
      method === "Pix" ||
      manualReason.trim().length >= 3
    );
  const isTerminalPayment =
    mode === "terminal" &&
    (
      method === "Crédito" ||
      method === "Débito" ||
      method === "Cartão"
    );

  function closeModal() {
    if (isSubmitting) {
      return;
    }

    setSelectedWorkOrderId(null);
    setMethod("Crédito");
    setMode("terminal");
    setCashReceived("");
    setManualReason("");
    setMessage("");
  }

  function openModal(workOrderId: number) {
    setSelectedWorkOrderId(workOrderId);
    setMessage("");
  }

  function changeMode(nextMode: string) {
    const paymentMode =
      paymentModes.find(
        (item) => item.label === nextMode
      )?.value ?? "terminal";

    setMode(paymentMode);

    if (paymentMode === "cash") {
      setMethod("Dinheiro");
      return;
    }

    if (paymentMode === "terminal") {
      setMethod(
        method === "Débito"
          ? "Débito"
          : "Crédito"
      );
      return;
    }

    if (method === "Dinheiro") {
      setMethod("Crédito");
    }
  }

  async function openPaidWorkOrderReceipt(
    result: LavaWorkOrderPaymentResult
  ) {
    const checkoutId =
      result.workOrder.checkoutId;

    if (!checkoutId) {
      showErrorToast(
        new Error(
          "A ordem de serviço concluída não possui checkout vinculado."
        ),
        {
          title:
            "Não foi possível carregar o comprovante",
        }
      );

      return;
    }

    const receiptKey =
      `checkout:${checkoutId}`;

    if (
      lastOpenedReceiptKeyRef.current === receiptKey &&
      receiptViewer.isOpen
    ) {
      return;
    }

    try {
      const receipt =
        await loadOfficialReceipt(
          checkoutId
        );

      lastOpenedReceiptKeyRef.current =
        receiptKey;

      setCurrentReceipt(receipt);
      receiptViewer.openReceipt(receipt);
    } catch (error) {
      showErrorToast(error, {
        title:
          "Não foi possível carregar o comprovante",
      });
    }
  }

  async function submitPayment() {
    if (!selectedWorkOrder || !canSubmit) {
      return;
    }

    const paymentInput = {
      workOrderId: selectedWorkOrder.id,
      method,
      mode,
      document: GENERAL_RECEIPT_DOCUMENT,
      cashReceived:
        mode === "cash"
          ? cashReceivedAmount
          : undefined,
      manualReason:
        mode === "manual"
          ? method === "Pix"
            ? "Pix confirmado manualmente."
            : manualReason
          : undefined,
    };

    if (isTerminalPayment) {
      setIsSubmitting(true);

      await terminalPayment.startTerminalPayment({
        method,
        amount,
        operation: () => onPay(paymentInput),
        isApproved: (result) =>
          Boolean(result?.approved),
        getMessage: (result) =>
          result?.message,
        onApproved: (result) => {
          if (result) {
            setMessage(result.message);
            terminalPayment.closeTerminalPayment();
            closeModal();
            void openPaidWorkOrderReceipt(result);
          }
        },
        onRejected: (result) => {
          setMessage(
            result?.message ??
              "Não foi possível confirmar o pagamento."
          );
        },
      });

      setIsSubmitting(false);
      return;
    }

    setIsSubmitting(true);

    const result = await onPay(paymentInput);

    setIsSubmitting(false);

    if (!result) {
      return;
    }

    setMessage(result.message);

    if (result.approved) {
      closeModal();
      void openPaidWorkOrderReceipt(
        result
      );
    }
  }

  return (
    <>
      <SectionTitle title="Pagamentos de OS" />

      <PremiumCard className="v7-card-fill" contentClassName="v7-card-content">
        <div className="v7-card-header">
          <SectionTitle title="Ordens abertas para pagamento" />
        </div>

        <div className="v7-list-scroll premium-scroll mt-3 pr-1">
          {payableWorkOrders.length === 0 ? (
            <EmptyState message="Nenhuma OS aberta aguardando pagamento." />
          ) : null}

          {payableWorkOrders.map((workOrder) => (
            <div
              key={workOrder.id}
              className="grid min-h-[76px] grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-4 border-b border-white/[0.06] py-3 last:border-b-0"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-white">
                  OS {workOrder.id} · {workOrder.clientName}
                </p>

                <p className="mt-1 truncate text-sm text-zinc-500">
                  {workOrder.vehicle} · {workOrder.plate} · {workOrder.service}
                </p>

                <p className="mt-1 truncate text-xs text-zinc-600">
                  {workOrder.checkoutStatus === "PAYMENT_FAILED"
                    ? "Pagamento recusado"
                    : workOrder.prepared
                      ? "Checkout preparado"
                      : "Aberta"}
                </p>
              </div>

              <div className="text-right">
                <p className="text-sm font-semibold text-white">
                  {formatBrlCurrency(workOrder.amount ?? 0)}
                </p>
              </div>

              <button
                type="button"
                onClick={() => openModal(workOrder.id)}
                className="grid h-9 w-9 place-items-center rounded-[4px] border border-white/[0.08] text-zinc-500 transition hover:border-white/[0.16] hover:bg-white/[0.04] hover:text-white"
                aria-label={`Pagar OS ${workOrder.id}`}
                title="Pagar OS"
              >
                <Eye
                  className="h-4 w-4"
                  aria-hidden="true"
                />
              </button>
            </div>
          ))}
        </div>
      </PremiumCard>

      <AnimatedModal
        open={Boolean(selectedWorkOrder)}
        onClose={closeModal}
        labelledBy="lava-payment-title"
        closeOnBackdrop={!isSubmitting}
        closeOnEscape={!isSubmitting}
        backdropClassName="z-[320] p-4"
        panelClassName="max-h-[calc(100dvh-32px)] w-full max-w-[560px] overflow-y-auto rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] p-5 shadow-[0_10px_34px_rgba(0,0,0,0.28)]"
      >
        {selectedWorkOrder ? (
          <div>
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="lava-payment-title"
                  className="text-base font-semibold uppercase text-white"
                >
                  OS {selectedWorkOrder.id}
                </h2>

                <p className="mt-2 text-sm text-zinc-500">
                  {selectedWorkOrder.clientName} · {selectedWorkOrder.vehicle}
                </p>
              </div>

              <button
                type="button"
                onClick={closeModal}
                className="grid h-9 w-9 shrink-0 place-items-center rounded-[4px] border border-white/[0.08] text-zinc-500 transition hover:bg-white/[0.05] hover:text-white"
                aria-label="Fechar pagamento"
              >
                <X className="h-4 w-4" aria-hidden="true" />
              </button>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <InfoField label="Placa" value={selectedWorkOrder.plate} />
              <InfoField label="Serviço" value={selectedWorkOrder.service} />
              <InfoField label="Valor" value={formatBrlCurrency(amount)} />
              <InfoField
                label="Checkout"
                value={selectedWorkOrder.checkoutStatus ?? "A preparar"}
              />
            </div>

            {!isPaid ? (
              <div className="mt-5 grid gap-4 border-t border-white/[0.08] pt-5">
                <div className="grid gap-4 sm:grid-cols-2">
                  <DropdownField
                    label="Modo"
                    value={
                      paymentModes.find(
                        (item) => item.value === mode
                      )?.label ?? "Terminal"
                    }
                    options={paymentModes.map((item) => item.label)}
                    onChange={changeMode}
                  />

                  <DropdownField
                    label="Pagamento"
                    value={method}
                    options={
                      mode === "cash"
                        ? ["Dinheiro"]
                        : mode === "terminal"
                          ? paymentMethods.filter(
                              (item) =>
                                item !== "Dinheiro" &&
                                item !== "Pix"
                            )
                          : paymentMethods.filter(
                              (item) => item !== "Dinheiro"
                            )
                    }
                    onChange={(value) =>
                      setMethod(value as LavaPaymentMethod)
                    }
                  />
                </div>

                {mode === "cash" ? (
                  <div className="grid gap-4 sm:grid-cols-2">
                    <TextField
                      label="Valor recebido"
                      value={cashReceived}
                      placeholder="R$ 0,00"
                      onChange={(value) =>
                        setCashReceived(
                          formatCurrencyInput(value)
                        )
                      }
                    />

                    <InfoField
                      label="Troco"
                      value={formatBrlCurrency(cashChange)}
                    />
                  </div>
                ) : null}

                {mode === "manual" && method !== "Pix" ? (
                  <TextField
                    label="Motivo"
                    value={manualReason}
                    placeholder="Ex: comprovante conferido"
                    onChange={setManualReason}
                  />
                ) : null}

                {message ? (
                  <div className="rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-3 py-2 text-sm text-zinc-300">
                    {message}
                  </div>
                ) : null}

                  <button
                  type="button"
                  className="service-action-button btn btn-primary btn-full"
                  onClick={() => void submitPayment()}
                  disabled={!canSubmit}
                >
                  <Check className="h-3.5 w-3.5" aria-hidden="true" />
                  {isSubmitting
                    ? "Processando pagamento..."
                    : "Confirmar pagamento"}
                </button>
              </div>
            ) : (
              <div className="mt-5 rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-3 py-3 text-sm text-zinc-400">
                O pagamento desta OS já foi aprovado.
              </div>
            )}
          </div>
        ) : null}
      </AnimatedModal>

      <TerminalPaymentModal
        {...terminalPayment.paymentModal}
        onClose={terminalPayment.closeTerminalPayment}
        onContinue={terminalPayment.closeTerminalPayment}
      />
      <ReceiptPdfModal
        open={receiptViewer.isOpen}
        title={currentReceipt?.title ?? "Comprovante"}
        previewUrl={receiptViewer.previewUrl}
        printHtml={receiptViewer.printHtml}
        onDownload={
          currentReceipt
            ? () => void downloadReceiptPdf(currentReceipt)
            : undefined
        }
        onClose={receiptViewer.closeReceipt}
      />
    </>
  );
}
