import {
  httpBarRepository,
} from "./http/httpBarRepository";

import {
  httpLavaRepository,
} from "./http/httpLavaRepository";

import {
  httpAdminRepository,
} from "./http/httpAdminRepository";

export const repositories = {
  bar: httpBarRepository,
  lava: httpLavaRepository,
  admin: httpAdminRepository,
} as const;
