const shortTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
});

export function formatShortTime(date: Date): string {
  return shortTimeFormatter.format(date);
}

export function getCurrentShortTime(): string {
  return formatShortTime(new Date());
}
