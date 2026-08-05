export type Client = {
  id: number;
  name: string;
  phone: string;
  vehicle: string;
  plate: string;
  active: boolean;
};

export type ClientInput = {
  name: string;
  phone: string;
  vehicle: string;
  plate: string;
};
