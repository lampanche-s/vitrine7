export type ReportScope =
  | "ITEM"
  | "SERVICE";

export type ReportPeriodPreset =
  | "today"
  | "previousDay"
  | "currentWeek"
  | "previousWeek"
  | "currentMonth"
  | "previousMonth"
  | "custom";

export const DEFAULT_REPORT_PERIOD: ReportPeriodPreset = "today";

export function isProtectedReportPeriod(
  period: ReportPeriodPreset
) {
  return period !== "today" && period !== "previousDay";
}

export function requiresPasswordForPeriodSelection(
  activePeriod: ReportPeriodPreset,
  nextPeriod: ReportPeriodPreset
) {
  return nextPeriod !== activePeriod && isProtectedReportPeriod(nextPeriod);
}

export type ReportDateRange = {
  from: string;
  to: string;
  startDate: string;
  endDate: string;
  label: string;
  fileSuffix: string;
};

export const reportPeriodOptions = [
  {
    value: "today",
    label: "Hoje",
    description: "Somente o dia de hoje",
  },
  {
    value: "previousDay",
    label: "Ontem",
    description: "Todo o dia de ontem",
  },
  {
    value: "currentWeek",
    label: "Semana atual",
    description: "De segunda-feira até hoje",
  },
  {
    value: "previousWeek",
    label: "Semana passada",
    description: "De segunda-feira a domingo",
  },
  {
    value: "currentMonth",
    label: "Mês atual",
    description: "Do primeiro dia do mês até hoje",
  },
  {
    value: "previousMonth",
    label: "Mês passado",
    description: "Todo o mês anterior",
  },
  {
    value: "custom",
    label: "Período personalizado",
    description: "Escolha as datas inicial e final",
  },
] as const;

export const exportReportPeriodOptions = reportPeriodOptions;

function startOfDay(date: Date) {
  const result = new Date(date);

  result.setHours(0, 0, 0, 0);

  return result;
}

function endOfDay(date: Date) {
  const result = new Date(date);

  result.setHours(23, 59, 59, 999);

  return result;
}

function startOfWeek(date: Date) {
  const result = startOfDay(date);
  const day = result.getDay();

  const distanceFromMonday =
    day === 0
      ? 6
      : day - 1;

  result.setDate(
    result.getDate() -
      distanceFromMonday
  );

  return result;
}

function toInputDate(date: Date) {
  const year = date.getFullYear();

  const month = String(
    date.getMonth() + 1
  ).padStart(2, "0");

  const day = String(
    date.getDate()
  ).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function fromInputDate(
  value: string,
  end: boolean
) {
  const [
    year,
    month,
    day,
  ] = value
    .split("-")
    .map(Number);

  if (
    !year ||
    !month ||
    !day
  ) {
    throw new Error(
      "Informe as datas inicial e final."
    );
  }

  return new Date(
    year,
    month - 1,
    day,
    end ? 23 : 0,
    end ? 59 : 0,
    end ? 59 : 0,
    end ? 999 : 0
  );
}

function formatDate(date: Date) {
  return new Intl.DateTimeFormat(
    "pt-BR"
  ).format(date);
}

function createRange(
  start: Date,
  end: Date
): ReportDateRange {
  const normalizedStart =
    startOfDay(start);

  const normalizedEnd =
    endOfDay(end);

  if (
    normalizedStart.getTime() >
    normalizedEnd.getTime()
  ) {
    throw new Error(
      "A data inicial não pode ser posterior à data final."
    );
  }

  const startDate =
    toInputDate(normalizedStart);

  const endDate =
    toInputDate(normalizedEnd);

  return {
    from:
      normalizedStart.toISOString(),

    to:
      normalizedEnd.toISOString(),

    startDate,
    endDate,

    label:
      startDate === endDate
        ? formatDate(normalizedStart)
        : `${formatDate(
            normalizedStart
          )} a ${formatDate(
            normalizedEnd
          )}`,

    fileSuffix:
      startDate === endDate
        ? startDate
        : `${startDate}-a-${endDate}`,
  };
}

export function getDefaultCustomReportDates(
  now = new Date()
) {
  return {
    from: toInputDate(
      new Date(
        now.getFullYear(),
        now.getMonth(),
        1
      )
    ),

    to: toInputDate(now),
  };
}

export function resolveReportDateRange(
  preset: ReportPeriodPreset,
  customFrom: string,
  customTo: string,
  now = new Date()
): ReportDateRange {
  const today =
    startOfDay(now);

  if (preset === "today") {
    return createRange(
      today,
      today
    );
  }

  if (
    preset === "previousDay"
  ) {
    const previousDay =
      new Date(today);

    previousDay.setDate(
      previousDay.getDate() - 1
    );

    return createRange(
      previousDay,
      previousDay
    );
  }

  if (
    preset === "currentWeek"
  ) {
    return createRange(
      startOfWeek(today),
      today
    );
  }

  if (
    preset === "previousWeek"
  ) {
    const currentWeekStart =
      startOfWeek(today);

    const previousWeekEnd =
      new Date(currentWeekStart);

    previousWeekEnd.setDate(
      previousWeekEnd.getDate() - 1
    );

    const previousWeekStart =
      new Date(previousWeekEnd);

    previousWeekStart.setDate(
      previousWeekStart.getDate() - 6
    );

    return createRange(
      previousWeekStart,
      previousWeekEnd
    );
  }

  if (
    preset === "currentMonth"
  ) {
    return createRange(
      new Date(
        today.getFullYear(),
        today.getMonth(),
        1
      ),
      today
    );
  }

  if (
    preset === "previousMonth"
  ) {
    const previousMonthStart =
      new Date(
        today.getFullYear(),
        today.getMonth() - 1,
        1
      );

    const previousMonthEnd =
      new Date(
        today.getFullYear(),
        today.getMonth(),
        0
      );

    return createRange(
      previousMonthStart,
      previousMonthEnd
    );
  }

  return createRange(
    fromInputDate(
      customFrom,
      false
    ),
    fromInputDate(
      customTo,
      true
    )
  );
}

export function resolveReportSummaryInput(
  preset: ReportPeriodPreset,
  customFrom: string,
  customTo: string,
  now = new Date()
) {
  const range = resolveReportDateRange(
    preset,
    customFrom,
    customTo,
    now
  );

  return {
    from: range.startDate,
    to: range.endDate,
  };
}
