import {
  useMemo,
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
} from "../../components/ui";
import type {
  Client,
  ClientInput,
  ClientConsumptionHistoryEntry,
} from "../../entities/client";
import {
  isClientInputComplete,
  normalizeClientInput,
} from "../../entities/client";
import {
  formatBrazilianPhone,
} from "../../shared/formatters/phoneInput";

type ClientStatusFilter = "active" | "inactive";

const emptyClientForm: ClientInput = {
  name: "",
  phone: "",
  vehicle: "",
  plate: "",
};

const CLIENTS_PAGE_SIZE = 6;

export function ClientsManager({
  clients,
  onCreate,
  onUpdate,
  onSetActive,
  onRemove,
  onLoadConsumptionHistory,
}: {
  clients: Client[];

  onCreate: (
    input: ClientInput
  ) => Promise<boolean>;

  onUpdate: (
    clientId: number,
    input: ClientInput
  ) => Promise<boolean>;

  onSetActive: (
    clientId: number,
    active: boolean
  ) => Promise<boolean>;

  onRemove: (
    clientId: number
  ) => Promise<boolean>;

  onLoadConsumptionHistory: (
    clientId: number
  ) => Promise<ClientConsumptionHistoryEntry[]>;
}) {
  const [form, setForm] = useState<ClientInput>(emptyClientForm);
  const [editingClientId, setEditingClientId] = useState<number | null>(null);
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [clientPendingDeletionId, setClientPendingDeletionId] =
    useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<ClientStatusFilter>("active");
  const [searchTerm, setSearchTerm] = useState("");
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [formError, setFormError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [consumptionHistory, setConsumptionHistory] = useState<ClientConsumptionHistoryEntry[]>([]);
  const [isLoadingConsumptionHistory, setIsLoadingConsumptionHistory] = useState(false);
  const {
    showToast,
  } = useToast();
  const clientsPerPage = CLIENTS_PAGE_SIZE;

  const editingClient = clients.find((client) => client.id === editingClientId);
  const selectedClient = clients.find((client) => client.id === selectedClientId);
  const clientPendingDeletion = clients.find(
    (client) => client.id === clientPendingDeletionId
  );

  const consumptionSummary = useMemo(() => {
    const services = new Map<
      string,
      {
        name: string;
        quantity: number;
        total: number;
      }
    >();

    const items = new Map<
      string,
      {
        name: string;
        quantity: number;
        total: number;
      }
    >();

    let totalSpent = 0;
    let serviceQuantity = 0;
    let itemQuantity = 0;

    for (const entry of consumptionHistory) {
      totalSpent += entry.total;

      for (const line of entry.lines) {
        const target =
          line.entryType === "SERVICE"
            ? services
            : items;

        const current = target.get(line.itemName);

        target.set(line.itemName, {
          name: line.itemName,
          quantity:
            (current?.quantity ?? 0) +
            line.quantity,
          total:
            (current?.total ?? 0) +
            line.total,
        });

        if (line.entryType === "SERVICE") {
          serviceQuantity += line.quantity;
        } else {
          itemQuantity += line.quantity;
        }
      }
    }

    return {
      totalSpent,
      visitCount: consumptionHistory.length,
      serviceQuantity,
      itemQuantity,
      services: Array.from(services.values()).sort(
        (a, b) => b.quantity - a.quantity
      ),
      items: Array.from(items.values()).sort(
        (a, b) => b.quantity - a.quantity
      ),
    };
  }, [consumptionHistory]);



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

  function updateForm(field: keyof ClientInput, value: string) {
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
    setForm(emptyClientForm);
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
    setConsumptionHistory([]);
    setIsLoadingConsumptionHistory(false);
  }

  async function openDetailsModal(clientId: number) {
    setSelectedClientId(clientId);
    setConsumptionHistory([]);
    setIsLoadingConsumptionHistory(true);

    try {
      setConsumptionHistory(
        await onLoadConsumptionHistory(clientId)
      );
    } finally {
      setIsLoadingConsumptionHistory(false);
    }
  }

  function changeStatusFilter(nextFilter: ClientStatusFilter) {
    setStatusFilter(nextFilter);
    setCurrentPage(1);
  }

  async function handleSubmit() {
    const normalizedForm =
      normalizeClientInput(form);

    if (!isClientInputComplete(normalizedForm)) {
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

  function handleEdit(client: Client) {
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

  function openDeleteConfirmation(clientId: number) {
    setClientPendingDeletionId(clientId);
  }

  function closeDeleteConfirmation() {
    setClientPendingDeletionId(null);
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

    closeDeleteConfirmation();

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
                onClick={() => void openDetailsModal(client.id)}
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
        labelledBy="client-details-title"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-[820px] max-h-[90vh] overflow-y-auto premium-scroll rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {selectedClient ? (
          <>
            <h2
              id="client-details-title"
              className="sr-only"
            >
              {selectedClient.name}
            </h2>

            <div className="flex items-start justify-between gap-4">
              <SectionTitle
                compact
                title={selectedClient.name}
                subtitle="Dados do cliente e do veículo."
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

            <div className="mt-5 grid gap-3 sm:grid-cols-4">
              <InfoField
                label="Telefone"
                value={selectedClient.phone || "Não informado"}
              />
              <InfoField label="Status" value={selectedClient.active ? "Ativo" : "Inativo"} />
              <InfoField label="Veículo" value={selectedClient.vehicle} />
              <InfoField label="Placa" value={selectedClient.plate} />
            </div>

            <div className="mt-5 border-t border-[var(--border-subtle)] pt-5">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-[var(--text-base)]">
                Resumo do cliente
              </h3>

              {isLoadingConsumptionHistory ? (
                <p className="mt-4 text-sm text-[var(--text-muted)]">
                  Carregando dados do cliente...
                </p>
              ) : (
                <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
                  <div className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
                    <span className="text-[11px] uppercase text-[var(--text-subtle)]">
                      Total gasto
                    </span>
                    <strong className="mt-1 block text-sm text-[var(--text-base)]">
                      R$ {consumptionSummary.totalSpent.toFixed(2).replace(".", ",")}
                    </strong>
                  </div>

                  <div className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
                    <span className="text-[11px] uppercase text-[var(--text-subtle)]">
                      Atendimentos
                    </span>
                    <strong className="mt-1 block text-sm text-[var(--text-base)]">
                      {consumptionSummary.visitCount}
                    </strong>
                  </div>

                  <div className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
                    <span className="text-[11px] uppercase text-[var(--text-subtle)]">
                      Serviços
                    </span>
                    <strong className="mt-1 block text-sm text-[var(--text-base)]">
                      {consumptionSummary.serviceQuantity}
                    </strong>
                  </div>

                  <div className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
                    <span className="text-[11px] uppercase text-[var(--text-subtle)]">
                      Itens
                    </span>
                    <strong className="mt-1 block text-sm text-[var(--text-base)]">
                      {consumptionSummary.itemQuantity}
                    </strong>
                  </div>
                </div>
              )}
            </div>

            {!isLoadingConsumptionHistory && consumptionHistory.length > 0 ? (
              <div className="mt-5 grid gap-4 md:grid-cols-2">
                <section className="min-w-0">
                  <h3 className="text-sm font-semibold uppercase tracking-wide text-[var(--text-base)]">
                    Serviços
                  </h3>

                  <div className="mt-3 divide-y divide-[var(--border-subtle)] rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3">
                    {consumptionSummary.services.length === 0 ? (
                      <p className="py-4 text-sm text-[var(--text-muted)]">
                        Nenhum serviço registrado.
                      </p>
                    ) : (
                      consumptionSummary.services.map((service) => (
                        <div
                          key={service.name}
                          className="flex items-center justify-between gap-4 py-3"
                        >
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-[var(--text-base)]">
                              {service.name}
                            </p>
                            <p className="mt-1 text-xs text-[var(--text-subtle)]">
                              {service.quantity}x
                            </p>
                          </div>
                          <span className="shrink-0 text-sm font-semibold text-[var(--text-base)]">
                            R$ {service.total.toFixed(2).replace(".", ",")}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </section>

                <section className="min-w-0">
                  <h3 className="text-sm font-semibold uppercase tracking-wide text-[var(--text-base)]">
                    Itens
                  </h3>

                  <div className="mt-3 divide-y divide-[var(--border-subtle)] rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3">
                    {consumptionSummary.items.length === 0 ? (
                      <p className="py-4 text-sm text-[var(--text-muted)]">
                        Nenhum item registrado.
                      </p>
                    ) : (
                      consumptionSummary.items.map((item) => (
                        <div
                          key={item.name}
                          className="flex items-center justify-between gap-4 py-3"
                        >
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-[var(--text-base)]">
                              {item.name}
                            </p>
                            <p className="mt-1 text-xs text-[var(--text-subtle)]">
                              {item.quantity}x
                            </p>
                          </div>
                          <span className="shrink-0 text-sm font-semibold text-[var(--text-base)]">
                            R$ {item.total.toFixed(2).replace(".", ",")}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </section>
              </div>
            ) : null}

            <div className="mt-5 border-t border-[var(--border-subtle)] pt-5">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-[var(--text-base)]">
                Histórico
              </h3>

              {isLoadingConsumptionHistory ? null : consumptionHistory.length === 0 ? (
                <p className="mt-3 text-sm text-[var(--text-muted)]">
                  Nenhum atendimento pago vinculado a este cliente.
                </p>
              ) : (
                <div className="mt-3 space-y-3">
                  {consumptionHistory.map((entry) => (
                    <div
                      key={entry.operationId}
                      className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div>
                          <span className="text-sm font-semibold text-[var(--text-base)]">
                            Comanda #{entry.operationId}
                          </span>
                          <p className="mt-1 text-xs text-[var(--text-subtle)]">
                            {new Date(entry.completedAt).toLocaleString("pt-BR")}
                          </p>
                        </div>
                        <strong className="text-sm text-[var(--text-base)]">
                          R$ {entry.total.toFixed(2).replace(".", ",")}
                        </strong>
                      </div>

                      <div className="mt-3 divide-y divide-[var(--border-subtle)] border-t border-[var(--border-subtle)]">
                        {entry.lines.map((line, index) => (
                          <div
                            key={`${entry.operationId}-${index}`}
                            className="flex items-center justify-between gap-4 py-2 text-sm"
                          >
                            <div className="min-w-0">
                              <p className="truncate text-[var(--text-base)]">
                                {line.itemName}
                              </p>
                              <p className="mt-0.5 text-xs text-[var(--text-subtle)]">
                                {line.entryType === "SERVICE" ? "Serviço" : "Item"}
                                {" · "}
                                {line.quantity}x
                                {" · "}
                                R$ {line.unitPrice.toFixed(2).replace(".", ",")} cada
                              </p>
                            </div>
                            <span className="shrink-0 text-[var(--text-base)]">
                              R$ {line.total.toFixed(2).replace(".", ",")}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
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
                  openDeleteConfirmation(selectedClient.id)
                }
              >
                Excluir
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>

      <AnimatedModal
        open={Boolean(clientPendingDeletion)}
        onClose={closeDeleteConfirmation}
        labelledBy="client-delete-title"
        describedBy="client-delete-description"
        backdropClassName="z-[230] p-4"
        panelClassName="w-full max-w-[440px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--color-danger-border)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
      >
        {clientPendingDeletion ? (
          <>
            <div className="p-5">
              <h2
                id="client-delete-title"
                className="text-lg font-semibold text-[var(--text-base)]"
              >
                Excluir cliente?
              </h2>

              <p
                id="client-delete-description"
                className="mt-3 text-sm leading-6 text-[var(--text-muted)]"
              >
                Você está prestes a excluir{" "}
                <strong className="text-[var(--text-base)]">
                  {clientPendingDeletion.name}
                </strong>
                . Essa ação não pode ser desfeita.
              </p>
            </div>

            <div className="flex justify-end gap-2 border-t border-[var(--border-subtle)] p-5">
              <Button
                variant="secondary"
                onClick={closeDeleteConfirmation}
              >
                Cancelar
              </Button>

              <Button
                variant="danger"
                leadingIcon={<Trash2 />}
                onClick={() =>
                  void handleDelete(clientPendingDeletion.id)
                }
              >
                Excluir cliente
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>

      <AnimatedModal
        open={isFormModalOpen}
        onClose={closeFormModal}
        labelledBy="client-form-title"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-[520px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        <h2
          id="client-form-title"
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
                : "Cadastre os dados básicos do cliente e do veículo."
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
