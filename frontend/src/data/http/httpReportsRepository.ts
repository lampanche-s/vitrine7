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

function reportPasswordHeaders(reportPassword?: string) {
  return reportPassword
    ? { "X-Report-Password": reportPassword }
    : undefined;
}

export const httpReportsRepository: ReportsRepository = {
  summary(input = {}) {
    const headers = reportPasswordHeaders(input.reportPassword);
    const path = `/reports/sales/summary?${queryString(input)}`;
    return headers
      ? httpClient.get(path, { headers })
      : httpClient.get(path);
  },

  export(input) {
    return httpClient.get(
      `/reports/sales/export?${queryString(input)}`,
      {
        timeoutMs: 60_000,
        headers: reportPasswordHeaders(input.reportPassword),
      }
    );
  },

  verifyProtectedReportPassword(password) {
    return httpClient.post(
      "/reports/access/verify",
      { password }
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
