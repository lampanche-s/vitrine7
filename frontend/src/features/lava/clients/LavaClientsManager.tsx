import {
  useState,
} from "react";
import {
  Eye,
  Pencil,
  Plus,
  Power,
  PowerOff,
  Trash2,
  X,
} from "lucide-react";
import {
  AnimatedModal,
  Button,
  ContentStack,
  EmptyState,
  FilterChip,
  InfoField,
  PageActions,
  Pagination,
  PremiumCard,
  SectionTitle,
  SearchField,
  TextField,
  useToast,
} from "../../../components/ui";
import type {
  LavaClient,
  LavaClientInput,
} from "../../../entities/client";
import {
  isLavaClientInputComplete,
  normalizeLavaClientInput,
} from "../../../entities/client";
import {
  formatBrazilianPhone,
} from "../../../shared/formatters/phoneInput";

type LavaClientStatusFilter = "active" | "inactive";

const emptyLavaClientForm: LavaClientInput = {
  name: "",
  phone: "",
  vehicle: "",
  plate: "",
};

const LAVA_CLIENTS_PAGE_SIZE = 6;

export function LavaClientsManager({
  clients,
  onCreate,
  onUpdate,
  onSetActive,
  onRemove,
}: {
  clients: LavaClient[];

  onCreate: (
    input: LavaClientInput
  ) => Promise<boolean>;

  onUpdate: (
    clientId: number,
    input: LavaClientInput
  ) => Promise<boolean>;

  onSetActive: (
    clientId: number,
    active: boolean
  ) => Promise<boolean>;

  onRemove: (
    clientId: number
  ) => Promise<boolean>;
}) {
  const [form, setForm] = useState<LavaClientInput>(emptyLavaClientForm);
  const [editingClientId, setEditingClientId] = useState<number | null>(null);
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<LavaClientStatusFilter>("active");
  const [searchTerm, setSearchTerm] = useState("");
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [formError, setFormError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const {
    showToast,
  } = useToast();
  const clientsPerPage = LAVA_CLIENTS_PAGE_SIZE;

  const editingClient = clients.find((client) => client.id === editingClientId);
  const selectedClient = clients.find((client) => client.id === selectedClientId);

  const filteredClients = clients.filter((client) => {
    const matchesStatus =
      statusFilter === "active"
        ? client.active
        : !client.active;

    const normalizedSearch = searchTerm.trim().toLowerCase();

    const matchesSearch =
      normalizedSearch.length === 0 ||
      client.name.toLowerCase().includes(normalizedSearch) ||
      client.phone.toLowerCase().includes(normalizedSearch) ||
      client.vehicle.toLowerCase().includes(normalizedSearch) ||
      client.plate.toLowerCase().includes(normalizedSearch);

    return matchesStatus && matchesSearch;
  });

  const totalPages = Math.max(
    1,
    Math.ceil(filteredClients.length / clientsPerPage)
  );

  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pageStart = (safeCurrentPage - 1) * clientsPerPage;

  const paginatedClients = filteredClients.slice(
    pageStart,
    pageStart + clientsPerPage
  );

  function updateForm(field: keyof LavaClientInput, value: string) {
    setFormError("");

    setForm((current) => ({
      ...current,
      [field]:
        field === "phone"
          ? formatBrazilianPhone(value)
          : value,
    }));
  }

  function resetForm() {
    setForm(emptyLavaClientForm);
    setEditingClientId(null);
    setFormError("");
  }

  function openCreateModal() {
    resetForm();
    setSelectedClientId(null);
    setIsFormModalOpen(true);
  }

  function closeFormModal() {
    setIsFormModalOpen(false);
    resetForm();
  }

  function closeDetailsModal() {
    setSelectedClientId(null);
  }

  function changeStatusFilter(nextFilter: LavaClientStatusFilter) {
    setStatusFilter(nextFilter);
    setCurrentPage(1);
  }

  async function handleSubmit() {
    const normalizedForm =
      normalizeLavaClientInput(form);

    if (!isLavaClientInputComplete(normalizedForm)) {
      setFormError("Preencha nome, veículo e placa antes de salvar.");
      return;
    }

    if (editingClientId !== null) {
      const updated = await onUpdate(
        editingClientId,
        normalizedForm
      );

      if (!updated) {
        setFormError(
          "Não foi possível atualizar o cliente."
        );
        return;
      }


      showToast({
        title: `Cliente ${normalizedForm.name} editado.`,
        variant: "success",
        dedupeKey: `client-updated|${editingClientId}`,
      });

      closeFormModal();
      return;
    }

    const created = await onCreate(normalizedForm);

    if (!created) {
      setFormError(
        "Não foi possível cadastrar o cliente."
      );
      return;
    }


    showToast({
      title: `Cliente ${normalizedForm.name} criado.`,
      variant: "success",
      dedupeKey: `client-created|${normalizedForm.name}|${normalizedForm.plate}`,
    });

    setCurrentPage(1);
    closeFormModal();
  }

  function handleEdit(client: LavaClient) {
    setSelectedClientId(null);
    setEditingClientId(client.id);
    setForm({
      name: client.name,
      phone: client.phone,
      vehicle: client.vehicle,
      plate: client.plate,
    });
    setFormError("");
    setIsFormModalOpen(true);
  }

  async function handleToggleStatus(
    clientId: number
  ) {
    const client = clients.find(
      (currentClient) =>
        currentClient.id === clientId
    );

    if (!client) {
      return;
    }

    const updated = await onSetActive(
      clientId,
      !client.active
    );

    if (!updated) {
      return;
    }

  }

  async function handleDelete(
    clientId: number
  ) {
    const removed = await onRemove(clientId);

    if (!removed) {
      return;
    }

    const removedClient = clients.find((client) => client.id === clientId);


    showToast({
      title: `Cliente ${removedClient?.name ?? clientId} excluído.`,
      variant: "warning",
      dedupeKey: `client-removed|${clientId}`,
    });

    if (editingClientId === clientId) {
      closeFormModal();
    }

    if (selectedClientId === clientId) {
      closeDetailsModal();
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
          Novo cliente
        </Button>
      </PageActions>

      <PremiumCard
        className="v7-card-fill"
        contentClassName="v7-card-content"
      >
        <div className="v7-card-header">
          <div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-center">
            <SearchField
              value={searchTerm}
              onChange={(value) => {
                setSearchTerm(value);
                setCurrentPage(1);
              }}
              placeholder="Buscar cliente, telefone, veículo ou placa..."
            />

            <div className="flex flex-wrap justify-end gap-1.5">
              <FilterChip
                label="Ativos"
                active={statusFilter === "active"}
                onClick={() => changeStatusFilter("active")}
              />

              <FilterChip
                label="Inativos"
                active={statusFilter === "inactive"}
                onClick={() => changeStatusFilter("inactive")}
              />
            </div>
          </div>
        </div>

        <div className="clean-inner-list v7-list-scroll premium-scroll mt-3 pr-1">
          {filteredClients.length === 0 && (
            <EmptyState
              message="Nenhum cliente encontrado."
            />
          )}

          {paginatedClients.map((client) => (
            <div
              key={client.id}
              className="grid min-h-[68px] w-full grid-cols-[minmax(0,1fr)_40px] items-center gap-3 border-b border-[var(--border-subtle)] py-3 text-left last:border-b-0"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-[var(--text-base)]">
                  {client.name}
                </p>

                <p className="mt-1 truncate text-sm text-[var(--text-muted)]">
                  {client.phone || "Telefone não informado"}
                </p>

                <p className="mt-1 truncate text-xs text-[var(--text-subtle)]">
                  {client.vehicle} · {client.plate}
                </p>
              </div>

              <Button
                size="icon"
                variant="ghost"
                onClick={() => setSelectedClientId(client.id)}
                leadingIcon={<Eye />}
                aria-label={`Visualizar cliente ${client.name}`}
                title="Visualizar"
              />
            </div>
          ))}
        </div>

        <div className="v7-card-footer">
          {filteredClients.length > clientsPerPage && (
            <Pagination
              currentPage={safeCurrentPage}
              totalPages={totalPages}
              totalItems={filteredClients.length}
              pageStart={pageStart}
              pageSize={paginatedClients.length}
              onPrevious={() => setCurrentPage(Math.max(1, safeCurrentPage - 1))}
              onNext={() => setCurrentPage(Math.min(totalPages, safeCurrentPage + 1))}
            />
          )}
        </div>
      </PremiumCard>

      <AnimatedModal
        open={Boolean(selectedClient)}
        onClose={closeDetailsModal}
        labelledBy="lava-client-details-title"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-[540px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {selectedClient ? (
          <>
            <h2
              id="lava-client-details-title"
              className="sr-only"
            >
              {selectedClient.name}
            </h2>

            <div className="flex items-start justify-between gap-4">
              <SectionTitle
                compact
                title={selectedClient.name}
                subtitle="Dados do cliente, veículo e histórico básico."
              />

              <Button
                size="icon"
                variant="ghost"
                onClick={closeDetailsModal}
                leadingIcon={<X />}
                aria-label="Fechar modal"
                title="Fechar"
              />
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <InfoField
                label="Telefone"
                value={selectedClient.phone || "Não informado"}
              />
              <InfoField label="Status" value={selectedClient.active ? "Ativo" : "Inativo"} />
              <InfoField label="Veículo" value={selectedClient.vehicle} />
              <InfoField label="Placa" value={selectedClient.plate} />
              <InfoField label="Último serviço" value={selectedClient.lastService} />
            </div>

            <div className="mt-5 grid gap-2 sm:grid-cols-3">
              <Button
                size="compact"
                variant="secondary"
                fullWidth
                leadingIcon={<Pencil />}
                onClick={() => handleEdit(selectedClient)}
              >
                Editar
              </Button>

              <Button
                size="compact"
                variant="secondary"
                fullWidth
                leadingIcon={
                  selectedClient.active ? <PowerOff /> : <Power />
                }
                onClick={() =>
                  void handleToggleStatus(selectedClient.id)
                }
              >
                {selectedClient.active ? "Desativar" : "Ativar"}
              </Button>

              <Button
                size="compact"
                variant="danger"
                fullWidth
                leadingIcon={<Trash2 />}
                onClick={() =>
                  void handleDelete(selectedClient.id)
                }
              >
                Excluir
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>

      <AnimatedModal
        open={isFormModalOpen}
        onClose={closeFormModal}
        labelledBy="lava-client-form-title"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-[520px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        <h2
          id="lava-client-form-title"
          className="sr-only"
        >
          {editingClient ? "Editar cliente" : "Novo cliente"}
        </h2>

        <div className="flex items-start justify-between gap-4">
          <SectionTitle
            compact
            title={editingClient ? "Editar cliente" : "Novo cliente"}
            subtitle={
              editingClient
                ? "Atualize os dados do cliente selecionado."
                : "Cadastre um novo cliente para usar na abertura de OS."
            }
          />

          <Button
            size="icon"
            variant="ghost"
            onClick={closeFormModal}
            leadingIcon={<X />}
            aria-label="Fechar modal"
            title="Fechar"
          />
        </div>

        <div className="mt-5 grid gap-4">
          <TextField
            label="Nome do cliente"
            value={form.name}
            placeholder="Ex: Marcos Almeida"
            onChange={(value) => updateForm("name", value)}
          />

          <TextField
            label="Telefone"
            value={form.phone}
            placeholder="(71) 98888-0000"
            type="tel"
            onChange={(value) => updateForm("phone", value)}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="Veículo"
              value={form.vehicle}
              placeholder="Ex: Honda Civic"
              onChange={(value) => updateForm("vehicle", value)}
            />

            <TextField
              label="Placa"
              value={form.plate}
              placeholder="ABC-1D23"
              onChange={(value) => updateForm("plate", value)}
            />
          </div>

          {formError && (
            <div
              className="rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]"
              role="alert"
            >
              {formError}
            </div>
          )}
        </div>

        <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            variant="secondary"
            onClick={closeFormModal}
          >
            Cancelar
          </Button>

          <Button
            variant="primary"
            onClick={() => void handleSubmit()}
          >
            {editingClient ? "Salvar alterações" : "Criar cliente"}
          </Button>
        </div>
      </AnimatedModal>
    </ContentStack>
  );
}

