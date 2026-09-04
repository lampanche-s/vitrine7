import {
  useCallback,
  useState,
} from "react";

import {
  useToast,
} from "../../components/ui";

import {
  httpClient,
} from "../http";

type PrintJobCreated = {
  id: string;
  status: "PENDING" | string;
};

type PrintJobStatus = {
  id: string;
  status:
    | "PENDING"
    | "PRINTING"
    | "PRINTED"
    | "FAILED"
    | string;
  errorMessage: string | null;
  completedAt: string | null;
};

function wait(milliseconds: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, milliseconds);
  });
}

export function useReceiptPrinter() {
  const [isPrinting, setIsPrinting] =
    useState(false);
  const {
    showToast,
    showErrorToast,
  } = useToast();

  const runPrintJob = useCallback(
    async (
      createPath: string,
      successTitle: string,
      successDescription: string
    ) => {
      if (isPrinting) {
        return;
      }

      setIsPrinting(true);

      try {
        const job =
          await httpClient.post<PrintJobCreated>(
            createPath
          );

        for (let attempt = 0; attempt < 20; attempt += 1) {
          await wait(500);

          const status =
            await httpClient.get<PrintJobStatus>(
              `/print-jobs/${job.id}`,
              {
                userActivity: false,
              }
            );

          if (status.status === "PRINTED") {
            showToast({
              title: successTitle,
              description: successDescription,
              variant: "success",
            });
            return;
          }

          if (status.status === "FAILED") {
            throw new Error(
              status.errorMessage ||
                "A impressora não conseguiu concluir a impressão."
            );
          }
        }

        showToast({
          title: "Impressão enviada",
          description:
            "O trabalho ficou aguardando confirmação do agente de impressão.",
          variant: "info",
        });
      } catch (error) {
        showErrorToast(error, {
          title:
            "Não foi possível imprimir",
          guidance:
            "Verifique se o agente está aberto e se a impressora configurada está disponível.",
        });
      } finally {
        setIsPrinting(false);
      }
    },
    [
      isPrinting,
      showErrorToast,
      showToast,
    ]
  );

  const printReceipt = useCallback(
    async (checkoutId: string) => {
      await runPrintJob(
        `/checkouts/${checkoutId}/print-jobs`,
        "Impresso",
        "O comprovante foi enviado para a impressora térmica."
      );
    },
    [runPrintJob]
  );

  const printPrePaymentNote = useCallback(
    async (tabId: number) => {
      await runPrintJob(
        `/bar/tabs/${tabId}/prepayment-print-jobs`,
        "Nota impressa",
        "A conferência de consumo foi enviada para a impressora térmica."
      );
    },
    [runPrintJob]
  );

  const printCashClosing = useCallback(
    async (day: "TODAY" | "YESTERDAY") => {
      await runPrintJob(
        `/cash-closing/${day}/print-jobs`,
        "Fechamento impresso",
        "O fechamento de caixa foi enviado para a impressora térmica."
      );
    },
    [runPrintJob]
  );

  return {
    isPrinting,
    printReceipt,
    printPrePaymentNote,
    printCashClosing,
  };
}
