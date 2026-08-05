export type LavaService = {
  id: number;
  name: string;
  category: string;
  price: string;
  smallCarPrice: string;
  mediumCarPrice: string;
  duration: string;
  active: boolean;
};

export type LavaServiceInput = {
  name: string;
  category: string;
  price?: string;
  smallCarPrice: string;
  mediumCarPrice: string;
  duration: string;
};
