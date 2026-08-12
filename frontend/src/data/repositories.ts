import {
  httpAdminRepository,
} from "./http/httpAdminRepository";

import {
  httpBarRepository,
} from "./http/httpBarRepository";

import {
  httpClientsRepository,
} from "./http/httpClientsRepository";

import {
  httpCashClosingRepository,
} from "./http/httpCashClosingRepository";

import {
  httpReportsRepository,
} from "./http/httpReportsRepository";

export const repositories = {
  admin: httpAdminRepository,
  bar: httpBarRepository,
  clients: httpClientsRepository,
  cashClosing: httpCashClosingRepository,
  reports: httpReportsRepository,
} as const;
