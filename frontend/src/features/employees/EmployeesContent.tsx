import { useEffect, useState } from "react";
import { ContentStack, FilterChip, useToast } from "../../components/ui";
import { repositories } from "../../data/repositories";
import type { Employee, EmployeeInput } from "../../entities/employee";
import { ModuleAccessGuard } from "../access";
import { EmployeesManager } from "./EmployeesManager";
import { EmployeeVoucherHistory } from "./EmployeeVoucherHistory";
import { EmployeeVoucherReports } from "./EmployeeVoucherReports";

type Tab = "employees" | "history" | "reports";
export function EmployeesContent() {
  return (
    <ModuleAccessGuard moduleId="clients">
      <EmployeesContentView />
    </ModuleAccessGuard>
  );
}
function EmployeesContentView() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [tab, setTab] = useState<Tab>("employees");
  const { showToast } = useToast();
  useEffect(() => {
    repositories.employees
      .list()
      .then(setEmployees)
      .catch((error: unknown) =>
        showToast({
          title:
            error instanceof Error
              ? error.message
              : "Não foi possível carregar os funcionários.",
          variant: "error",
        }),
      );
  }, [showToast]);
  async function create(input: EmployeeInput) {
    try {
      const value = await repositories.employees.create(input);
      setEmployees((current) =>
        [...current, value].sort((a, b) => a.name.localeCompare(b.name)),
      );
      return true;
    } catch (error) {
      showToast({
        title:
          error instanceof Error
            ? error.message
            : "Não foi possível cadastrar.",
        variant: "error",
      });
      return false;
    }
  }
  async function update(id: number, input: EmployeeInput) {
    try {
      const value = await repositories.employees.update(id, input);
      setEmployees((current) =>
        current.map((item) => (item.id === id ? value : item)),
      );
      return true;
    } catch (error) {
      showToast({
        title:
          error instanceof Error ? error.message : "Não foi possível editar.",
        variant: "error",
      });
      return false;
    }
  }
  async function remove(id: number) {
    try {
      await repositories.employees.remove(id);
      setEmployees((current) => current.filter((item) => item.id !== id));
      return true;
    } catch (error) {
      showToast({
        title:
          error instanceof Error ? error.message : "Não foi possível excluir.",
        variant: "error",
      });
      return false;
    }
  }
  return (
    <ContentStack>
      <div className="flex flex-wrap gap-2">
        <FilterChip
          label="Funcionários"
          active={tab === "employees"}
          onClick={() => setTab("employees")}
        />
        <FilterChip
          label="Histórico de vales"
          active={tab === "history"}
          onClick={() => setTab("history")}
        />
        <FilterChip
          label="Relatórios de vales"
          active={tab === "reports"}
          onClick={() => setTab("reports")}
        />
      </div>
      {tab === "employees" ? (
        <EmployeesManager
          employees={employees}
          onCreate={create}
          onUpdate={update}
          onRemove={remove}
        />
      ) : tab === "history" ? (
        <EmployeeVoucherHistory employees={employees} />
      ) : (
        <EmployeeVoucherReports employees={employees} />
      )}
    </ContentStack>
  );
}
