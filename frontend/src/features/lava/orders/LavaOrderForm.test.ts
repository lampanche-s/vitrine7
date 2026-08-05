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
  LavaOrderForm,
} from "./LavaOrderForm";

describe("LavaOrderForm", () => {
  it("renderiza abertura com pagamento imediato", () => {
    const html = renderToString(
      createElement(
        ToastProvider,
        null,
        createElement(LavaOrderForm, {
          clients: [
            {
              id: 1,
              name: "Ana Souza",
              phone: "(71) 99999-0000",
              vehicle: "Honda Fit",
              plate: "ABC1D23",
              lastService: "Hoje",
              visits: 3,
              active: true,
            },
          ],
          services: [
            {
              id: 10,
              name: "Lavagem completa",
              category: "Lavagem",
              price: "R$ 40,00",
              smallCarPrice: "R$ 40,00",
              mediumCarPrice: "R$ 55,00",
              duration: "30 min",
              active: true,
            },
          ],
          onCreateOrder: vi.fn(),
          onPayOrder: vi.fn(),
          onCompleteOrder: vi.fn(),
        })
      )
    );

    expect(html).toContain("Abrir OS e pagar");
    expect(html).toContain("Pagamento");
    expect(html).toContain("Valor recebido");
    expect(html).toContain("Troco");
  });
});
