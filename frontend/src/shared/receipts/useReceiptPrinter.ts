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

  const printReceipt = useCallback(
    async (checkoutId: string) => {
      if (isPrinting) {
        return;
      }

      setIsPrinting(true);

      try {
        const job =
          await httpClient.post<PrintJobCreated>(
            `/checkouts/${checkoutId}/print-jobs`
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
              title: "Impresso",
              description:
                "O comprovante foi enviado para a impressora térmica.",
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

  return {
    isPrinting,
    printReceipt,
  };
}
