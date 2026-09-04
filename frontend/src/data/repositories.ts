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
import {
  httpSuppliersRepository,
} from "./http/httpSuppliersRepository";
import { httpEmployeesRepository } from "./http/httpEmployeesRepository";

export const repositories = {
  admin: httpAdminRepository,
  bar: httpBarRepository,
  clients: httpClientsRepository,
  suppliers: httpSuppliersRepository,
  employees: httpEmployeesRepository,
  cashClosing: httpCashClosingRepository,
  reports: httpReportsRepository,
} as const;
