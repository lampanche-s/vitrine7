import { useState } from "react";
import {
  Pencil,
  Plus,
  Power,
  PowerOff,
  Trash2,
  X,
} from "lucide-react";
import {
  AnimatedModal,
  CounterCard,
  EmptyState,
  FilterChip,
  Pagination,
  PremiumCard,
  SectionTitle,
  TextField,
} from "../../../components/ui";
import type {
  LavaService,
  LavaServiceInput,
} from "../../../entities/service";
import {
  getLavaServiceMediumCarPrice,
  getLavaServicePriceSummary,
  getLavaServiceSmallCarPrice,
  isLavaServiceInputComplete,
  normalizeLavaServiceInput,
} from "../../../entities/service";
import {
  currencyInputToNumber,
  formatCurrencyInput,
} from "../../../shared/formatters/currencyInput";

const emptyServiceForm: LavaServiceInput = {
  name: "",
  category: "",
  smallCarPrice: "",
  mediumCarPrice: "",
  duration: "",
};

const LAVA_SERVICES_PAGE_SIZE = 6;

type ServiceStatusFilter = "active" | "inactive";

export function LavaServicesManager({
  services,
  onCreate,
  onUpdate,
  onSetActive,
  onRemove,
}: {
  services: LavaService[];

  onCreate: (
    input: LavaServiceInput
  ) => Promise<boolean>;

  onUpdate: (
    serviceId: number,
    input: LavaServiceInput
  ) => Promise<boolean>;

  onSetActive: (
    serviceId: number,
    active: boolean
  ) => Promise<boolean>;

  onRemove: (
    serviceId: number
  ) => Promise<boolean>;
}) {
  const [form, setForm] = useState<LavaServiceInput>(emptyServiceForm);
  const [editingServiceId, setEditingServiceId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<ServiceStatusFilter>("active");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formError, setFormError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const servicesPerPage = LAVA_SERVICES_PAGE_SIZE;

  const editingService = services.find((service) => service.id === editingServiceId);
  const activeServices = services.filter((service) => service.active).length;
  const inactiveServices = services.length - activeServices;

  const filteredServices = services.filter((service) => {
    return statusFilter === "active"
      ? service.active
      : !service.active;
  });

  const totalPages = Math.max(
    1,
    Math.ceil(filteredServices.length / servicesPerPage)
  );

  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pageStart = (safeCurrentPage - 1) * servicesPerPage;

  const paginatedServices = filteredServices.slice(
    pageStart,
    pageStart + servicesPerPage
  );

  function updateForm(field: keyof LavaServiceInput, value: string) {
    setFormError("");

    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  }

  function resetForm() {
    setForm(emptyServiceForm);
    setEditingServiceId(null);
    setFormError("");
  }

  function openCreateModal() {
    resetForm();
    setIsModalOpen(true);
  }

  function closeModal() {
    setIsModalOpen(false);
    resetForm();
  }

  function changeStatusFilter(nextFilter: ServiceStatusFilter) {
    setStatusFilter(nextFilter);
    setCurrentPage(1);
  }

  async function handleSubmit() {
    const normalizedForm =
      normalizeLavaServiceInput(form);

    if (!isLavaServiceInputComplete(normalizedForm)) {
      setFormError("Preencha todos os campos antes de salvar.");
      return;
    }

    if (
      currencyInputToNumber(normalizedForm.smallCarPrice) <= 0 ||
      currencyInputToNumber(normalizedForm.mediumCarPrice) <= 0
    ) {
      setFormError("Informe valores válidos para carro pequeno e médio.");
      return;
    }

    if (editingServiceId !== null) {
      const updated = await onUpdate(
        editingServiceId,
        normalizedForm
      );

      if (!updated) {
        return;
      }


      closeModal();
      return;
    }

    const created = await onCreate(normalizedForm);

    if (!created) {
      return;
    }


    setCurrentPage(1);
    closeModal();
  }

  function handleEdit(service: LavaService) {
    setEditingServiceId(service.id);
    setFormError("");
    setForm({
      name: service.name,
      category: service.category,
      smallCarPrice: formatCurrencyInput(
        getLavaServiceSmallCarPrice(service)
      ),
      mediumCarPrice: formatCurrencyInput(
        getLavaServiceMediumCarPrice(service)
      ),
      duration: service.duration,
    });
    setIsModalOpen(true);
  }

  async function handleToggleStatus(
    serviceId: number
  ) {
    const service = services.find(
      (currentService) =>
        currentService.id === serviceId
    );

    if (!service) {
      return;
    }

    const updated = await onSetActive(
      serviceId,
      !service.active
    );

    if (!updated) {
      return;
    }

  }

  async function handleDelete(
    serviceId: number
  ) {
    const removed = await onRemove(serviceId);

    if (!removed) {
      return;
    }

    if (editingServiceId === serviceId) {
      closeModal();
    }
  }

  return (
    <>
      <PremiumCard className="v7-card-fill" contentClassName="v7-card-content">
        <div className="v7-card-header flex flex-col gap-5">
          <div className="flex items-center justify-between gap-4">
            <SectionTitle title="Serviços cadastrados" />

            <button
              type="button"
              className="service-action-button btn btn-primary btn-compact shrink-0 whitespace-nowrap"
            onClick={openCreateModal}
          >
              <Plus className="h-3.5 w-3.5" aria-hidden="true" />
              Novo serviço
            </button>
          </div>

          <div className="flex flex-col gap-3 border-y border-white/[0.06] py-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="grid max-w-[330px] grid-cols-3 gap-2">
              <CounterCard label="Total" value={services.length} />
              <CounterCard label="Ativos" value={activeServices} />
              <CounterCard label="Inativos" value={inactiveServices} />
            </div>

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

        <div className="v7-list-scroll premium-scroll mt-4 pr-1">
          {filteredServices.length === 0 && (
            <EmptyState message="Nenhum serviço encontrado para este filtro." />
          )}

          {paginatedServices.map((service) => (
            <div
              key={service.id}
              className="border-b border-white/[0.06] py-4 last:border-b-0"
            >
              <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-center">
                <div className="min-w-0">
                  <p className="min-w-0 truncate text-base font-semibold text-white">
                    {service.name}
                  </p>

                  <div className="mt-2 flex min-w-0 flex-wrap gap-x-4 gap-y-1 text-sm text-zinc-500">
                    <span className="max-w-[180px] truncate">{service.category}</span>
                    <span className="whitespace-nowrap">
                      {getLavaServicePriceSummary(service)}
                    </span>
                    <span className="whitespace-nowrap">{service.duration}</span>
                  </div>
                </div>

                <div className="grid gap-2 sm:grid-cols-3 xl:min-w-[360px]">
                  <button
                    type="button"
                    className="service-action-button btn btn-secondary btn-compact"
                    onClick={() => handleEdit(service)}
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    Editar
                  </button>

                  <button
                    type="button"
                    className="service-action-button btn btn-secondary btn-compact"
                    onClick={() => handleToggleStatus(service.id)}
                  >
                    {service.active ? (
                      <PowerOff className="h-3.5 w-3.5" aria-hidden="true" />
                    ) : (
                      <Power className="h-3.5 w-3.5" aria-hidden="true" />
                    )}

                    {service.active ? "Desativar" : "Ativar"}
                  </button>

                  <button
                    type="button"
                    className="service-action-button btn btn-danger btn-compact"
                    onClick={() => handleDelete(service.id)}
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                    Apagar
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="v7-card-footer">
          {filteredServices.length > servicesPerPage && (
            <Pagination
              currentPage={safeCurrentPage}
              totalPages={totalPages}
              totalItems={filteredServices.length}
              pageStart={pageStart}
              pageSize={paginatedServices.length}
              onPrevious={() => setCurrentPage(Math.max(1, safeCurrentPage - 1))}
              onNext={() => setCurrentPage(Math.min(totalPages, safeCurrentPage + 1))}
            />
          )}
        </div>
      </PremiumCard>

      <AnimatedModal
        open={isModalOpen}
        onClose={closeModal}
        labelledBy="lava-service-modal-title"
        describedBy="lava-service-modal-description"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] shadow-[0_10px_34px_rgba(0,0,0,0.28)]"
      >
        <div className="flex items-start justify-between gap-4 border-b border-white/10 p-5">
          <div>
            <h3
              id="lava-service-modal-title"
              className="text-lg font-semibold uppercase tracking-[0.04em] text-white"
            >
              {editingService ? "Editar serviço" : "Novo serviço"}
            </h3>

            <p
              id="lava-service-modal-description"
              className="mt-1 text-sm text-zinc-500"
            >
              {editingService
                ? "Atualize os dados do serviço."
                : "Cadastre um novo serviço para o lava jato."}
            </p>
          </div>

          <button
            type="button"
            onClick={closeModal}
            className="grid h-8 w-8 shrink-0 place-items-center rounded-[4px] text-zinc-500 transition hover:bg-white/[0.05] hover:text-white"
            aria-label="Fechar modal"
            title="Fechar"
          >
            <X
              className="h-4 w-4"
              aria-hidden="true"
            />
          </button>
        </div>

        <div className="space-y-4 p-5">
          <TextField
            label="Nome do serviço"
            value={form.name}
            placeholder="Ex: Lavagem completa"
            onChange={(value) => updateForm("name", value)}
          />

          <TextField
            label="Categoria"
            value={form.category}
            placeholder="Ex: Lavagem, Detalhamento"
            onChange={(value) => updateForm("category", value)}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="Preço carro pequeno"
              value={form.smallCarPrice}
              placeholder="R$ 35,00"
              onChange={(value) =>
                updateForm(
                  "smallCarPrice",
                  formatCurrencyInput(value)
                )
              }
            />

            <TextField
              label="Preço carro médio"
              value={form.mediumCarPrice}
              placeholder="R$ 45,00"
              onChange={(value) =>
                updateForm(
                  "mediumCarPrice",
                  formatCurrencyInput(value)
                )
              }
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="Tempo"
              value={form.duration}
              placeholder="40 min"
              onChange={(value) => updateForm("duration", value)}
            />
          </div>

          {formError ? (
            <div
              className="rounded-[4px] border border-[#D4AF37]/20 bg-[#D4AF37]/10 px-3 py-2 text-sm text-[#F2C94C]"
              role="alert"
            >
              {formError}
            </div>
          ) : null}
        </div>

        <div className="flex justify-end gap-2 border-t border-white/10 p-5">
          <button
            type="button"
            onClick={closeModal}
            className="h-10 rounded-[4px] border border-white/10 px-4 text-sm text-zinc-400 transition hover:border-white/20 hover:text-white"
          >
            Cancelar
          </button>

          <button
            type="button"
            onClick={() => void handleSubmit()}
            className="btn-primary h-10 rounded-[4px] px-4 text-sm font-medium"
          >
            {editingService ? "Salvar alterações" : "Criar serviço"}
          </button>
        </div>
      </AnimatedModal>
    </>
  );
}

