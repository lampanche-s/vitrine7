export type SystemBooleanSettingKey =
  | "adminMode";

export type SystemSettings = {
  companyName: string;
  cnpj: string;
  phone: string;
  address: string;
  adminMode: boolean;
};
