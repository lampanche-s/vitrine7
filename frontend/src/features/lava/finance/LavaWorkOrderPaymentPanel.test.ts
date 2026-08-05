import {
  createElement,
} from "react";
import {
  renderToString,
} from "react-dom/server";
import {
  describe,
  expect,
  it,
  vi,
} from "vitest";

import {
  ToastProvider,
} from "../../../components/ui";
import {
  LavaWorkOrderPaymentPanel,
} from "./LavaWorkOrderPaymentPanel";

describe("LavaWorkOrderPaymentPanel", () => {
  it("renderiza OS aberta para pagamento sem exibir OS já paga", () => {
    const html = renderToString(
      createElement(
        ToastProvider,
        null,
        createElement(LavaWorkOrderPaymentPanel, {
          workOrders: [
            {
              id: 21,
              financeEntryId: 21,
              clientId: 7,
              clientName: "Marcos Almeida",
              clientPhone: "(71) 98824-1092",
              serviceId: 11,
              vehicle: "Honda Civic",
              plate: "QXZ8A21",
              service: "Lavagem completa",
              vehicleSize: "Médio",
              stage: "waiting",
              elapsed: "Em aberto",
              amount: 45,
              checkoutStatus: "PAYMENT_FAILED",
              prepared: true,
              paymentStatus: "DECLINED",
            },
            {
              id: 22,
              financeEntryId: 22,
              clientId: 8,
              clientName: "Cliente pago",
              clientPhone: "",
              serviceId: 11,
              vehicle: "Jeep Compass",
              plate: "ABC1D23",
              service: "Lavagem completa",
              vehicleSize: "Pequeno",
              stage: "waiting",
              elapsed: "PAID",
              amount: 35,
              checkoutStatus: "FINALIZED",
              prepared: true,
              paymentStatus: "APPROVED",
              paidAt: "2026-07-18T10:12:00Z",
            },
          ],
          onPay: vi.fn(),
        })
      )
    );

    expect(html).toContain("OS 21");
    expect(html).toContain("Pagamento recusado");
    expect(html).not.toContain("Cliente pago");
  });
});
