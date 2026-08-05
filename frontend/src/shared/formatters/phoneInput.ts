import { onlyDigits } from "./digits";

export function formatBrazilianPhone(value: string) {
  const digits = onlyDigits(value).slice(0, 11);

  if (digits.length === 0) {
    return "";
  }

  if (digits.length <= 2) {
    return `(${digits}`;
  }

  const areaCode = digits.slice(0, 2);
  const phone = digits.slice(2);

  if (phone.length <= 4) {
    return `(${areaCode}) ${phone}`;
  }

  if (digits.length <= 10) {
    return `(${areaCode}) ${phone.slice(0, 4)}-${phone.slice(4)}`;
  }

  return `(${areaCode}) ${phone.slice(0, 5)}-${phone.slice(5)}`;
}
