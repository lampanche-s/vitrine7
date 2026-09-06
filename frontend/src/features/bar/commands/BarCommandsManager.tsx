import {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Bell,
  Check,
  CircleDollarSign,
  Minus,
  Pencil,
  Plus,
  Printer,
  ReceiptText,
  RotateCcw,
  Trash2,
  X,
} from "lucide-react";

import {
  AnimatedModal,
  Button,
  DropdownField,
  PageActions,
  ReceiptPdfModal,
  SearchField,
  TextField,
  useToast,
} from "../../../components/ui";


import {
  repositories,
} from "../../../data/repositories";

import type {
  Client,
} from "../../../entities/client";
import type { Employee } from "../../../entities/employee";

import type {
  BarCatalogItem,
} from "../../../entities/catalog-item";

import {
  getBarCommandItemCount,
  getBarCommandSummary,
  getBarCommandTotal,
} from "../../../entities/command";

import type {
  AddBarCommandItemInput,
  BarCommand,
  BarCommandStatus,
  CloseBarCommandInput,
  OpenBarCommandInput,
  VoucherBarCommandInput,
} from "../../../entities/command";

import type {
  BarPaymentMethod,
  BarReceiptDocument,
} from "../../../entities/sale-history";

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
  useReceiptPrinter,
} from "../../../shared/receipts/useReceiptPrinter";
import {
  currencyInputToNumber,
  formatCurrencyInput,
} from "../../../shared/formatters/currencyInput";

import {
  getCurrentShortTime,
} from "../../../shared/lib/date-time";
import {
  shouldRequestVehicleDetails,
} from "../close-command/close-command.rules";
import {
  canPrintPrePaymentNote,
} from "./pre-payment-note.rules";
import {
  canPrintItems,
  canPrintLine,
  canPrintServices,
  operationalPrintLineLabel,
} from "./operational-print.rules";

import {
  TerminalPaymentModal,
  useTerminalPaymentFlow,
} from "../../payment-terminal";

type CommandCatalogProduct = {
  id: number;
  name: string;
  type: "ITEM" | "SERVICE";
  price: number;
  stockEnabled: boolean;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
};

type PaymentPartDraft = {
  method: BarPaymentMethod;
  amount: string;
  cashReceived: string;
};

type NewCommandSelection =
  | { type: "CLIENT"; id: number; name: string }
  | { type: "EMPLOYEE"; id: number; name: string };

function createPaymentDraft(total = 0): PaymentPartDraft {
  return {
    method: "Dinheiro",
    amount: total > 0
      ? formatCurrencyInput(String(Math.round(total * 100)))
      : "",
    cashReceived: "",
  };
}

function normalizeClientName(value: string) {
  return value.trim().replace(/\s+/g, " ").toLocaleLowerCase("pt-BR");
}

const commandPaymentMethods: BarPaymentMethod[] = [
  "Dinheiro",
  "Pix",
  "Crédito",
  "Débito",
];

const GENERAL_RECEIPT_DOCUMENT: BarReceiptDocument =
  "Recibo geral";

export function BarCommandsManager({
  commands,
  catalogEntries,
  onOpenCommand,
  onSetCommandStatus,
  onPrintPrePaymentNote,
  onPrintItems,
  onPrintServices,
  onPrintLine,
  onAddCommandItem,
  onUpdateCommandItemQuantity,
  onRemoveCommandItem,
  onCancelCommand,
  onCloseCommand,
  onCloseVoucher,
}: {
  commands: BarCommand[];
  catalogEntries: BarCatalogItem[];

  onOpenCommand: (
    input: OpenBarCommandInput
  ) => Promise<BarCommand | null>;

  onSetCommandStatus: (
    commandId: number,
    status: BarCommandStatus
  ) => Promise<boolean>;
  onPrintPrePaymentNote: (
    commandId: number
  ) => Promise<boolean>;
  onPrintItems: (commandId: number) => Promise<boolean>;
  onPrintServices: (commandId: number) => Promise<boolean>;
  onPrintLine: (
    commandId: number,
    lineId: number
  ) => Promise<boolean>;

  onAddCommandItem: (
    commandId: number,
    input: AddBarCommandItemInput
  ) => Promise<boolean>;

  onUpdateCommandItemQuantity: (
    commandId: number,
    itemKey: string,
    quantity: number,
    unitPrice?: number
  ) => Promise<boolean>;

  onRemoveCommandItem: (
    commandId: number,
    itemKey: string
  ) => Promise<boolean>;

  onCancelCommand: (
    commandId: number
  ) => Promise<boolean>;

  onCloseCommand: (
    input: CloseBarCommandInput
  ) => Promise<BarCommand | null>;
  onCloseVoucher: (input: VoucherBarCommandInput) => Promise<BarCommand | null>;
}) {
  const [searchTerm, setSearchTerm] =
    useState("");

  const [catalogSearch, setCatalogSearch] =
    useState("");

  const [
    selectedCommandId,
    setSelectedCommandId,
  ] = useState<number | null>(
    commands[0]?.id ?? null
  );

  const [isCreateModalOpen, setIsCreateModalOpen] =
    useState(false);

  const [isStockAlertsOpen, setIsStockAlertsOpen] =
    useState(false);

  const [isCloseModalOpen, setIsCloseModalOpen] =
    useState(false);

  const [
    commandPendingCancellation,
    setCommandPendingCancellation,
  ] = useState<BarCommand | null>(null);

  const [newCommandName, setNewCommandName] =
    useState("");
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [newCommandSelection, setNewCommandSelection] = useState<NewCommandSelection | null>(null);
  const [isUnregisteredClientConfirmOpen, setIsUnregisteredClientConfirmOpen] =
    useState(false);

  const [paymentParts, setPaymentParts] =
    useState<PaymentPartDraft[]>([createPaymentDraft()]);
  const [isClosingCommand, setIsClosingCommand] =
    useState(false);
  const [isPrintingPrePaymentNote, setIsPrintingPrePaymentNote] =
    useState(false);
  const [operationalPrintPending, setOperationalPrintPending] =
    useState<string | null>(null);
  const [vehicleName, setVehicleName] = useState("");
  const [vehiclePlate, setVehiclePlate] = useState("");
  const [pendingService, setPendingService] = useState<CommandCatalogProduct | null>(null);
  const [isVehicleModalOpen, setIsVehicleModalOpen] = useState(false);
  const [currentReceipt, setCurrentReceipt] =
    useState<ReceiptDocument | null>(null);
  const [
    editingItemPrice,
    setEditingItemPrice,
  ] = useState<{
    commandId: number;
    itemKey: string;
  } | null>(null);

  const [
    itemPriceDraft,
    setItemPriceDraft,
  ] = useState("");

  const [
    isSavingItemPrice,
    setIsSavingItemPrice,
  ] = useState(false);
  const lastOpenedReceiptKeyRef = useRef<string | null>(null);
  const terminalPayment =
    useTerminalPaymentFlow<BarCommand | null>();
  const receiptViewer = useReceiptViewer();
  const receiptPrinter = useReceiptPrinter();
  const {
    showToast,
    showErrorToast,
  } = useToast();

  useEffect(() => {
    let active = true;

    void repositories.clients.list()
      .then((loadedClients) => {
        if (active) {
          setClients(loadedClients.filter((client) => client.active));
        }
      })
      .catch(() => {
        if (active) {
          setClients([]);
        }
      });
    void repositories.employees.list()
      .then((loaded) => { if (active) setEmployees(loaded); })
      .catch(() => { if (active) setEmployees([]); });

    return () => {
      active = false;
    };
  }, []);

  const selectedCommand =
    commands.find(
      (command) =>
        command.id === selectedCommandId
    ) ??
    commands[0] ??
    null;

  const selectedCommandTotal =
    selectedCommand
      ? getBarCommandTotal(selectedCommand)
      : 0;
  const selectedCommandTotalCents =
    Math.round(selectedCommandTotal * 100);

  const nonCashTotalCents = paymentParts.reduce(
    (sum, part) => {
      if (part.method === "Dinheiro") {
        return sum;
      }

      return (
        sum +
        Math.round(
          currencyInputToNumber(part.amount) * 100
        )
      );
    },
    0
  );

  const paymentPartValues = paymentParts.map((part, index) => {
    if (part.method !== "Dinheiro") {
      return {
        method: part.method,
        amount: currencyInputToNumber(part.amount),
        cashReceived: undefined,
      };
    }

    const cashReceived =
      currencyInputToNumber(part.cashReceived);

    const cashReceivedCents = Math.max(
      0,
      Math.round(cashReceived * 100)
    );

    const previousCashReceivedCents = paymentParts
      .slice(0, index)
      .filter((previousPart) => previousPart.method === "Dinheiro")
      .reduce(
        (sum, previousPart) =>
          sum + Math.max(
            0,
            Math.round(
              currencyInputToNumber(previousPart.cashReceived) * 100
            )
          ),
        0
      );

    const remainingForCashCents = Math.max(
      0,
      selectedCommandTotalCents -
        nonCashTotalCents -
        previousCashReceivedCents
    );

    const amountCents = Math.min(
      cashReceivedCents,
      remainingForCashCents
    );

    return {
      method: part.method,
      amount: amountCents / 100,
      cashReceived,
    };
  });

  const paymentPartsTotal = paymentPartValues.reduce(
    (sum, part) => sum + part.amount,
    0
  );

  const paymentPartsTotalCents =
    Math.round(paymentPartsTotal * 100);

  const paymentRemainingCents =
    selectedCommandTotalCents - paymentPartsTotalCents;

  const totalCashChange = paymentPartValues.reduce((sum, part) => {
    if (part.method !== "Dinheiro") {
      return sum;
    }

    return sum + Math.max(0, (part.cashReceived ?? 0) - part.amount);
  }, 0);

  const paymentMissingAmount =
    Math.max(0, paymentRemainingCents) / 100;

  const paymentExcessAmount =
    Math.max(0, -paymentRemainingCents) / 100;

  const paymentAmountsValid =
    paymentPartValues.every(
    (part) => part.amount > 0
  );

  const paymentPartsValid =
    paymentPartValues.length >= 1 &&
    paymentPartValues.length <= 3 &&
    paymentAmountsValid &&
    paymentRemainingCents === 0;
  const canCloseSelectedCommand =
    !isClosingCommand && paymentPartsValid;
  const terminalPaymentPart = paymentPartValues.find((part) =>
    part.method === "Crédito" ||
    part.method === "Débito" ||
    part.method === "Cartão"
  );
  const isTerminalPayment = Boolean(terminalPaymentPart);
  const shouldRequestSelectedCommandVehicle =
    selectedCommand
      ? shouldRequestVehicleDetails(selectedCommand, catalogEntries)
      : false;

  const vehicleDetailsValid = (() => {
    const normalizedName = vehicleName.trim().replace(/\s+/g, " ");
    const plate = vehiclePlate.replace(/[^A-Za-z0-9]/g, "").toUpperCase();
    return normalizedName.length > 0
      && normalizedName.length <= 120
      && (/^[A-Z]{3}[0-9]{4}$/.test(plate) || /^[A-Z]{3}[0-9][A-Z][0-9]{2}$/.test(plate));
  })();

  const catalogProducts =
    useMemo<CommandCatalogProduct[]>(
      () => catalogEntries.map((entry) => ({
        id: entry.id,
        name: entry.name,
        type: entry.type,
        price: entry.price,
        stockEnabled: entry.stockEnabled,
        stockQuantity: entry.stockQuantity,
        minimumStockQuantity:
          entry.minimumStockQuantity,
      })),
      [catalogEntries]
    );

  const stockAlertItems = useMemo(
    () =>
      catalogEntries
        .filter(
          (entry) =>
            entry.type === "ITEM" &&
            entry.stockEnabled &&
            (entry.stockQuantity ?? 0) <=
              (entry.minimumStockQuantity ?? 0)
        )
        .sort((left, right) => {
          const quantityDifference =
            (left.stockQuantity ?? 0) -
            (right.stockQuantity ?? 0);

          return quantityDifference !== 0
            ? quantityDifference
            : left.name.localeCompare(
                right.name,
                "pt-BR"
              );
        }),
    [catalogEntries]
  );

  const zeroStockItems = stockAlertItems.filter(
    (entry) => (entry.stockQuantity ?? 0) === 0
  );

  const lowStockItems = stockAlertItems.filter(
    (entry) => (entry.stockQuantity ?? 0) > 0
  );

  const filteredCommands = useMemo(() => {
    const normalizedSearch =
      searchTerm.trim().toLowerCase();

    return commands.filter((command) => {
      if (normalizedSearch.length === 0) {
        return true;
      }

      return (
        command.name
          .toLowerCase()
          .includes(normalizedSearch) ||
        getBarCommandSummary(command)
          .toLowerCase()
          .includes(normalizedSearch)
      );
    });
  }, [commands, searchTerm]);

  const normalizedCatalogSearch =
    catalogSearch
      .trim()
      .toLocaleLowerCase("pt-BR");

  const filteredCatalogProducts =
    normalizedCatalogSearch.length === 0
      ? []
      : catalogProducts.filter(
          (product) =>
            product.name
              .toLocaleLowerCase("pt-BR")
              .includes(
                normalizedCatalogSearch
              ) ||
            product.type
              .toLocaleLowerCase("pt-BR")
              .includes(
                normalizedCatalogSearch
              )
        );

  function closeCreateModal() {
    setIsCreateModalOpen(false);
    setIsUnregisteredClientConfirmOpen(false);
    setNewCommandSelection(null);
  }

  function openCloseModal() {
    if (!selectedCommand) {
      return;
    }

    setPaymentParts([createPaymentDraft(getBarCommandTotal(selectedCommand))]);
    setVehicleName(selectedCommand.vehicleName ?? "");
    setVehiclePlate(selectedCommand.vehiclePlate ?? "");
    setIsCloseModalOpen(true);
  }

  function closeCloseModal() {
    if (isClosingCommand) {
      return;
    }

    setIsCloseModalOpen(false);
    setPaymentParts([createPaymentDraft()]);
    setVehicleName("");
    setVehiclePlate("");
  }

  function updatePaymentPart(
    index: number,
    update: Partial<PaymentPartDraft>
  ) {
    setPaymentParts((current) => current.map((part, currentIndex) =>
      currentIndex === index ? { ...part, ...update } : part
    ));
  }

  function addPaymentPart() {
    if (paymentParts.length >= 3) {
      return;
    }

    const remainingAmount =
      Math.max(0, paymentRemainingCents) / 100;

    setPaymentParts((current) => [
      ...current,
      createPaymentDraft(remainingAmount),
    ]);
  }

  function removePaymentPart(index: number) {
    if (paymentParts.length <= 1) {
      return;
    }

    setPaymentParts((current) => current.filter((_, currentIndex) => currentIndex !== index));
  }

  async function openClosedCommandReceipt(
    closedCommand: BarCommand
  ) {
    const checkoutId =
      closedCommand.checkoutId;

    if (!checkoutId) {
      showErrorToast(
        new Error(
          "A comanda concluída não possui checkout vinculado."
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

  function closeCancelModal() {
    setCommandPendingCancellation(null);
  }

  async function createCommand(selection: NewCommandSelection | null) {
    const createdCommand = await onOpenCommand({
      name: newCommandName,
      clientId: selection?.type === "CLIENT" ? selection.id : null,
      employeeId: selection?.type === "EMPLOYEE" ? selection.id : null,
      openedAt: getCurrentShortTime(),
    });

    if (!createdCommand) {
      return;
    }

    setSelectedCommandId(createdCommand.id);
    setNewCommandName("");
    setNewCommandSelection(null);
    setIsUnregisteredClientConfirmOpen(false);
    setIsCreateModalOpen(false);
  }

  async function handleOpenCommand() {
    const normalizedName = normalizeClientName(newCommandName);
    if (!normalizedName) {
      showErrorToast(new Error("Informe o nome da comanda."), {
        title: "Nome obrigatório",
      });
      return;
    }

    if (newCommandSelection) {
      await createCommand(newCommandSelection);
      return;
    }

    setIsUnregisteredClientConfirmOpen(true);
  }

  async function handleAddProduct(
    product: CommandCatalogProduct
  ) {
    if (
      product.type === "ITEM" &&
      product.stockEnabled &&
      (product.stockQuantity ?? 0) <= 0
    ) {
      showErrorToast(
        new Error(
          `O item ${product.name} está sem estoque.`
        ),
        { title: "Estoque zerado" }
      );
      return;
    }

    if (!selectedCommand) {
      return;
    }

    if (
      product.type === "SERVICE"
      && selectedCommand.clientId == null
      && (!selectedCommand.vehicleName || !selectedCommand.vehiclePlate)
    ) {
      setVehicleName("");
      setVehiclePlate("");
      setPendingService(product);
      setIsVehicleModalOpen(true);
      return;
    }

    const added =
      await onAddCommandItem(
        selectedCommand.id,
        {
          catalogItemId: product.id,
          quantity: 1,
        }
      );

    if (!added) {
      return;
    }

    setCatalogSearch("");
  }

  function closeVehicleModal() {
    setIsVehicleModalOpen(false);
    setPendingService(null);
    setVehicleName("");
    setVehiclePlate("");
  }

  async function confirmPendingService() {
    if (!selectedCommand || !pendingService || !vehicleDetailsValid) {
      return;
    }

    const added = await onAddCommandItem(selectedCommand.id, {
      catalogItemId: pendingService.id,
      quantity: 1,
      vehicleName: vehicleName.trim().replace(/\s+/g, " "),
      vehiclePlate: vehiclePlate.replace(/[^A-Za-z0-9]/g, "").toUpperCase(),
    });
    if (!added) {
      return;
    }
    closeVehicleModal();
    setCatalogSearch("");
  }

  async function handleChangeStatus() {
    if (!selectedCommand) {
      return;
    }

    if (selectedCommand.status === "open") {
      openCloseModal();
      return;
    }

    const nextStatus: BarCommandStatus = "open";

    const updated =
      await onSetCommandStatus(
        selectedCommand.id,
        nextStatus
      );

    if (!updated) {
      return;
    }

    setCatalogSearch("");
  }

  async function handlePrintPrePaymentNote() {
    if (!selectedCommand || isPrintingPrePaymentNote) {
      return;
    }

    setIsPrintingPrePaymentNote(true);

    try {
      const accepted = await onPrintPrePaymentNote(selectedCommand.id);

      if (accepted) {
        showToast({
          title: "Impressão enviada",
          description:
            "A conferência de consumo foi enviada para a fila de impressão.",
          variant: "success",
        });
      }
    } finally {
      setIsPrintingPrePaymentNote(false);
    }
  }

  async function handleOperationalPrint(
    pendingKey: string,
    operation: () => Promise<boolean>,
    description: string
  ) {
    if (operationalPrintPending) {
      return;
    }

    setOperationalPrintPending(pendingKey);

    try {
      const accepted = await operation();
      if (accepted) {
        showToast({
          title: "Impressão enviada",
          description,
          variant: "success",
        });
      }
    } finally {
      setOperationalPrintPending(null);
    }
  }

  function handlePrintItems() {
    if (!selectedCommand || !canPrintItems(selectedCommand)) {
      return;
    }

    void handleOperationalPrint(
      "items",
      () => onPrintItems(selectedCommand.id),
      "Os itens foram enviados para a fila de impressão."
    );
  }

  function handlePrintServices() {
    if (!selectedCommand || !canPrintServices(selectedCommand)) {
      return;
    }

    void handleOperationalPrint(
      "services",
      () => onPrintServices(selectedCommand.id),
      "Os serviços foram enviados para a fila de impressão."
    );
  }

  function handlePrintLine(item: BarCommand["items"][number]) {
    if (
      !selectedCommand ||
      !canPrintLine(selectedCommand, item) ||
      item.lineId === undefined
    ) {
      return;
    }

    void handleOperationalPrint(
      `line:${item.lineId}`,
      () => onPrintLine(selectedCommand.id, item.lineId as number),
      item.entryType === "SERVICE"
        ? "O serviço foi enviado para a fila de impressão."
        : "O item foi enviado para a fila de impressão."
    );
  }

  async function handleCancelCommand() {
    if (!commandPendingCancellation) {
      return;
    }

    const cancelled =
      await onCancelCommand(
        commandPendingCancellation.id
      );

    if (!cancelled) {
      return;
    }

    if (
      selectedCommandId ===
      commandPendingCancellation.id
    ) {
      const remainingCommand =
        commands.find(
          (command) =>
            command.id !==
            commandPendingCancellation.id
        );

      setSelectedCommandId(
        remainingCommand?.id ?? null
      );
    }

    setCommandPendingCancellation(null);
  }

  async function handleCloseCommand() {
    if (!selectedCommand || isClosingCommand || !paymentPartsValid) {
      return;
    }

    const commandToClose = selectedCommand;
    const commandTotal = getBarCommandTotal(commandToClose);
    const closedAt = getCurrentShortTime();
    const closeInput: CloseBarCommandInput = {
      commandId: commandToClose.id,
      payments: paymentPartValues,
      document: GENERAL_RECEIPT_DOCUMENT,
      time: closedAt,
      ...(shouldRequestSelectedCommandVehicle
        ? { vehicleName, vehiclePlate }
        : {}),
    };

    function handleClosedCommand(closedCommand: BarCommand) {
      const remainingCommand = commands.find(
        (command) => command.id !== closedCommand.id
      );

      setSelectedCommandId(remainingCommand?.id ?? null);
      setIsCloseModalOpen(false);
      setPaymentParts([createPaymentDraft()]);
      void openClosedCommandReceipt(closedCommand);
    }

    if (isTerminalPayment && terminalPaymentPart) {
      setIsClosingCommand(true);

      await terminalPayment.startTerminalPayment({
        method: terminalPaymentPart.method,
        amount: terminalPaymentPart.amount || commandTotal,
        operation: () => onCloseCommand(closeInput),
        isApproved: (closedCommand): closedCommand is BarCommand =>
          Boolean(closedCommand),
        getMessage: (closedCommand) =>
          closedCommand
            ? "Pagamentos aprovados."
            : "Não foi possível concluir os pagamentos.",
        onApproved: (closedCommand) => {
          if (closedCommand) {
            handleClosedCommand(closedCommand);
          }
        },
      });

      setIsClosingCommand(false);
      return;
    }

    setIsClosingCommand(true);
    const closedCommand = await onCloseCommand(closeInput);
    setIsClosingCommand(false);

    if (closedCommand) {
      handleClosedCommand(closedCommand);
    }
  }

  async function handleCloseVoucher() {
    if (!selectedCommand || selectedCommand.employeeId == null || isClosingCommand) return;
    setIsClosingCommand(true);
    const closed = await onCloseVoucher({
      commandId: selectedCommand.id,
      ...(shouldRequestSelectedCommandVehicle ? { vehicleName, vehiclePlate } : {}),
    });
    setIsClosingCommand(false);
    if (closed) {
      const remaining = commands.find((command) => command.id !== closed.id);
      setSelectedCommandId(remaining?.id ?? null);
      setIsCloseModalOpen(false);
      setVehicleName("");
      setVehiclePlate("");
    }
  }

  function openItemPriceEditor(
    commandId: number,
    item: BarCommand["items"][number]
  ) {
    setEditingItemPrice({
      commandId,
      itemKey: item.key,
    });

    setItemPriceDraft(
      formatCurrencyInput(
        String(
          Math.round(
            item.unitPrice * 100
          )
        )
      )
    );
  }

  function closeItemPriceEditor() {
    if (isSavingItemPrice) {
      return;
    }

    setEditingItemPrice(null);
    setItemPriceDraft("");
  }

  async function saveItemPrice(
    commandId: number,
    item: BarCommand["items"][number]
  ) {
    const unitPrice =
      currencyInputToNumber(
        itemPriceDraft
      );

    if (unitPrice <= 0) {
      showErrorToast(
        new Error(
          "Informe um preço maior que zero."
        ),
        {
          title: "Preço inválido",
        }
      );

      return;
    }

    setIsSavingItemPrice(true);

    try {
      const updated =
        await onUpdateCommandItemQuantity(
          commandId,
          item.key,
          item.quantity,
          unitPrice
        );

      if (updated) {
        setEditingItemPrice(null);
        setItemPriceDraft("");
      }
    } finally {
      setIsSavingItemPrice(false);
    }
  }

  return (
    <>
      <section className="commands-screen flex h-full min-h-0 flex-col">
        <PageActions>
          <Button
            variant="primary"
            leadingIcon={<Plus />}
            onClick={() => setIsCreateModalOpen(true)}
          >
            Nova comanda
          </Button>

          <button
            type="button"
            onClick={() => setIsStockAlertsOpen(true)}
            className="v7-motion-fast v7-pressable relative grid h-[var(--control-height)] w-[var(--control-height)] place-items-center rounded-[var(--control-radius)] border border-[var(--border-subtle)] bg-[var(--surface-raised)] text-[var(--text-base)] hover:border-[var(--border-hover)] hover:bg-[var(--surface-hover)]"
            aria-label={`Alertas de estoque: ${stockAlertItems.length}`}
            title="Alertas de estoque"
          >
            <Bell className="h-4 w-4" aria-hidden="true" />
            {stockAlertItems.length > 0 ? (
              <span className="absolute -right-1.5 -top-1.5 grid min-h-5 min-w-5 place-items-center rounded-full bg-[var(--color-danger)] px-1 text-[10px] font-bold text-white">
                {stockAlertItems.length > 99
                  ? "99+"
                  : stockAlertItems.length}
              </span>
            ) : null}
          </button>
        </PageActions>

        <div className="mt-4 w-full shrink-0">
          <SearchField
            value={searchTerm}
            onChange={setSearchTerm}
            placeholder="Buscar nome da comanda ou item..."
          />
        </div>

        <div className="commands-viewport-grid mt-4 grid min-h-0 flex-1 gap-4 xl:grid-cols-[405px_minmax(0,1fr)]">
          <div className="command-scroll-area command-scroll-area--commands command-scroll-area--fill v7-list-scroll flex flex-col gap-3 pr-1">
            {filteredCommands.length === 0 && (
              <div className="py-4 text-sm text-[var(--text-muted)]">
                Nenhuma comanda aberta.
              </div>
            )}

            {filteredCommands.map(
              (command) => {
                const selected =
                  command.id ===
                  selectedCommand?.id;

                return (
                  <button
                    key={command.id}
                    type="button"
                    onClick={() => {
                      setSelectedCommandId(
                        command.id
                      );

                      setCatalogSearch("");
                    }}
                    className={[
                      "min-h-[96px] w-full rounded-[5px] border px-4 py-3 text-left transition-colors",
                      selected
                        ? "border-[var(--color-accent-border)] bg-[var(--surface-active)]"
                        : "border-[var(--border-subtle)] bg-[var(--surface-control)] hover:border-[var(--border-hover)] hover:bg-[var(--surface-raised)]",
                    ].join(" ")}
                  >
                    <div className="min-w-0">
                      <p className="truncate text-base font-semibold text-[var(--text-base)]">
                        {command.name}
                      </p>
                    </div>

                    <div className="mt-4 flex items-center justify-between gap-3 text-xs">
                      <span className="text-[var(--text-subtle)]">
                        {getBarCommandItemCount(
                          command
                        )}{" "}
                        {getBarCommandItemCount(
                          command
                        ) === 1
                          ? "item"
                          : "itens"}
                      </span>

                      <span
                        className={
                          selected
                            ? "font-semibold text-[var(--color-accent)]"
                            : "font-semibold text-[var(--text-base)]"
                        }
                      >
                        {formatBrlCurrency(
                          getBarCommandTotal(
                            command
                          )
                        )}
                      </span>
                    </div>
                  </button>
                );
              }
            )}
          </div>

          {!selectedCommand && (
            <div className="grid min-h-[420px] place-items-center rounded-[4px] border border-dashed border-[var(--border-subtle)] bg-[var(--surface-control)] p-6 text-center xl:h-full">
              <div>
                <ReceiptText className="mx-auto h-7 w-7 text-[var(--text-subtle)]" aria-hidden="true" />

                <p className="mt-3 text-sm font-medium text-[var(--text-base)]">
                  Nenhuma comanda selecionada
                </p>

                <p className="mt-1 text-sm text-[var(--text-subtle)]">
                  Abra ou selecione uma comanda para gerenciar seus itens.
                </p>
              </div>
            </div>
          )}

          {selectedCommand && (
            <div className="flex min-h-0 min-w-0 flex-col rounded-[5px] border border-[var(--border-subtle)] bg-[var(--surface-card)] p-4">
              <div className="relative z-20 flex shrink-0 flex-col gap-4 border-b border-[var(--border-subtle)] pb-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0">
                  <h4 className="truncate text-lg font-semibold text-[var(--text-base)]">
                    {selectedCommand.name}
                  </h4>

                  <p className="mt-1 text-sm text-[var(--text-muted)]">
                    Aberta às{" "}
                    {selectedCommand.openedAt}
                  </p>
                </div>

                <div className="relative w-full lg:min-w-0 lg:flex-1">
                  <SearchField
                    value={catalogSearch}
                    onChange={setCatalogSearch}
                    disabled={selectedCommand.status !== "open"}
                    placeholder={
                      selectedCommand.status !== "open"
                        ? "Retorne ao consumo para adicionar itens"
                        : "Buscar item ou serviço..."
                    }
                  />

                  {selectedCommand.status === "open" &&
                  normalizedCatalogSearch.length > 0 ? (
                    <div className="absolute left-0 right-0 top-[calc(100%+6px)] z-50 max-h-72 overflow-y-auto rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card-strong)] p-1 shadow-[var(--shadow-dropdown)]">
                      {filteredCatalogProducts.length === 0 ? (
                        <div className="px-3 py-4 text-center text-sm text-[var(--text-muted)]">
                          Nenhum item ou serviço encontrado.
                        </div>
                      ) : (
                        filteredCatalogProducts.map(
                          (product) => (
                            <button
                              key={product.id}
                              type="button"
                              disabled={
                                product.type === "ITEM" &&
                                product.stockEnabled &&
                                (product.stockQuantity ?? 0) <= 0
                              }
                              onClick={() =>
                                void handleAddProduct(
                                  product
                                )
                              }
                              className="flex min-h-14 w-full items-center justify-between gap-4 rounded-[3px] px-3 py-2 text-left transition-colors hover:bg-[var(--surface-hover)] disabled:cursor-not-allowed disabled:opacity-45"
                            >
                              <div className="min-w-0">
                                <p className="truncate text-sm font-medium text-[var(--text-base)]">
                                  {product.name}
                                </p>

                                <p className="mt-1 truncate text-xs text-[var(--text-muted)]">
                                  {product.type === "SERVICE"
                                    ? "Serviço"
                                    : product.stockEnabled
                                      ? `Item · Estoque ${product.stockQuantity ?? 0}`
                                      : "Item · Estoque não controlado"}
                                </p>
                              </div>

                              <div className="flex shrink-0 items-center gap-3">
                                <span className="text-sm font-semibold text-[var(--color-accent)]">
                                  {formatBrlCurrency(product.price)}
                                </span>

                                <span className="grid h-8 min-w-8 place-items-center rounded-[4px] border border-[var(--border-subtle)] px-2 text-[var(--text-base)]">
                                  {product.type === "ITEM" &&
                                  product.stockEnabled &&
                                  (product.stockQuantity ?? 0) <= 0 ? (
                                    <span className="text-[10px] font-semibold text-[var(--color-danger)]">
                                      Zerado
                                    </span>
                                  ) : (
                                    <Plus
                                      className="h-4 w-4"
                                      aria-hidden="true"
                                    />
                                  )}
                                </span>
                              </div>
                            </button>
                          )
                        )
                      )}
                    </div>
                  ) : null}
                </div>
              </div>

              <div className="mt-4 flex min-h-0 flex-1 flex-col">
                <div className="flex items-center justify-between gap-3">
                  <h5 className="text-sm font-semibold text-[var(--text-base)]">
                    Itens da comanda
                  </h5>

                  <span className="text-sm font-semibold text-[var(--color-accent)]">
                    {formatBrlCurrency(
                      getBarCommandTotal(
                        selectedCommand
                      )
                    )}
                  </span>
                </div>

                <div className="command-scroll-area command-scroll-area--items command-scroll-area--items-fill v7-list-scroll mt-4 flex flex-col pr-1">
                  {selectedCommand.items.length ===
                    0 && (
                    <div className="py-4 text-sm text-[var(--text-subtle)]">
                      Nenhum item adicionado.
                    </div>
                  )}

                  {selectedCommand.items.map(
                    (item) => (
                      <div
                        key={item.key}
                        className="flex min-h-[72px] items-center justify-between gap-3 border-b border-[var(--border-subtle)] py-2 last:border-b-0"
                      >
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-[var(--text-base)]">
                            {item.name}
                          </p>

                          {editingItemPrice?.commandId ===
                            selectedCommand.id &&
                          editingItemPrice.itemKey === item.key ? (
                            <>
                              <div className="mt-1 flex items-center gap-1.5">
                                <input
                                  autoFocus
                                  type="text"
                                  inputMode="numeric"
                                  value={itemPriceDraft}
                                  disabled={isSavingItemPrice}
                                  onChange={(event) =>
                                    setItemPriceDraft(
                                      formatCurrencyInput(
                                        event.target.value
                                      )
                                    )
                                  }
                                  onKeyDown={(event) => {
                                    if (event.key === "Enter") {
                                      event.preventDefault();

                                      void saveItemPrice(
                                        selectedCommand.id,
                                        item
                                      );
                                    }

                                    if (event.key === "Escape") {
                                      closeItemPriceEditor();
                                    }
                                  }}
                                  className="h-9 w-[138px] rounded-[var(--control-radius)] border border-[var(--color-accent-border)] bg-[var(--surface-control)] px-3 text-base font-semibold text-[var(--color-accent)] outline-none focus:border-[var(--color-accent)]"
                                  aria-label={`Novo preço de ${item.name}`}
                                />

                                <Button
                                  size="icon"
                                  variant="primary"
                                  disabled={isSavingItemPrice}
                                  leadingIcon={<Check />}
                                  onClick={() =>
                                    void saveItemPrice(
                                      selectedCommand.id,
                                      item
                                    )
                                  }
                                  aria-label="Salvar preço"
                                  title="Salvar preço"
                                />

                                <Button
                                  size="icon"
                                  variant="ghost"
                                  disabled={isSavingItemPrice}
                                  leadingIcon={<X />}
                                  onClick={closeItemPriceEditor}
                                  aria-label="Cancelar edição"
                                  title="Cancelar"
                                />
                              </div>

                              <p className="mt-1 text-xs text-[var(--text-subtle)]">
                                Aplicado somente nesta comanda
                              </p>
                            </>
                          ) : (
                            <>
                              <Button
                                size="compact"
                                variant="ghost"
                                disabled={selectedCommand.status !== "open"}
                                leadingIcon={<Pencil />}
                                onClick={() =>
                                  openItemPriceEditor(
                                    selectedCommand.id,
                                    item
                                  )
                                }
                                className="-ml-2 mt-1 h-9 px-2 text-base font-semibold text-[var(--color-accent)] hover:bg-[var(--color-accent-soft)] hover:text-[var(--color-accent-hover)]"
                                aria-label={`Alterar preço de ${item.name}`}
                                title="Alterar preço nesta comanda"
                              >
                                {formatBrlCurrency(
                                  item.unitPrice
                                )}
                              </Button>

                              <p className="text-xs text-[var(--text-subtle)]">
                                {selectedCommand.status !== "open"
                                  ? "Retorne ao consumo para alterar"
                                  : "Preço unitário · clique para alterar"}
                              </p>
                            </>
                          )}
                        </div>

                        <div className="flex items-center gap-2">
                          {operationalPrintLineLabel(item) ? (
                            <Button
                              size="icon"
                              variant="secondary"
                              disabled={
                                !canPrintLine(selectedCommand, item) ||
                                operationalPrintPending !== null
                              }
                              onClick={() => handlePrintLine(item)}
                              leadingIcon={<Printer />}
                              aria-label={operationalPrintLineLabel(item) as string}
                              title={operationalPrintLineLabel(item) as string}
                            />
                          ) : null}

                          <Button
                            size="icon"
                            variant="secondary"
                            disabled={selectedCommand.status !== "open"}
                            onClick={() =>
                              void onUpdateCommandItemQuantity(
                                selectedCommand.id,
                                item.key,
                                item.quantity - 1
                              )
                            }
                            leadingIcon={<Minus />}
                            aria-label="Diminuir quantidade"
                            title="Diminuir"
                          />

                          <span className="min-w-7 text-center text-sm font-semibold text-[var(--text-base)]">
                            {item.quantity}
                          </span>

                          <Button
                            size="icon"
                            variant="secondary"
                            disabled={selectedCommand.status !== "open"}
                            onClick={() =>
                              void onUpdateCommandItemQuantity(
                                selectedCommand.id,
                                item.key,
                                item.quantity + 1
                              )
                            }
                            leadingIcon={<Plus />}
                            aria-label="Aumentar quantidade"
                            title="Aumentar"
                          />

                          <Button
                            size="icon"
                            variant="danger"
                            disabled={selectedCommand.status !== "open"}
                            onClick={() =>
                              void onRemoveCommandItem(
                                selectedCommand.id,
                                item.key
                              )
                            }
                            leadingIcon={<Trash2 />}
                            aria-label="Remover item"
                            title="Remover"
                          />
                        </div>
                      </div>
                    )
                  )}
                </div>
              </div>

              <div className="mt-4 flex shrink-0 border-t border-[var(--border-subtle)] pt-4">
                <div className="flex w-full flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <Button
                      variant="secondary"
                      disabled={
                        !canPrintServices(selectedCommand) ||
                        operationalPrintPending !== null
                      }
                      leadingIcon={<Printer />}
                      onClick={handlePrintServices}
                      className="w-full sm:w-auto"
                    >
                      {operationalPrintPending === "services"
                        ? "Enviando..."
                        : "Imprimir serviços"}
                    </Button>

                    <Button
                      variant="secondary"
                      disabled={
                        !canPrintItems(selectedCommand) ||
                        operationalPrintPending !== null
                      }
                      leadingIcon={<Printer />}
                      onClick={handlePrintItems}
                      className="w-full sm:w-auto"
                    >
                      {operationalPrintPending === "items"
                        ? "Enviando..."
                        : "Imprimir itens"}
                    </Button>
                  </div>

                  <div className="flex flex-col gap-2 sm:flex-row">
                    {selectedCommand.status === "open" ? (
                      <Button
                        variant="secondary"
                        disabled={
                          !canPrintPrePaymentNote(selectedCommand) ||
                          isClosingCommand ||
                          isPrintingPrePaymentNote
                        }
                        leadingIcon={<Printer />}
                        onClick={() =>
                          void handlePrintPrePaymentNote()
                        }
                        className="w-full sm:w-auto"
                      >
                        {isPrintingPrePaymentNote
                          ? "Enviando..."
                          : "Imprimir nota"}
                      </Button>
                    ) : null}

                    <Button
                      variant={
                        selectedCommand.status === "open"
                          ? "primary"
                          : "secondary"
                      }
                      leadingIcon={
                        selectedCommand.status === "open" ? (
                          <CircleDollarSign />
                        ) : (
                          <RotateCcw />
                        )
                      }
                      onClick={() => void handleChangeStatus()}
                      className="w-full sm:w-auto"
                    >
                      {selectedCommand.status === "open"
                        ? "Receber"
                        : "Retornar ao consumo"}
                    </Button>

                    {selectedCommand.status === "awaitingPayment" ? (
                      <Button
                        variant="primary"
                        leadingIcon={<Check />}
                        onClick={openCloseModal}
                        className="w-full sm:w-auto"
                      >
                        Fechar e receber
                      </Button>
                    ) : null}
                    <Button
                      variant="danger"
                      leadingIcon={<Trash2 />}
                      onClick={() =>
                        setCommandPendingCancellation(selectedCommand)
                      }
                      className="w-full sm:w-auto"
                    >
                      Cancelar comanda
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>


      <AnimatedModal
        open={isStockAlertsOpen}
        onClose={() => setIsStockAlertsOpen(false)}
        labelledBy="stock-alerts-title"
        backdropClassName="z-[230] p-4"
        panelClassName="w-full max-w-lg overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3
              id="stock-alerts-title"
              className="text-lg font-semibold text-[var(--text-base)]"
            >
              Alertas de estoque
            </h3>
            <p className="mt-1 text-sm text-[var(--text-muted)]">
              Itens zerados ou no limite mínimo configurado.
            </p>
          </div>
          <Button
            size="icon"
            variant="ghost"
            leadingIcon={<X />}
            aria-label="Fechar alertas"
            onClick={() => setIsStockAlertsOpen(false)}
          />
        </div>

        <div className="v7-list-scroll premium-scroll mt-4 max-h-[420px] space-y-5 overflow-y-auto pr-1">
          {stockAlertItems.length === 0 ? (
            <div className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-4 py-5 text-center text-sm text-[var(--text-muted)]">
              Nenhum item com estoque baixo.
            </div>
          ) : null}

          {zeroStockItems.length > 0 ? (
            <section>
              <h4 className="text-xs font-semibold uppercase tracking-wide text-[var(--color-danger)]">
                Estoque zerado
              </h4>
              <div className="mt-2 divide-y divide-[var(--border-subtle)] rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3">
                {zeroStockItems.map((entry) => (
                  <div
                    key={entry.id}
                    className="flex items-center justify-between gap-4 py-3"
                  >
                    <span className="truncate text-sm font-medium text-[var(--text-base)]">
                      {entry.name}
                    </span>
                    <span className="shrink-0 text-xs font-semibold text-[var(--color-danger)]">
                      0 disponíveis
                    </span>
                  </div>
                ))}
              </div>
            </section>
          ) : null}

          {lowStockItems.length > 0 ? (
            <section>
              <h4 className="text-xs font-semibold uppercase tracking-wide text-[var(--color-accent)]">
                Estoque baixo
              </h4>
              <div className="mt-2 divide-y divide-[var(--border-subtle)] rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3">
                {lowStockItems.map((entry) => (
                  <div
                    key={entry.id}
                    className="flex items-center justify-between gap-4 py-3"
                  >
                    <span className="truncate text-sm font-medium text-[var(--text-base)]">
                      {entry.name}
                    </span>
                    <span className="shrink-0 text-xs text-[var(--text-muted)]">
                      {entry.stockQuantity ?? 0} disponíveis · mínimo {entry.minimumStockQuantity ?? 0}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      </AnimatedModal>

      <AnimatedModal
        open={isCreateModalOpen}
        onClose={closeCreateModal}
        labelledBy="bar-command-create-title"
        describedBy="bar-command-create-description"
        backdropClassName="inset-0 z-[200] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
      >
        <div className="flex items-start justify-between gap-4 border-b border-[var(--border-subtle)] p-5">
          <div>
            <h3
              id="bar-command-create-title"
              className="text-lg font-semibold uppercase tracking-[0.04em] text-[var(--text-base)]"
            >
              Nova comanda
            </h3>

            <p
              id="bar-command-create-description"
              className="mt-1 text-sm text-[var(--text-muted)]"
            >
              Selecione um cliente ou funcionário, ou informe um nome para uma comanda avulsa.
            </p>
          </div>

          <Button
            size="icon"
            variant="ghost"
            onClick={closeCreateModal}
            leadingIcon={<X />}
            aria-label="Fechar modal"
            title="Fechar"
          />
        </div>

        <div className="p-5">
          <label className="block">
            <span className="mb-2 block text-xs font-medium uppercase tracking-normal text-[var(--text-subtle)]">
              Nome da comanda
            </span>

            <input
              value={newCommandName}
              id="bar-command-name"
              name="commandName"
              autoFocus
              onChange={(event) => {
                const value = event.target.value;
                setNewCommandName(value);
                if (newCommandSelection && normalizeClientName(newCommandSelection.name) !== normalizeClientName(value)) {
                  setNewCommandSelection(null);
                }
              }}
              placeholder="Ex.: Mesa 04, João ou Aniversário"
              className="h-11 w-full rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-sm text-[var(--text-base)] outline-none transition placeholder:text-[var(--text-subtle)] focus:border-[var(--border-hover)]"
            />
          </label>

          {newCommandName.trim() ? (
            <div className="mt-2 max-h-40 overflow-y-auto rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)]">
              {[...clients.map((client) => ({ type: "CLIENT" as const, id: client.id, name: client.name })),
                ...employees.map((employee) => ({ type: "EMPLOYEE" as const, id: employee.id, name: employee.name }))]
                .filter((option) => normalizeClientName(option.name).includes(normalizeClientName(newCommandName)))
                .slice(0, 6)
                .map((option) => (
                  <button
                    key={`${option.type}:${option.id}`}
                    type="button"
                    className="flex w-full items-center justify-between gap-3 border-b border-[var(--border-subtle)] px-3 py-2 text-left text-sm text-[var(--text-base)] last:border-b-0 hover:bg-[var(--surface-hover)]"
                    onClick={() => {
                      setNewCommandName(option.name);
                      setNewCommandSelection(option);
                    }}
                  >
                    <span>{option.name}</span>
                    {option.type === "EMPLOYEE" ? (
                      <span className="rounded-full border border-[var(--border-subtle)] px-2 py-0.5 text-[10px] uppercase tracking-wide text-[var(--text-muted)]">Funcionário</span>
                    ) : null}
                  </button>
                ))}
            </div>
          ) : null}
        </div>

        <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
          <Button
            variant="secondary"
            onClick={closeCreateModal}
          >
            Cancelar
          </Button>

          <Button
            variant="primary"
            onClick={() => void handleOpenCommand()}
          >
            Abrir comanda
          </Button>
        </div>
      </AnimatedModal>

      <AnimatedModal
        open={isUnregisteredClientConfirmOpen}
        onClose={() => setIsUnregisteredClientConfirmOpen(false)}
        labelledBy="unregistered-client-title"
        backdropClassName="inset-0 z-[210] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
      >
        <div className="p-5">
          <h3 id="unregistered-client-title" className="text-lg font-semibold text-[var(--text-base)]">Cliente não cadastrado</h3>
          <p className="mt-2 text-sm leading-6 text-[var(--text-muted)]">
            “{newCommandName.trim()}” não é um cliente registrado. Deseja criar uma comanda avulsa?
          </p>
        </div>
        <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
          <Button variant="secondary" onClick={() => setIsUnregisteredClientConfirmOpen(false)}>Cancelar</Button>
          <Button variant="primary" onClick={() => void createCommand(null)}>Criar comanda avulsa</Button>
        </div>
      </AnimatedModal>

      <AnimatedModal
        open={isVehicleModalOpen && Boolean(pendingService)}
        onClose={closeVehicleModal}
        labelledBy="bar-command-vehicle-title"
        backdropClassName="inset-0 z-[210] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
      >
        <div className="border-b border-[var(--border-subtle)] p-5">
          <h3 id="bar-command-vehicle-title" className="text-lg font-semibold text-[var(--text-base)]">Identificação do veículo</h3>
          <p className="mt-2 text-sm text-[var(--text-muted)]">Informe o veículo para adicionar {pendingService?.name ?? "o serviço"}.</p>
        </div>
        <div className="grid gap-4 p-5 sm:grid-cols-2">
          <TextField label="Veículo" value={vehicleName} onChange={setVehicleName} placeholder="Ex.: Onix prata" />
          <TextField label="Placa" value={vehiclePlate} onChange={setVehiclePlate} placeholder="AAA1234" />
        </div>
        <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
          <Button variant="secondary" onClick={closeVehicleModal}>Cancelar</Button>
          <Button variant="primary" disabled={!vehicleDetailsValid} onClick={() => void confirmPendingService()}>Adicionar serviço</Button>
        </div>
      </AnimatedModal>

      <AnimatedModal
        open={
          isCloseModalOpen &&
          Boolean(selectedCommand)
        }
        onClose={closeCloseModal}
        labelledBy="bar-command-close-title"
        closeOnBackdrop={!isClosingCommand}
        closeOnEscape={!isClosingCommand}
        backdropClassName="inset-0 z-[200] p-4"
        panelClassName="w-full max-w-md overflow-visible rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
      >
        {selectedCommand ? (
          <>
            <div className="flex items-start justify-between gap-4 border-b border-[var(--border-subtle)] p-5">
              <div className="min-w-0">
                <h3
                  id="bar-command-close-title"
                  className="truncate text-lg font-semibold uppercase tracking-[0.04em] text-[var(--text-base)]"
                >
                  Fechar {selectedCommand.name}
                </h3>

              </div>

              <Button
                size="icon"
                variant="ghost"
                onClick={closeCloseModal}
                leadingIcon={<X />}
                aria-label="Fechar modal"
                title="Fechar"
              />
            </div>

            <div className="space-y-4 p-5">
              {shouldRequestSelectedCommandVehicle ? (
                <div className="grid gap-4 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3 sm:grid-cols-2">
                  <TextField label="Veículo (obrigatório para serviço avulso)" value={vehicleName} onChange={setVehicleName} placeholder="Ex.: Onix prata" />
                  <TextField label="Placa (obrigatória para serviço avulso)" value={vehiclePlate} onChange={setVehiclePlate} placeholder="AAA1234" />
                </div>
              ) : null}
              <div className="grid grid-cols-2 gap-2 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3 sm:grid-cols-4">
                <div>
                  <span className="block text-[11px] uppercase tracking-wide text-[var(--text-subtle)]">
                    Total
                  </span>
                  <strong className="mt-1 block text-sm text-[var(--text-base)]">
                    {formatBrlCurrency(selectedCommandTotal)}
                  </strong>
                </div>

                <div>
                  <span className="block text-[11px] uppercase tracking-wide text-[var(--text-subtle)]">
                    Informado
                  </span>
                  <strong className="mt-1 block text-sm text-[var(--text-base)]">
                    {formatBrlCurrency(paymentPartsTotal)}
                  </strong>
                </div>

                <div>
                  <span className="block text-[11px] uppercase tracking-wide text-[var(--text-subtle)]">
                    Falta pagar
                  </span>
                  <strong className="mt-1 block text-sm text-[var(--text-base)]">
                    {formatBrlCurrency(paymentMissingAmount)}
                  </strong>
                </div>

                <div>
                  <span className="block text-[11px] uppercase tracking-wide text-[var(--text-subtle)]">
                    Troco
                  </span>
                  <strong className="mt-1 block text-sm text-[var(--text-base)]">
                    {formatBrlCurrency(totalCashChange)}
                  </strong>
                </div>
              </div>

              {paymentParts.map((part, index) => {
                return (
                  <div key={index} className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-semibold uppercase text-[var(--text-subtle)]">Pagamento {index + 1}</span>
                      {paymentParts.length > 1 ? (
                        <Button size="icon" variant="ghost" leadingIcon={<Trash2 />} aria-label="Remover forma de pagamento" onClick={() => removePaymentPart(index)} />
                      ) : null}
                    </div>
                    <div className="mt-3 grid gap-4 sm:grid-cols-2">
                      <DropdownField
                        label="Forma"
                        value={part.method}
                        options={commandPaymentMethods}
                        onChange={(value) => updatePaymentPart(index, {
                          method: value as BarPaymentMethod,
                          cashReceived: "",
                        })}
                      />

                      {part.method === "Dinheiro" ? (
                        <TextField
                          label="Valor recebido"
                          value={part.cashReceived}
                          placeholder="R$ 0,00"
                          onChange={(value) =>
                            updatePaymentPart(index, {
                              cashReceived: formatCurrencyInput(value),
                            })
                          }
                        />
                      ) : (
                        <TextField
                          label="Valor"
                          value={part.amount}
                          placeholder="R$ 0,00"
                          onChange={(value) =>
                            updatePaymentPart(index, {
                              amount: formatCurrencyInput(value),
                            })
                          }
                        />
                      )}

                    </div>
                  </div>
                );
              })}

              <div className="flex items-center justify-between gap-3">
                {paymentRemainingCents < 0 ? (
                  <div
                    className="text-sm leading-5 text-[var(--color-danger)]"
                    aria-live="polite"
                  >
                    O valor informado excede o total em{" "}
                    <strong>
                      {formatBrlCurrency(paymentExcessAmount)}
                    </strong>
                    .
                  </div>
                ) : null}
                {paymentParts.length < 3 ? (
                  <Button onClick={addPaymentPart}>
                    {paymentParts.length === 1
                      ? "Dividir pagamento"
                      : "Adicionar outra forma"}
                  </Button>
                ) : null}
              </div>
            </div>
            {selectedCommand.employeeId != null && selectedCommand.status === "open" ? (
              <div className="border-t border-[var(--border-subtle)] px-5 py-4">
                <Button
                  variant="secondary"
                  disabled={isClosingCommand || selectedCommand.items.length === 0}
                  onClick={() => void handleCloseVoucher()}
                >
                  {isClosingCommand ? "Registrando Vale..." : "Registrar como Vale"}
                </Button>
                <p className="mt-2 text-xs text-[var(--text-muted)]">O Vale cobre o valor integral da comanda e não registra entrada no caixa.</p>
              </div>
            ) : null}
            <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
              <Button
                variant="secondary"
                disabled={isClosingCommand || receiptPrinter.isPrinting}
                onClick={closeCloseModal}
              >
                Voltar
              </Button>

              <Button
                variant="primary"
                disabled={!canCloseSelectedCommand}
                leadingIcon={<Check />}
                onClick={() => void handleCloseCommand()}
              >
                {isClosingCommand
                  ? "Processando pagamento..."
                  : isTerminalPayment
                    ? "Cobrar e concluir"
                    : "Concluir pagamento"}
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>

      <AnimatedModal
        open={Boolean(commandPendingCancellation)}
        onClose={closeCancelModal}
        labelledBy="bar-command-cancel-title"
        describedBy="bar-command-cancel-description"
        backdropClassName="inset-0 z-[210] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[4px] border border-[var(--color-danger-border)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
      >
        {commandPendingCancellation ? (
          <>
            <div className="p-5">
              <h3
                id="bar-command-cancel-title"
                className="text-lg font-semibold uppercase tracking-[0.04em] text-[var(--text-base)]"
              >
                Cancelar comanda
              </h3>

              <p
                id="bar-command-cancel-description"
                className="mt-2 text-sm leading-6 text-[var(--text-muted)]"
              >
                A comanda “
                {commandPendingCancellation.name}”
                será removida sem gerar uma venda no histórico.
              </p>
            </div>

            <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
              <Button
                variant="secondary"
                onClick={closeCancelModal}
              >
                Voltar
              </Button>

                  <Button
                    variant="danger"
                onClick={() => void handleCancelCommand()}
              >
                Cancelar comanda
              </Button>
            </div>
          </>
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
        isPrinting={receiptPrinter.isPrinting}
        onPrint={
          currentReceipt?.checkoutId
            ? () => void receiptPrinter.printReceipt(currentReceipt.checkoutId as string)
            : undefined
        }
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
