export type LavaClient = {
  id: number;
  name: string;
  phone: string;
  vehicle: string;
  plate: string;
  lastService: string;
  visits: number;
  active: boolean;
};

export type LavaClientInput = {
  name: string;
  phone: string;
  vehicle: string;
  plate: string;
};
