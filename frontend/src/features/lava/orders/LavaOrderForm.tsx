import {
  useRef,
  useState,
} from "react";
import {
  DropdownField,
  InfoField,
  PremiumCard,
  ReceiptPdfModal,
  SectionTitle,
  TextField,
  useToast,
} from "../../../components/ui";
import type {
  LavaClient,
} from "../../../entities/client";
import type {
  LavaService,
} from "../../../entities/service";
import {
  getLavaServicePriceForSize,
} from "../../../entities/service";
import type {
  LavaPaymentMethod,
  LavaWorkOrderPaymentInput,
  LavaWorkOrderPaymentResult,
} from "../../../entities/payment";
import type {
  CreateLavaOrderInput,
  LavaOrderClientMode,
  LavaOrderFormInput,
  LavaProgressStage,
  LavaVehicleSize,
} from "../../../entities/work-order";
import {
  formatBrlCurrency,
  parseBrlCurrency,
} from "../../../shared/lib/currency";
import {
  currencyInputToNumber,
  formatCurrencyInput,
} from "../../../shared/formatters/currencyInput";
import type {
  OpenLavaWorkOrderRepositoryResult,
} from "../../../data/contracts";
import {
  formatBrazilianPhone,
} from "../../../shared/formatters/phoneInput";
import {
  TerminalPaymentModal,
  useTerminalPaymentFlow,
} from "../../payment-terminal";
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

type LavaOrderSubmitPaymentResult = {
  workOrderId: number;
  paymentResult: LavaWorkOrderPaymentResult | null;
};

const GENERAL_RECEIPT_DOCUMENT = "Recibo geral";
const lavaOrderPaymentMethods: LavaPaymentMethod[] = [
  "Dinheiro",
  "Pix",
  "Crédito",
  "Débito",
];

export function LavaOrderForm({
  clients,
  services,
  onCreateOrder,
  onPayOrder,
  onCompleteOrder,
}: {
  clients: LavaClient[];
  services: LavaService[];
  onCreateOrder: (
    order: CreateLavaOrderInput
  ) => Promise<OpenLavaWorkOrderRepositoryResult | null>;
  onPayOrder: (
    payment: LavaWorkOrderPaymentInput
  ) => Promise<LavaWorkOrderPaymentResult | null>;
  onCompleteOrder: (
    workOrderId: number,
    stage: LavaProgressStage
  ) => Promise<boolean>;
}) {
  const activeClients = clients.filter((client) => client.active);
  const activeServices = services.filter((service) => service.active);

  const [form, setForm] = useState<LavaOrderFormInput>({
    clientMode: "registered",
    clientId: activeClients[0]?.id.toString() ?? "",
    guestName: "",
    guestPhone: "",
    guestVehicle: "",
    guestPlate: "",
    serviceId: activeServices[0]?.id.toString() ?? "",
    vehicleSize: "Pequeno",
    paymentMethod: "Dinheiro",
    fiscalDocument: GENERAL_RECEIPT_DOCUMENT,
  });

  const [formError, setFormError] = useState("");
  const [flowMessage, setFlowMessage] = useState("");
  const [cashReceived, setCashReceived] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const submitLockRef = useRef(false);
  const lastOpenedReceiptKeyRef = useRef<string | null>(null);
  const [currentReceipt, setCurrentReceipt] =
    useState<ReceiptDocument | null>(null);
  const [
    pendingPaymentWorkOrderId,
    setPendingPaymentWorkOrderId,
  ] = useState<number | null>(null);
  const terminalPayment =
    useTerminalPaymentFlow<LavaOrderSubmitPaymentResult>();
  const receiptViewer = useReceiptViewer();
  const {
    showErrorToast,
  } = useToast();

  const selectedClient = activeClients.find(
    (client) => client.id.toString() === form.clientId
  );

  const selectedService = activeServices.find(
    (service) => service.id.toString() === form.serviceId
  );

  const isRegisteredClientMode =
    form.clientMode === "registered";
  const appliedServicePrice =
    selectedService
      ? getLavaServicePriceForSize(
          selectedService,
          form.vehicleSize
        )
      : "";
  const orderClientName =
    isRegisteredClientMode
      ? selectedClient?.name ?? ""
      : form.guestName.trim();
  const orderVehicle =
    isRegisteredClientMode
      ? selectedClient?.vehicle ?? ""
      : form.guestVehicle.trim();
  const orderPlate =
    isRegisteredClientMode
      ? selectedClient?.plate ?? ""
      : form.guestPlate.trim().toUpperCase();
  const orderAmount = parseBrlCurrency(
    appliedServicePrice
  );
  const paymentMethod: LavaPaymentMethod =
    form.paymentMethod === "Pendente"
      ? "Dinheiro"
      : form.paymentMethod;
  const cashReceivedAmount =
    currencyInputToNumber(cashReceived);
  const cashChange = Math.max(
    0,
    cashReceivedAmount - orderAmount
  );
  const isCashPayment =
    paymentMethod === "Dinheiro";
  const isPixPayment =
    paymentMethod === "Pix";
  const isTerminalPayment =
    !isCashPayment &&
    !isPixPayment;
  const canSubmit =
    !isSubmitting &&
    (
      !isCashPayment ||
      cashReceivedAmount >= orderAmount
    );

  function updateForm(field: keyof LavaOrderFormInput, value: string) {
    setFormError("");
    setFlowMessage("");

    setForm((current) => ({
      ...current,
      [field]:
        field === "clientMode"
          ? (value as LavaOrderClientMode)
          : field === "vehicleSize"
            ? (value as LavaVehicleSize)
            : field === "guestPhone"
              ? formatBrazilianPhone(value)
              : value,
    }));
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

  async function handleSubmit() {
    if (submitLockRef.current) {
      return;
    }

    if (!selectedService) {
      setFormError("Selecione um serviço ativo para abrir a OS.");
      return;
    }

    if (isRegisteredClientMode && !selectedClient) {
      setFormError("Selecione um cliente ativo para abrir a OS.");
      return;
    }

    if (
      !isRegisteredClientMode &&
      (
        orderClientName.length === 0 ||
        orderVehicle.length === 0 ||
        orderPlate.length === 0
      )
    ) {
      setFormError("Preencha nome, modelo do carro e placa para abrir a OS avulsa.");
      return;
    }

    if (isCashPayment && cashReceivedAmount < orderAmount) {
      setFormError(
        "O valor recebido precisa cobrir o total da OS."
      );
      return;
    }

    submitLockRef.current = true;
    setIsSubmitting(true);
    setFormError("");
    setFlowMessage("");
    const activeService = selectedService;
    const activeClient = selectedClient;

    async function createOrderAndRequestPayment(): Promise<LavaOrderSubmitPaymentResult> {
      let workOrderId = pendingPaymentWorkOrderId;

      if (workOrderId === null) {
        const created = await onCreateOrder({
          clientId: activeClient?.id ?? null,
          clientName: orderClientName,
          clientPhone:
            isRegisteredClientMode
              ? activeClient?.phone
              : form.guestPhone,
          serviceId: activeService.id,

          vehicle: orderVehicle,
          plate: orderPlate,

          service: activeService.name,
          amount: orderAmount,

          vehicleSize: form.vehicleSize,
          paymentMethod: "Pendente",
          fiscalDocument: GENERAL_RECEIPT_DOCUMENT,
        });

        if (!created) {
          throw new Error(
            "Não foi possível abrir a ordem de serviço."
          );
        }

        workOrderId = created.workOrder.id;
        setPendingPaymentWorkOrderId(workOrderId);

      }

      const paymentResult = await onPayOrder({
        workOrderId,
        method: paymentMethod,
        mode: isCashPayment
          ? "cash"
          : isPixPayment
            ? "manual"
            : "terminal",
        document: GENERAL_RECEIPT_DOCUMENT,
        cashReceived: isCashPayment
          ? cashReceivedAmount
          : undefined,
        manualReason: isPixPayment
          ? "Pix confirmado manualmente."
          : undefined,
      });

      if (paymentResult?.approved) {
        const completed = await onCompleteOrder(
          workOrderId,
          "done"
        );

        if (!completed) {
          throw new Error(
            "Pagamento aprovado, mas a OS não foi concluída."
          );
        }
      }

      return {
        workOrderId,
        paymentResult,
      };
    }

    function handleApprovedPayment({
      workOrderId,
      paymentResult,
    }: LavaOrderSubmitPaymentResult) {
      setPendingPaymentWorkOrderId(null);
      setFlowMessage(
        `OS ${workOrderId} aberta, paga e concluída com sucesso. ${
          paymentResult?.message ?? "Pagamento aprovado."
        }`
      );

      setForm((current) => ({
        ...current,
        guestName: "",
        guestPhone: "",
        guestVehicle: "",
        guestPlate: "",
        vehicleSize: "Pequeno",
        paymentMethod: "Dinheiro",
        fiscalDocument: GENERAL_RECEIPT_DOCUMENT,
      }));
      setCashReceived("");

      if (paymentResult) {
        void openPaidWorkOrderReceipt(
          paymentResult
        );
      }
    }

    function handleRejectedPayment({
      workOrderId,
      paymentResult,
    }: LavaOrderSubmitPaymentResult) {
      setFormError(
        paymentResult?.message ??
          "Não foi possível confirmar o pagamento. Confira a comunicação e tente novamente."
      );
      setFlowMessage(
        `OS ${workOrderId} aberta. Corrija o pagamento e tente novamente sem criar outra OS.`
      );
    }

    try {
      if (isTerminalPayment) {
        await terminalPayment.startTerminalPayment({
          method: paymentMethod,
          amount: orderAmount,
          operation: createOrderAndRequestPayment,
          isApproved: (result) =>
            Boolean(
              result.paymentResult?.approved
            ),
          getMessage: (result) =>
            result.paymentResult?.message,
          onApproved: (result) => {
            terminalPayment.closeTerminalPayment();
            handleApprovedPayment(result);
          },
          onRejected: handleRejectedPayment,
        });

        return;
      }

      const submitResult =
        await createOrderAndRequestPayment();

      if (!submitResult.paymentResult) {
        setFormError(
          "Não foi possível confirmar o pagamento. Confira a comunicação e tente novamente."
        );
        return;
      }

      if (!submitResult.paymentResult.approved) {
        setFormError(
          submitResult.paymentResult.message
        );
        setFlowMessage(
          `OS ${submitResult.workOrderId} aberta. Corrija o pagamento e tente novamente sem criar outra OS.`
        );
        return;
      }

      handleApprovedPayment(submitResult);
    } catch (error) {
      setFormError(
        error instanceof Error
          ? error.message
          : "Não foi possível concluir a operação."
      );
    } finally {
      submitLockRef.current = false;
      setIsSubmitting(false);
    }
  }

  return (
    <>
    <div className="flex h-full min-h-0 flex-1 flex-col">
      <div className="grid min-h-0 flex-1 items-stretch gap-4 2xl:grid-cols-[minmax(0,1fr)_340px]">
        <PremiumCard className="h-full">
          <SectionTitle title="Abrir ordem de serviço" />

          <div className="mt-3 grid gap-4">
            <div className="grid grid-cols-2 gap-2 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-1">
              <ClientModeButton
                label="Cliente cadastrado"
                active={isRegisteredClientMode}
                onClick={() =>
                  updateForm("clientMode", "registered")
                }
              />

              <ClientModeButton
                label="Cliente novo / avulso"
                active={!isRegisteredClientMode}
                onClick={() =>
                  updateForm("clientMode", "guest")
                }
              />
            </div>

            {isRegisteredClientMode ? (
              <LavaOrderDropdownField
                label="Cliente"
                value={form.clientId}
                onChange={(value) => updateForm("clientId", value)}
                options={activeClients.map((client) => ({
                  value: client.id.toString(),
                  label: `${client.name} · ${client.vehicle}`,
                }))}
                emptyLabel="Nenhum cliente ativo"
              />
            ) : (
              <div className="grid gap-4">
                <TextField
                  label="Nome do cliente"
                  value={form.guestName}
                  placeholder="Ex: Ana Souza"
                  onChange={(value) => updateForm("guestName", value)}
                />

                <div className="grid gap-4 sm:grid-cols-2">
                  <TextField
                    label="Modelo do carro"
                    value={form.guestVehicle}
                    placeholder="Ex: Toyota Corolla"
                    onChange={(value) => updateForm("guestVehicle", value)}
                  />

                  <TextField
                    label="Placa"
                    value={form.guestPlate}
                    placeholder="ABC-1D23"
                    onChange={(value) => updateForm("guestPlate", value)}
                  />
                </div>

                <TextField
                  label="Telefone"
                  value={form.guestPhone}
                  placeholder="(71) 98716-0075"
                  type="tel"
                  onChange={(value) => updateForm("guestPhone", value)}
                />
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_180px]">
              <LavaOrderDropdownField
                label="Serviço"
                value={form.serviceId}
                onChange={(value) => updateForm("serviceId", value)}
                options={activeServices.map((service) => ({
                  value: service.id.toString(),
                  label: service.name,
                }))}
                emptyLabel="Nenhum serviço ativo"
              />

              <DropdownField
                label="Porte do veículo"
                value={form.vehicleSize}
                options={["Pequeno", "Médio"]}
                onChange={(value) => updateForm("vehicleSize", value)}
              />
            </div>

            <div className="grid gap-4 border-t border-white/[0.08] pt-4">
              <DropdownField
                label="Pagamento"
                value={paymentMethod}
                options={lavaOrderPaymentMethods}
                onChange={(value) =>
                  updateForm("paymentMethod", value)
                }
              />

              {isCashPayment ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  <TextField
                    label="Valor recebido"
                    value={cashReceived}
                    placeholder="R$ 0,00"
                    onChange={(value) => {
                      setFormError("");
                      setFlowMessage("");
                      setCashReceived(
                        formatCurrencyInput(value)
                      );
                    }}
                  />

                  <InfoField
                    label="Troco"
                    value={formatBrlCurrency(cashChange)}
                  />
                </div>
              ) : isTerminalPayment ? (
                <div className="rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-3 py-2 text-sm text-zinc-400">
                  O pagamento será enviado ao terminal configurado.
                </div>
              ) : (
                <div className="rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-3 py-2 text-sm text-zinc-400">
                  Confirme o Pix manualmente para concluir a OS.
                </div>
              )}
            </div>

            {formError && (
              <div className="rounded-[4px] border border-[#D4AF37]/20 bg-[#D4AF37]/10 px-3 py-2 text-sm text-[#F2C94C]">
                {formError}
              </div>
            )}

            {flowMessage && (
              <div className="rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-3 py-2 text-sm text-zinc-300">
                {flowMessage}
              </div>
            )}

            <button
              type="button"
              className="service-action-button btn btn-primary btn-full"
              onClick={handleSubmit}
              disabled={!canSubmit}
            >
              {isSubmitting
                ? "Processando..."
                : pendingPaymentWorkOrderId
                  ? "Tentar pagamento novamente"
                  : "Abrir OS e pagar"}
            </button>
          </div>
        </PremiumCard>

        <PremiumCard className="h-full">
          <SectionTitle title="Resumo da OS" />

          <div className="mt-3 space-y-3">
            <LavaOrderSummaryRow
              label="Cliente"
              value={orderClientName || "A definir"}
            />

            <LavaOrderSummaryRow
              label="Veículo"
              value={
                orderVehicle && orderPlate
                  ? `${orderVehicle} · ${orderPlate}`
                  : "A definir"
              }
            />

            <LavaOrderSummaryRow
              label="Serviço"
              value={selectedService?.name ?? "A definir"}
            />

            <LavaOrderSummaryRow
              label="Porte"
              value={form.vehicleSize}
            />

            <LavaOrderSummaryRow
              label="Total"
              value={appliedServicePrice || "A definir"}
            />

            <LavaOrderSummaryRow
              label="Pagamento"
              value={paymentMethod}
            />

            {isCashPayment ? (
              <LavaOrderSummaryRow
                label="Troco"
                value={formatBrlCurrency(cashChange)}
                highlight
              />
            ) : null}

          </div>
        </PremiumCard>
      </div>
    </div>
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

function ClientModeButton({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={[
        "v7-motion-fast h-9 rounded-[3px] px-3 text-sm font-medium",
        active
          ? "bg-white/[0.08] text-white"
          : "text-zinc-500 hover:bg-white/[0.035] hover:text-zinc-200",
      ].join(" ")}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

function LavaOrderDropdownField({
  label,
  value,
  options,
  emptyLabel,
  onChange,
}: {
  label: string;
  value: string;
  options: {
    value: string;
    label: string;
  }[];
  emptyLabel: string;
  onChange: (value: string) => void;
}) {
  const selectedLabel =
    options.find((option) => option.value === value)?.label ?? emptyLabel;

  return (
    <DropdownField
      label={label}
      value={selectedLabel}
      options={options.length > 0 ? options.map((option) => option.label) : [emptyLabel]}
      onChange={(selectedOptionLabel) => {
        const selectedOption = options.find(
          (option) => option.label === selectedOptionLabel
        );

        if (selectedOption) {
          onChange(selectedOption.value);
        }
      }}
    />
  );
}

function LavaOrderSummaryRow({
  label,
  value,
  highlight = false,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-white/[0.07] pb-3 last:border-b-0 last:pb-0">
      <span className="text-sm text-zinc-500">{label}</span>

      <span
        className={[
          "max-w-[190px] truncate text-right text-sm font-semibold",
          highlight ? "text-[#F2C94C]" : "text-zinc-100",
        ].join(" ")}
      >
        {value}
      </span>
    </div>
  );
}


