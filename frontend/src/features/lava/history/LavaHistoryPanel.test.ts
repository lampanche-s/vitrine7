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
  LAVA_HISTORY_PAGE_SIZE,
  LavaHistoryPanel,
} from "./LavaHistoryPanel";
import {
  PaginationFooter,
  ToastProvider,
} from "../../../components/ui";
import {
  mockCurrentSession,
} from "../../../data/test-fixtures/session.fixture";
import {
  AccessProvider,
} from "../../access";

const historyEntry = {
  id: 21,
  checkoutId:
    "11111111-1111-4111-8111-111111111111",
  clientName: "Marcos Almeida",
  clientPhone: "(71) 98824-1092",
  vehicle: "Honda Civic",
  plate: "QXZ8A21",
  vehicleSize: "Médio" as const,
  service: "Lavagem completa",
  serviceCount: 1,
  receiptItems: [
    {
      quantity: 1,
      name: "Lavagem completa",
      unitPrice: 45,
      total: 45,
    },
  ],
  amount: 45,
  method: "Pix" as const,
  document: "Recibo geral" as const,
  status: "Concluída" as const,
  paymentStatus: "APPROVED" as const,
  time: "18/07/2026, 10:12",
  createdAt: "2026-07-18T10:00:00Z",
  paidAt: "2026-07-18T10:10:00Z",
  completedAt: "2026-07-18T10:12:00Z",
  detailPath: "/lava/work-orders/21",
};

describe("LavaHistoryPanel", () => {
  it("usa 7 registros por página no histórico real", () => {
    expect(LAVA_HISTORY_PAGE_SIZE).toBe(7);
  });

  it("renderiza loading discreto antes da primeira resposta paginada", () => {
    const html = renderToString(
      createElement(
        AccessProvider,
        {
          session: mockCurrentSession,
          children: createElement(
            ToastProvider,
            null,
            createElement(LavaHistoryPanel, {
              entries: [historyEntry],
              onLoadHistory: vi.fn(),
            })
          ),
        }
      )
    );

    expect(html).toContain("Carregando histórico");
    expect(html).not.toContain("OS 21");
    expect(html).not.toContain("Pagamento backend");
    expect(html).not.toContain("APPROVED");
  });

  it("renderiza controles de paginação quando o backend informa mais de uma página", () => {
    const html = renderToString(
      createElement(PaginationFooter, {
        page: 1,
        totalPages: 3,
        totalItems: 42,
        first: true,
        last: false,
        onPrevious: vi.fn(),
        onNext: vi.fn(),
      })
    );

    expect(html).toContain("Anterior");
    expect(html).toContain("Página");
    expect(html).toContain("1");
    expect(html).toContain("3");
    expect(html).toContain("42");
    expect(html).toContain("registros");
    expect(html).toContain("Próxima");
    expect(html).toContain("disabled");
  });

});
