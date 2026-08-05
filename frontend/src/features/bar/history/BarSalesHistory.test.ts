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
} from "vitest";

import {
  BarSalesHistory,
} from "./BarSalesHistory";
import {
  ToastProvider,
} from "../../../components/ui";
import {
  mockCurrentSession,
} from "../../../data/test-fixtures/session.fixture";
import {
  AccessProvider,
} from "../../access";

describe("BarSalesHistory", () => {
  it("renderiza loading discreto antes da primeira resposta paginada", () => {
    const html = renderToString(
      createElement(
        AccessProvider,
        {
          session: mockCurrentSession,
          children: createElement(
            ToastProvider,
            null,
            createElement(BarSalesHistory, {
              onLoadHistory: async () => ({
                entries: [],
                page: 0,
                totalPages: 1,
                totalElements: 0,
              }),
            })
          ),
        }
      )
    );

    expect(html).toContain("Carregando histórico");
    expect(html).not.toContain("Comanda 7");
  });
});
