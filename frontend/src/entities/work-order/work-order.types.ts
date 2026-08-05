export type LavaOrderPaymentMethod =
  | "Pendente"
  | "Crédito"
  | "Débito"
  | "Cartão"
  | "Pix"
  | "Dinheiro";

export type LavaOrderFiscalDocument =
  "Recibo geral";

export type LavaVehicleSize = "Pequeno" | "Médio";

export type LavaOrderClientMode =
  | "registered"
  | "guest";

export type LavaOrderFormInput = {
  clientMode: LavaOrderClientMode;
  clientId: string;
  guestName: string;
  guestPhone: string;
  guestVehicle: string;
  guestPlate: string;
  serviceId: string;
  vehicleSize: LavaVehicleSize;
  paymentMethod: LavaOrderPaymentMethod;
  fiscalDocument: LavaOrderFiscalDocument;
};

export type CreateLavaOrderInput = {
  clientId: number | null;
  clientName: string;
  clientPhone?: string;
  serviceId: number;
  vehicle: string;
  plate: string;
  service: string;
  amount: number;
  vehicleSize: LavaVehicleSize;
  paymentMethod: LavaOrderPaymentMethod;
  fiscalDocument: LavaOrderFiscalDocument;
  serviceDurationMinutes?: number | null;
  notes?: string;
};

export type LavaProgressStage =
  | "waiting"
  | "prewash"
  | "washing"
  | "finishing"
  | "delivery"
  | "done"
  | "cancelled";

export type LavaWorkOrderOperationalStatus =
  | "OPEN"
  | "PAYMENT_PENDING"
  | "PAID"
  | "COMPLETED"
  | "CANCELLED";

export type LavaOperationalStage = Exclude<
  LavaProgressStage,
  "cancelled"
>;

export type LavaProgressStageDefinition = {
  id: LavaOperationalStage;
  label: string;
  shortLabel: string;
};

export type LavaProgressVehicle = {
  id: number;
  financeEntryId: number;
  clientId: number | null;
  clientName?: string;
  clientPhone?: string;
  serviceId: number | null;
  vehicle: string;
  plate: string;
  service: string;
  vehicleSize: LavaVehicleSize | null;
  notes?: string;
  stage: LavaProgressStage;
  elapsed: string;
  amount?: number;
  checkoutId?: string | null;
  checkoutStatus?: string | null;
  operationalStatus?: LavaWorkOrderOperationalStatus;
  prepared?: boolean;
  paymentStatus?: string | null;
  cashReceived?: number | null;
  cashChange?: number | null;
  paidAt?: string;
  createdAt?: string;
  serviceDurationMinutes?: number | null;
  serviceTimeNotifiedAt?: string;
};

export type LavaHistoryEntry = {
  id: number;
  checkoutId: string | null;
  paymentId?: string | null;
  paymentReversedAt?: string | null;
  paymentReversalReason?: string | null;
  clientName: string;
  clientPhone: string;
  vehicle: string;
  plate: string;
  vehicleSize: LavaVehicleSize | null;
  service: string;
  serviceCount: number;
  receiptItems?: {
    quantity: number;
    name: string;
    unitPrice: number;
    total: number;
    kind?: "service";
  }[];
  amount: number;
  method: import("../payment").LavaHistoricalPaymentMethod;
  document: LavaOrderFiscalDocument;
  status:
    | "Concluída"
    | "Estorno pendente"
    | "Estornada";
  paymentStatus:
    | "APPROVED"
    | "REVERSAL_PENDING"
    | "REVERSED";
  time: string;
  createdAt: string;
  paidAt: string | null;
  completedAt: string;
  detailPath: string;
};
