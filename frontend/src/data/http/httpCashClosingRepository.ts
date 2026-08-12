import type {
  CashClosingDay,
  CashClosingReport,
  CashClosingRepository,
} from "../contracts/cash-closing.repository";

import {
  httpClient,
} from "../../shared/http";

export const httpCashClosingRepository: CashClosingRepository = {
  get(day: CashClosingDay) {
    return httpClient.get<CashClosingReport>(
      `/cash-closing/${day}`
    );
  },

  close(day: CashClosingDay) {
    return httpClient.post<CashClosingReport>(
      `/cash-closing/${day}/close`
    );
  },
};
