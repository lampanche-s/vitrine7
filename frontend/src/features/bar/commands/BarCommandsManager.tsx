import {
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
  ReceiptText,
  RotateCcw,
  Trash2,
  X,
} from "lucide-react";

import {
  AnimatedModal,
  Button,
  DropdownField,
  InfoField,
  PageActions,
  ReceiptPdfModal,
  SearchField,
  TextField,
  useToast,
} from "../../../components/ui";

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
  onAddCommandItem,
  onUpdateCommandItemQuantity,
  onRemoveCommandItem,
  onCancelCommand,
  onCloseCommand,
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

  const [paymentMethod, setPaymentMethod] =
    useState<BarPaymentMethod>("Dinheiro");
  const [cashReceived, setCashReceived] =
    useState("");
  const [isClosingCommand, setIsClosingCommand] =
    useState(false);
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
    showErrorToast,
  } = useToast();

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
  const cashReceivedAmount =
    currencyInputToNumber(cashReceived);
  const cashChange = Math.max(
    0,
    cashReceivedAmount - selectedCommandTotal
  );
  const canCloseSelectedCommand =
    !isClosingCommand &&
    (
      paymentMethod !== "Dinheiro" ||
      cashReceivedAmount >= selectedCommandTotal
    );
  const isTerminalPayment =
    paymentMethod === "Crédito" ||
    paymentMethod === "Débito" ||
    paymentMethod === "Cartão";

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
  }

  function closeCloseModal() {
    if (isClosingCommand) {
      return;
    }

    setIsCloseModalOpen(false);
    setCashReceived("");
  }

  function changePaymentMethod(value: string) {
    setPaymentMethod(value as BarPaymentMethod);
    setCashReceived("");
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

  async function handleOpenCommand() {
    const createdCommand =
      await onOpenCommand({
        name: newCommandName,
        openedAt: getCurrentShortTime(),
      });

    if (!createdCommand) {
      return;
    }

    setSelectedCommandId(
      createdCommand.id
    );

    setNewCommandName("");
    setIsCreateModalOpen(false);

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

  async function handleChangeStatus() {
    if (!selectedCommand) {
      return;
    }

    const nextStatus: BarCommandStatus =
      selectedCommand.status === "open"
        ? "awaitingPayment"
        : "open";

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
    if (!selectedCommand || isClosingCommand) {
      return;
    }

    const commandToClose =
      selectedCommand;
    const commandTotal =
      getBarCommandTotal(commandToClose);
    const cashReceivedSnapshot =
      paymentMethod === "Dinheiro"
        ? cashReceivedAmount
        : undefined;
    const closedAt = getCurrentShortTime();
    const closeInput = {
      commandId: commandToClose.id,
      payment: paymentMethod,
      document: GENERAL_RECEIPT_DOCUMENT,
      cashReceived: cashReceivedSnapshot,
      time: closedAt,
    };

    function handleClosedCommand(
      closedCommand: BarCommand
    ) {
      const remainingCommand =
        commands.find(
          (command) =>
            command.id !== closedCommand.id
        );

      setSelectedCommandId(
        remainingCommand?.id ?? null
      );

      setIsCloseModalOpen(false);
      setCashReceived("");

      void openClosedCommandReceipt(
        closedCommand
      );
    }

    if (isTerminalPayment) {
      setIsClosingCommand(true);

      await terminalPayment.startTerminalPayment({
        method: paymentMethod,
        amount: commandTotal,
        operation: () =>
          onCloseCommand(closeInput),
        isApproved: (
          closedCommand
        ): closedCommand is BarCommand =>
          Boolean(closedCommand),
        getMessage: (closedCommand) =>
          closedCommand
            ? "Pagamento aprovado."
            : "Não foi possível confirmar o pagamento na maquininha.",
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

    const closedCommand =
      await onCloseCommand(closeInput);

    setIsClosingCommand(false);

    if (!closedCommand) {
      return;
    }

    handleClosedCommand(closedCommand);
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
                    disabled={
                      selectedCommand.status ===
                      "awaitingPayment"
                    }
                    placeholder={
                      selectedCommand.status ===
                      "awaitingPayment"
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
                                disabled={
                                  selectedCommand.status ===
                                  "awaitingPayment"
                                }
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
                                {selectedCommand.status ===
                                "awaitingPayment"
                                  ? "Retorne ao consumo para alterar"
                                  : "Preço unitário · clique para alterar"}
                              </p>
                            </>
                          )}
                        </div>

                        <div className="flex items-center gap-2">
                          <Button
                            size="icon"
                            variant="secondary"
                            disabled={
                              selectedCommand.status ===
                              "awaitingPayment"
                            }
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
                            disabled={
                              selectedCommand.status ===
                              "awaitingPayment"
                            }
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
                            disabled={
                              selectedCommand.status ===
                              "awaitingPayment"
                            }
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

              <div className="mt-4 flex shrink-0 justify-end border-t border-[var(--border-subtle)] pt-4">
                <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
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
                      ? "Enviar para pagamento"
                      : "Retornar ao consumo"}
                  </Button>

                  {selectedCommand.status === "awaitingPayment" ? (
                    <Button
                      variant="primary"
                      leadingIcon={<Check />}
                      onClick={() => setIsCloseModalOpen(true)}
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
              Defina um nome livre para identificar esta comanda.
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
              onChange={(event) =>
                setNewCommandName(
                  event.target.value
                )
              }
              placeholder="Ex.: Mesa 04, João ou Aniversário"
              className="h-11 w-full rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-sm text-[var(--text-base)] outline-none transition placeholder:text-[var(--text-subtle)] focus:border-[var(--border-hover)]"
            />
          </label>
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
        open={
          isCloseModalOpen &&
          Boolean(selectedCommand)
        }
        onClose={closeCloseModal}
        labelledBy="bar-command-close-title"
        describedBy="bar-command-close-description"
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

                <p
                  id="bar-command-close-description"
                  className="mt-1 text-sm text-[var(--text-muted)]"
                >
                  Total:{" "}
                  {formatBrlCurrency(
                    getBarCommandTotal(
                      selectedCommand
                    )
                  )}
                </p>
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
              <DropdownField
                label="Forma de pagamento"
                  value={paymentMethod}
                  options={commandPaymentMethods}
                onChange={changePaymentMethod}
              />

              {paymentMethod === "Dinheiro" ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  <InfoField
                    label="Valor total"
                    value={formatBrlCurrency(
                      selectedCommandTotal
                    )}
                  />

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
            </div>

            <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
              <Button
                variant="secondary"
                disabled={isClosingCommand}
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
                    ? "Cobrar na maquininha"
                    : paymentMethod === "Pix"
                      ? "Confirmar Pix"
                    : "Finalizar em dinheiro"}
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
