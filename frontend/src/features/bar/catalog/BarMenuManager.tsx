import {
  useMemo,
  useState,
  useEffect,
} from "react";

import {
  Pencil,
  Plus,
  Trash2,
  X,
} from "lucide-react";

import {
  AdminToggle,
  AnimatedModal,
  Button,
  ContentStack,
  DropdownSelect,
  EmptyState,
  PageActions,
  Pagination,
  PremiumCard,
  SearchField,
  SectionTitle,
  TextField,
} from "../../../components/ui";

import {
  isBarCatalogItemInputComplete,
  normalizeBarCatalogItemInput,
} from "../../../entities/catalog-item";

import type {
  BarCatalogItem,
  BarCatalogItemInput,
  BarCatalogItemType,
} from "../../../entities/catalog-item";

import {
  currencyInputToNumber,
  formatCurrencyInput,
} from "../../../shared/formatters/currencyInput";

import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";
import type { Supplier } from "../../../entities/supplier";
import { repositories } from "../../../data/repositories";

type CatalogForm = {
  name: string;
  type: BarCatalogItemType;
  price: string;
  stockEnabled: boolean;
  stockQuantity: string;
  minimumStockQuantity: string;
  supplierId: string;
};

const emptyCatalogForm: CatalogForm = {
  name: "",
  type: "ITEM",
  price: "",
  stockEnabled: false,
  stockQuantity: "0",
  minimumStockQuantity: "0",
  supplierId: "",
};

const CATALOG_RECORDS_PAGE_SIZE = 7;

const catalogTypeLabels: Record<
  BarCatalogItemType,
  string
> = {
  ITEM: "Item",
  SERVICE: "Serviço",
};

export function BarMenuManager({
  catalogEntries,
  onCreate,
  onUpdate,
  onRemove,
}: {
  catalogEntries: BarCatalogItem[];
  onCreate: (
    input: BarCatalogItemInput
  ) => Promise<BarCatalogItem | null>;
  onUpdate: (
    entryId: number,
    input: BarCatalogItemInput
  ) => Promise<BarCatalogItem | null>;
  onRemove: (
    entryId: number
  ) => Promise<boolean>;
}) {
  const [searchTerm, setSearchTerm] =
    useState("");
  const [form, setForm] =
    useState<CatalogForm>(emptyCatalogForm);
  const [editingEntry, setEditingEntry] =
    useState<BarCatalogItem | null>(null);
  const [isModalOpen, setIsModalOpen] =
    useState(false);
  const [entryPendingDelete, setEntryPendingDelete] =
    useState<BarCatalogItem | null>(null);
  const [isDeletingEntry, setIsDeletingEntry] =
    useState(false);
  const [formError, setFormError] =
    useState("");
  const [currentPage, setCurrentPage] =
    useState(1);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);

  useEffect(() => {
    let active = true;
    repositories.suppliers.list().then((loaded) => {
      if (active) setSuppliers(loaded);
    }).catch(() => undefined);
    return () => { active = false; };
  }, []);

  const records = useMemo(
    () =>
      [...catalogEntries].sort(
        (left, right) =>
          left.name.localeCompare(
            right.name,
            "pt-BR"
          )
      ),
    [catalogEntries]
  );

  const normalizedSearchTerm =
    searchTerm
      .trim()
      .toLocaleLowerCase("pt-BR");

  const filteredRecords = records.filter(
    (entry) =>
      !normalizedSearchTerm ||
      [
        entry.name,
        catalogTypeLabels[entry.type],
        formatBrlCurrency(entry.price),
      ].some((value) =>
        value
          .toLocaleLowerCase("pt-BR")
          .includes(normalizedSearchTerm)
      )
  );

  const totalPages = Math.max(
    1,
    Math.ceil(
      filteredRecords.length /
        CATALOG_RECORDS_PAGE_SIZE
    )
  );
  const safeCurrentPage = Math.min(
    currentPage,
    totalPages
  );
  const pageStart =
    (safeCurrentPage - 1) *
    CATALOG_RECORDS_PAGE_SIZE;
  const paginatedRecords =
    filteredRecords.slice(
      pageStart,
      pageStart + CATALOG_RECORDS_PAGE_SIZE
    );

  function updateForm(
    field: Exclude<
      keyof CatalogForm,
      "stockEnabled"
    >,
    value: string
  ) {
    setFormError("");
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  }

  function updateStockForm(
    field:
      | "stockQuantity"
      | "minimumStockQuantity",
    value: string
  ) {
    if (value === "") {
      updateForm(field, "");
      return;
    }

    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
      updateForm(field, "0");
      return;
    }

    updateForm(
      field,
      String(
        Math.min(
          999_999,
          Math.max(0, Math.floor(parsed))
        )
      )
    );
  }

  function setCatalogType(
    type: BarCatalogItemType
  ) {
    setFormError("");
    setForm((current) => ({
      ...current,
      type,
      stockEnabled:
        type === "ITEM"
          ? current.stockEnabled
          : false,
      supplierId:
        type === "ITEM"
          ? current.supplierId
          : "",
    }));
  }

  function toggleStockControl() {
    setFormError("");
    setForm((current) => ({
      ...current,
      stockEnabled: !current.stockEnabled,
    }));
  }

  function resetForm() {
    setForm(emptyCatalogForm);
    setEditingEntry(null);
    setFormError("");
  }

  function openCreateModal() {
    resetForm();
    setIsModalOpen(true);
  }

  function openEditModal(
    entry: BarCatalogItem
  ) {
    setEditingEntry(entry);
    setForm({
      name: entry.name,
      type: entry.type,
      price: formatBrlCurrency(entry.price),
      stockEnabled: entry.stockEnabled,
      stockQuantity:
        entry.stockQuantity === null
          ? "0"
          : String(entry.stockQuantity),
      minimumStockQuantity:
        entry.minimumStockQuantity === null
          ? "0"
          : String(entry.minimumStockQuantity),
      supplierId:
        entry.supplierId === null
          ? ""
          : String(entry.supplierId),
    });
    setFormError("");
    setIsModalOpen(true);
  }

  function closeModal() {
    setIsModalOpen(false);
    resetForm();
  }

  async function handleSubmit() {
    const input =
      normalizeBarCatalogItemInput({
        name: form.name,
        type: form.type,
        price: currencyInputToNumber(
          form.price
        ),
        stockEnabled:
          form.type === "ITEM" &&
          form.stockEnabled,
        stockQuantity:
          form.type === "ITEM" &&
          form.stockEnabled
            ? Number(form.stockQuantity)
            : null,
        minimumStockQuantity:
          form.type === "ITEM" &&
          form.stockEnabled
            ? Number(
                form.minimumStockQuantity
              )
            : null,
        supplierId:
          form.type === "ITEM" && form.supplierId
            ? Number(form.supplierId)
            : null,
      });

    if (!isBarCatalogItemInputComplete(input)) {
      setFormError(
        "Informe nome, tipo, preço e estoque válidos."
      );
      return;
    }

    const result = editingEntry
      ? await onUpdate(
          editingEntry.id,
          input
        )
      : await onCreate(input);

    if (!result) {
      setFormError(
        editingEntry
          ? "Não foi possível atualizar o cadastro."
          : "Não foi possível criar o cadastro."
      );
      return;
    }

    setCurrentPage(1);
    closeModal();
  }

  async function confirmDelete() {
    if (
      !entryPendingDelete ||
      isDeletingEntry
    ) {
      return;
    }

    setIsDeletingEntry(true);

    try {
      const removed = await onRemove(
        entryPendingDelete.id
      );

      if (removed) {
        setEntryPendingDelete(null);
      }
    } finally {
      setIsDeletingEntry(false);
    }
  }

  return (
    <ContentStack>
      <PageActions>
        <Button
          variant="primary"
          leadingIcon={<Plus />}
          onClick={openCreateModal}
        >
          Novo item/serviço
        </Button>
      </PageActions>

      <PremiumCard className="v7-card-fill" contentClassName="v7-card-content">
        <div className="flex min-h-0 flex-1 flex-col">
          <SearchField
            value={searchTerm}
            onChange={(value) => {
              setSearchTerm(value);
              setCurrentPage(1);
            }}
            placeholder="Buscar item ou serviço..."
          />

          <div className="bar-catalog-list v7-list-scroll premium-scroll mt-3 pr-1">
            {filteredRecords.length === 0 ? (
              <EmptyState message="Nenhum item ou serviço encontrado." />
            ) : null}

            {paginatedRecords.map((entry) => (
              <div
                key={entry.id}
                className="bar-catalog-row min-h-[68px] border-b border-[var(--border-subtle)] py-3 last:border-b-0"
              >
                <div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-center">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-[var(--text-base)]">
                      {entry.name}
                    </p>
                    <p className="mt-1 text-xs text-[var(--text-muted)]">
                      {catalogTypeLabels[entry.type]}
                      {" · "}
                      {formatBrlCurrency(entry.price)}
                    </p>
                    {entry.type === "ITEM" ? (
                      <p className="mt-1 text-xs text-[var(--text-subtle)]">
                        {entry.stockEnabled
                          ? `Estoque: ${entry.stockQuantity ?? 0} · Mínimo: ${entry.minimumStockQuantity ?? 0}`
                          : "Controle de estoque desativado"}
                      </p>
                    ) : null}
                  </div>

                  <div className="flex flex-wrap gap-2 xl:justify-end">
                    <Button
                      size="compact"
                      variant="secondary"
                      leadingIcon={<Pencil />}
                      onClick={() => openEditModal(entry)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="compact"
                      variant="danger"
                      leadingIcon={<Trash2 />}
                      onClick={() => setEntryPendingDelete(entry)}
                    >
                      Excluir
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="v7-card-footer">
            {filteredRecords.length >
            CATALOG_RECORDS_PAGE_SIZE ? (
              <Pagination
                currentPage={safeCurrentPage}
                totalPages={totalPages}
                totalItems={filteredRecords.length}
                pageStart={pageStart}
                pageSize={paginatedRecords.length}
                onPrevious={() =>
                  setCurrentPage(
                    Math.max(1, safeCurrentPage - 1)
                  )
                }
                onNext={() =>
                  setCurrentPage(
                    Math.min(totalPages, safeCurrentPage + 1)
                  )
                }
              />
            ) : null}
          </div>
        </div>
      </PremiumCard>

      <AnimatedModal
        open={isModalOpen}
        onClose={closeModal}
        labelledBy="bar-menu-form-title"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-[460px] overflow-visible rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        <div className="flex items-start justify-between gap-4">
          <SectionTitle
            compact
            title={editingEntry
              ? "Editar item/serviço"
              : "Novo item/serviço"}
          />
          <Button
            size="icon"
            variant="ghost"
            onClick={closeModal}
            leadingIcon={<X />}
            aria-label="Fechar modal"
            title="Fechar"
          />
        </div>

        <div className="mt-4 grid gap-4">
          <TextField
            label="Nome"
            value={form.name}
            placeholder="Nome do item ou serviço"
            onChange={(value) =>
              updateForm("name", value)
            }
          />
          <DropdownSelect
            label="Tipo"
            value={form.type}
            placeholder="Selecione o tipo"
            options={[
              { value: "ITEM", label: "Item" },
              { value: "SERVICE", label: "Serviço" },
            ]}
            onChange={(value) =>
              setCatalogType(
                value === "SERVICE"
                  ? "SERVICE"
                  : "ITEM"
              )
            }
            constrainToModal
          />
          <TextField
            label="Preço"
            value={form.price}
            placeholder="R$ 0,00"
            onChange={(value) =>
              updateForm(
                "price",
                formatCurrencyInput(value)
              )
            }
          />

          {form.type === "ITEM" ? (
            <DropdownSelect
              label="Fornecedor"
              value={form.supplierId}
              placeholder="Sem fornecedor"
              options={[
                { value: "", label: "Sem fornecedor" },
                ...suppliers.map((supplier) => ({
                  value: String(supplier.id),
                  label: supplier.name,
                })),
              ]}
              onChange={(value) => updateForm("supplierId", value)}
              constrainToModal
            />
          ) : null}

          {form.type === "ITEM" ? (
            <AdminToggle
              title="Controlar estoque"
              text="Ative para limitar vendas e receber alertas de estoque baixo."
              checked={form.stockEnabled}
              onClick={toggleStockControl}
            />
          ) : null}

          {form.type === "ITEM" &&
          form.stockEnabled ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField
                label="Estoque atual"
                type="number"
                value={form.stockQuantity}
                placeholder="0"
                min={0}
                max={999_999}
                step={1}
                onChange={(value) =>
                  updateStockForm(
                    "stockQuantity",
                    value
                  )
                }
              />
              <TextField
                label="Estoque mínimo"
                type="number"
                value={form.minimumStockQuantity}
                placeholder="0"
                min={0}
                max={999_999}
                step={1}
                onChange={(value) =>
                  updateStockForm(
                    "minimumStockQuantity",
                    value
                  )
                }
              />
            </div>
          ) : null}
          {formError ? (
            <div
              className="rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]"
              role="alert"
            >
              {formError}
            </div>
          ) : null}
        </div>

        <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" onClick={closeModal}>
            Cancelar
          </Button>
          <Button
            variant="primary"
            onClick={() => void handleSubmit()}
          >
            {editingEntry ? "Salvar alterações" : "Criar"}
          </Button>
        </div>
      </AnimatedModal>

      <AnimatedModal
        open={entryPendingDelete !== null}
        onClose={() => setEntryPendingDelete(null)}
        labelledBy="bar-catalog-delete-title"
        closeOnBackdrop={!isDeletingEntry}
        closeOnEscape={!isDeletingEntry}
        backdropClassName="z-[230] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {entryPendingDelete ? (
          <>
            <SectionTitle
              compact
              title="Excluir item/serviço"
              subtitle={entryPendingDelete.name}
            />
            <p className="mt-4 text-sm text-[var(--text-muted)]">
              O cadastro deixará de aparecer em novas comandas. Os registros anteriores serão preservados.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <Button
                variant="secondary"
                disabled={isDeletingEntry}
                onClick={() => setEntryPendingDelete(null)}
              >
                Cancelar
              </Button>
              <Button
                variant="danger"
                disabled={isDeletingEntry}
                onClick={() => void confirmDelete()}
              >
                {isDeletingEntry ? "Excluindo..." : "Excluir"}
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>
    </ContentStack>
  );
}
