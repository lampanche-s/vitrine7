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


export type ClientConsumptionHistoryLine = {
  entryType: "ITEM" | "SERVICE";
  itemName: string;
  quantity: number;
  unitPrice: number;
  total: number;
};

export type ClientConsumptionHistoryEntry = {
  operationId: number;
  completedAt: string;
  total: number;
  paymentStatus: string;
  lines: ClientConsumptionHistoryLine[];
};
