import { useEffect, useMemo, useRef, useState } from "react";
import { RefreshCw } from "lucide-react";

import { Button, ContentStack, DropdownSelect, PremiumCard, TextField } from "../../../components/ui";
import type { ReportsRepository, SalesReport, SalesReportScope } from "../../../data/contracts";
import { UnifiedReportExportActions } from "../../reports/UnifiedReportExportActions";
import {
  DEFAULT_REPORT_PERIOD,
  getDefaultCustomReportDates,
  isProtectedReportPeriod,
  reportPeriodOptions,
  requiresPasswordForPeriodSelection,
  resolveReportSummaryInput,
  type ReportPeriodPreset,
} from "../../reports/report-period";
import { BarReportSections } from "./BarReportSections";
import { createReportRequestGuard } from "./barReports.selectors";
import { ProtectedReportPeriodModal } from "./ProtectedReportPeriodModal";

const scopeOptions = [
  { value: "ALL", label: "Todos" },
  { value: "ITEM", label: "Itens" },
  { value: "SERVICE", label: "Serviços" },
];

export function BarReportsSummary({ report, isLoading = false }: { report: SalesReport | null; isLoading?: boolean }) {
  return <BarReportSections report={report} isLoading={isLoading} />;
}

export function BarReportsQueryError({ message }: { message: string }) {
  return <div className="rounded-[4px] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-danger)]" role="alert">{message}</div>;
}

export function BarReports({ repository }: { repository: ReportsRepository }) {
  const defaults = useMemo(() => getDefaultCustomReportDates(), []);
  const [activePeriod, setActivePeriod] = useState<ReportPeriodPreset>(DEFAULT_REPORT_PERIOD);
  const [pendingPeriod, setPendingPeriod] = useState<ReportPeriodPreset | null>(null);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [reportPassword, setReportPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [scope, setScope] = useState<SalesReportScope>("ALL");
  const [customFrom, setCustomFrom] = useState(defaults.from);
  const [customTo, setCustomTo] = useState(defaults.to);
  const [report, setReport] = useState<SalesReport | null>(null);
  const [loadError, setLoadError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const requestGuard = useRef(createReportRequestGuard());
  const authorizedPassword = useRef("");

  function handlePeriodChange(nextPeriod: ReportPeriodPreset) {
    if (nextPeriod === activePeriod) return;

    if (requiresPasswordForPeriodSelection(activePeriod, nextPeriod)) {
      setPendingPeriod(nextPeriod);
      setPasswordModalOpen(true);
      setReportPassword("");
      setPasswordError("");
      return;
    }

    authorizedPassword.current = "";
    setReportPassword("");
    setPasswordError("");
    setPendingPeriod(null);
    setPasswordModalOpen(false);
    setActivePeriod(nextPeriod);
  }

  function cancelPasswordModal() {
    setPendingPeriod(null);
    setPasswordModalOpen(false);
    setReportPassword("");
    setPasswordError("");
  }

  async function confirmProtectedPeriod() {
    if (!pendingPeriod) return;

    try {
      await repository.verifyProtectedReportPassword(reportPassword);
      authorizedPassword.current = reportPassword;
      setActivePeriod(pendingPeriod);
      setPendingPeriod(null);
      setPasswordModalOpen(false);
      setReportPassword("");
      setPasswordError("");
    } catch {
      setPasswordError("Senha incorreta.");
    }
  }

  useEffect(() => {
    const guard = requestGuard.current;
    const isCurrent = guard.begin();

    async function load() {
      setReport(null);
      setLoadError("");
      setIsLoading(true);
      try {
        const dates = resolveReportSummaryInput(activePeriod, customFrom, customTo);
        const response = await repository.summary({
          ...dates,
          scope,
          reportPassword: isProtectedReportPeriod(activePeriod)
            ? authorizedPassword.current
            : undefined,
        });
        if (isCurrent()) setReport(response);
      } catch (error) {
        if (isCurrent()) setLoadError(error instanceof Error ? error.message : "Não foi possível carregar o relatório.");
      } finally {
        if (isCurrent()) setIsLoading(false);
      }
    }

    void load();
    return () => guard.invalidate();
  }, [activePeriod, customFrom, customTo, reloadKey, repository, scope]);

  return (
    <ContentStack>
      <PremiumCard>
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-end">
            <div className="grid flex-1 gap-4 sm:grid-cols-2">
              <DropdownSelect
                label="Período"
                value={activePeriod}
                placeholder="Selecione o período"
                listMaxHeight={320}
                options={reportPeriodOptions.map((option) => ({ value: option.value, label: option.label, description: option.description }))}
                onChange={(value) => handlePeriodChange(value as ReportPeriodPreset)}
              />
              <DropdownSelect label="Escopo" value={scope} placeholder="Selecione o escopo" options={scopeOptions} onChange={(value) => setScope(value as SalesReportScope)} />
            </div>
            {activePeriod === "custom" ? (
              <div className="grid flex-1 gap-4 sm:grid-cols-2">
                <TextField label="Data inicial" type="date" value={customFrom} onChange={setCustomFrom} />
                <TextField label="Data final" type="date" value={customTo} onChange={setCustomTo} />
              </div>
            ) : null}
            <Button size="compact" variant="secondary" leadingIcon={<RefreshCw />} disabled={isLoading} onClick={() => setReloadKey((value) => value + 1)}>Atualizar</Button>
          </div>

          <div className="border-t border-[var(--border-subtle)] pt-4">
            <UnifiedReportExportActions
              repository={repository}
              activePeriod={activePeriod}
              activeCustomFrom={customFrom}
              activeCustomTo={customTo}
              getReportPassword={() => isProtectedReportPeriod(activePeriod) ? authorizedPassword.current : undefined}
            />
          </div>
        </div>
      </PremiumCard>
      {loadError ? <BarReportsQueryError message={loadError} /> : null}
      <BarReportsSummary report={report} isLoading={isLoading} />
      <ProtectedReportPeriodModal
        open={passwordModalOpen}
        password={reportPassword}
        error={passwordError}
        onPasswordChange={(value) => {
          setReportPassword(value);
          setPasswordError("");
        }}
        onCancel={cancelPasswordModal}
        onConfirm={() => void confirmProtectedPeriod()}
      />
    </ContentStack>
  );
}
