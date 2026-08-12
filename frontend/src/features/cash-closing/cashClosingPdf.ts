import type {
  CashClosingReport,
} from "../../data/contracts/cash-closing.repository";

function brl(cents: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(cents / 100);
}

function dateTime(value: string | null) {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function paymentLabel(method: string) {
  switch (method) {
    case "CASH":
      return "Dinheiro";
    case "PIX":
      return "Pix";
    case "CREDIT_CARD":
      return "Crédito";
    case "DEBIT_CARD":
      return "Débito";
    default:
      return method;
  }
}

export async function exportCashClosingPdf(
  report: CashClosingReport
) {
  const {
    jsPDF,
  } = await import("jspdf");

  const pdf = new jsPDF({
    orientation: "portrait",
    unit: "mm",
    format: "a4",
  });

  const left = 16;
  const right = 194;
  let y = 18;

  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(16);
  pdf.text("Fechamento de Caixa", left, y);

  y += 8;
  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(10);
  pdf.text(`Operador: ${report.userName}`, left, y);
  y += 5;
  pdf.text(
    `Data: ${new Intl.DateTimeFormat("pt-BR").format(
      new Date(`${report.businessDate}T12:00:00`)
    )}`,
    left,
    y
  );
  y += 5;
  pdf.text(
    report.closed
      ? `Fechado em: ${dateTime(report.closedAt)}`
      : "Status: ainda não fechado",
    left,
    y
  );

  y += 8;
  pdf.line(left, y, right, y);
  y += 7;

  const metrics = [
    ["Total bruto vendido", brl(report.grossSalesCents)],
    ["Total líquido", brl(report.totalReceivedCents)],
    ["Vendas concluídas", String(report.saleCount)],
    ["Ticket médio", brl(report.averageTicketCents)],
    ["Estornos", `${report.reversedCount} (${brl(report.reversedCents)})`],
    ["Recebido em espécie", brl(report.cashReceivedCents)],
    ["Troco entregue", brl(report.cashChangeCents)],
    [
      "Comandas em aberto",
      `${report.openCommandCount} (${brl(report.openCommandAmountCents)})`,
    ],
    ["Primeira venda", dateTime(report.firstSaleAt)],
    ["Última venda", dateTime(report.lastSaleAt)],
  ];

  pdf.setFont("helvetica", "bold");
  for (const [label, value] of metrics) {
    pdf.text(label, left, y);
    pdf.setFont("helvetica", "normal");
    pdf.text(value, 70, y);
    pdf.setFont("helvetica", "bold");
    y += 6;
  }

  y += 2;
  pdf.line(left, y, right, y);
  y += 7;
  pdf.text("Formas de pagamento", left, y);
  y += 6;
  pdf.setFont("helvetica", "normal");

  if (report.paymentBreakdown.length === 0) {
    pdf.text("Nenhuma venda concluída.", left, y);
    y += 6;
  } else {
    for (const item of report.paymentBreakdown) {
      pdf.text(
        `${paymentLabel(item.method)} — ${item.saleCount} venda(s)`,
        left,
        y
      );
      pdf.text(brl(item.amountCents), right, y, {
        align: "right",
      });
      y += 6;
    }
  }

  y += 2;
  pdf.line(left, y, right, y);
  y += 7;
  pdf.setFont("helvetica", "bold");
  pdf.text("Operações do dia", left, y);
  y += 6;
  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(9);

  if (report.operations.length === 0) {
    pdf.text("Nenhuma operação no período.", left, y);
  } else {
    for (const operation of report.operations) {
      if (y > 280) {
        pdf.addPage();
        y = 18;
      }

      const status = operation.paymentStatus === "REVERSED"
        ? "Estornada"
        : "Concluída";

      pdf.text(
        `${dateTime(operation.completedAt)} · Comanda #${operation.operationId} · ${operation.displayName}`,
        left,
        y
      );
      y += 4.5;
      pdf.text(
        `${paymentLabel(operation.paymentMethod)} · ${status}`,
        left + 4,
        y
      );
      pdf.text(
        brl(operation.amountCents),
        right,
        y,
        { align: "right" }
      );
      y += 6;
    }
  }

  const fileDate = report.businessDate.replaceAll("-", "");
  pdf.save(`fechamento-caixa-${fileDate}.pdf`);
}
