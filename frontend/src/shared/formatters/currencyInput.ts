import { onlyDigits } from "./digits";
import { formatBrlCurrency } from "../lib/currency";

export function formatCurrencyInput(value: string) {
  const digits = onlyDigits(value);

  if (!digits) {
    return "";
  }

  const numericValue = Number(digits) / 100;

  return formatBrlCurrency(numericValue);
}

export function currencyInputToNumber(value: string) {
  const digits = onlyDigits(value);

  if (!digits) {
    return 0;
  }

  return Number(digits) / 100;
}
