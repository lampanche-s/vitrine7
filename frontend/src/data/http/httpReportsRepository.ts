import type {
  ReportsRepository,
  SalesReportScope,
} from "../contracts/reports.repository";

import {
  httpClient,
} from "../../shared/http";

function queryString(input: {
  from?: string;
  to?: string;
  scope?: SalesReportScope;
}) {
  const params = new URLSearchParams();

  if (input.from) {
    params.set("from", input.from);
  }

  if (input.to) {
    params.set("to", input.to);
  }

  params.set("scope", input.scope ?? "ALL");

  return params.toString();
}

export const httpReportsRepository: ReportsRepository = {
  summary(input = {}) {
    return httpClient.get(
      `/reports/sales/summary?${queryString(input)}`
    );
  },

  export(input) {
    return httpClient.get(
      `/reports/sales/export?${queryString(input)}`,
      {
        timeoutMs: 60_000,
      }
    );
  },

  downloadSystemBackup() {
    return httpClient.download(
      "/system/backup",
      {
        method: "POST",
        timeoutMs: 300_000,
      }
    );
  },
};
