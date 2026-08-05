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
  httpReportsRepository,
} from "./http/httpReportsRepository";

export const repositories = {
  admin: httpAdminRepository,
  bar: httpBarRepository,
  clients: httpClientsRepository,
  reports: httpReportsRepository,
} as const;
