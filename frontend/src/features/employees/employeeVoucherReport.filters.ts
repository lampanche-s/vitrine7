export type VoucherReportPeriod = "today" | "yesterday" | "currentWeek" | "lastWeek" | "currentMonth" | "lastMonth" | "custom";

export const voucherReportPeriodOptions = [
  { value: "today", label: "Hoje" },
  { value: "yesterday", label: "Ontem" },
  { value: "currentWeek", label: "Semana atual" },
  { value: "lastWeek", label: "Semana passada" },
  { value: "currentMonth", label: "Mês atual" },
  { value: "lastMonth", label: "Mês passado" },
  { value: "custom", label: "Personalizado" },
];

export function resolveVoucherReportRange(period: VoucherReportPeriod, customFrom: string, customTo: string, now = new Date()) {
  const start = new Date(now);
  const end = new Date(now);
  end.setHours(24, 0, 0, 0);
  if (period === "today") start.setHours(0, 0, 0, 0);
  if (period === "yesterday") { start.setDate(start.getDate() - 1); start.setHours(0, 0, 0, 0); end.setDate(end.getDate() - 1); }
  if (period === "currentWeek") { start.setDate(start.getDate() - ((start.getDay() + 6) % 7)); start.setHours(0, 0, 0, 0); }
  if (period === "lastWeek") { start.setDate(start.getDate() - ((start.getDay() + 6) % 7) - 7); start.setHours(0, 0, 0, 0); end.setTime(start.getTime()); end.setDate(end.getDate() + 7); }
  if (period === "currentMonth") { start.setDate(1); start.setHours(0, 0, 0, 0); }
  if (period === "lastMonth") { start.setMonth(start.getMonth() - 1, 1); start.setHours(0, 0, 0, 0); end.setFullYear(start.getFullYear(), start.getMonth() + 1, 1); end.setHours(0, 0, 0, 0); }
  if (period === "custom") {
    const customEnd = customTo ? new Date(`${customTo}T00:00:00`) : null;
    if (customEnd) customEnd.setDate(customEnd.getDate() + 1);
    return { from: customFrom ? new Date(`${customFrom}T00:00:00`).toISOString() : undefined, to: customEnd?.toISOString() };
  }
  return { from: start.toISOString(), to: end.toISOString() };
}
