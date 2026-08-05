/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  type KeyboardEvent,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  createPortal,
} from "react-dom";

import {
  AlertTriangle,
  ChevronRight,
  CheckCircle2,
  Info,
  X,
  XCircle,
} from "lucide-react";

import {
  AnimatedModal,
} from "./animated-modal";

import {
  Button,
} from "./button";

import type {
  LucideIcon,
} from "lucide-react";

type ToastVariant =
  | "success"
  | "error"
  | "warning"
  | "info";

type ToastInput = {
  title: string;
  description?: string;
  variant?: ToastVariant;
  duration?: number;
  dedupeKey?: string;
  errorDetails?: ErrorDetailsInput;
};

type ToastItemData = Required<
  Pick<ToastInput, "title" | "variant" | "duration">
> &
  Pick<ToastInput, "description" | "dedupeKey"> & {
    id: string;
    errorDetails?: ErrorDetails;
  };

type ToastContextValue = {
  showToast: (toast: ToastInput) => void;
  showErrorToast: (
    error: unknown,
    options?: ShowErrorToastOptions
  ) => void;
};

type ErrorDetailsInput = {
  title?: string;
  explanation?: string;
  guidance?: string;
};

type ErrorDetails = {
  title: string;
  explanation: string;
  guidance?: string;
};

type ShowErrorToastOptions = {
  title?: string;
  fallbackMessage?: string;
  guidance?: string;
  dedupeKey?: string;
};

const ToastContext =
  createContext<ToastContextValue | null>(null);

const toastIcons: Record<ToastVariant, LucideIcon> = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

function createToastId() {
  if (
    typeof crypto !== "undefined" &&
    "randomUUID" in crypto
  ) {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random()
    .toString(16)
    .slice(2)}`;
}

function getRawErrorMessage(
  error: unknown,
  fallbackMessage = "Não foi possível concluir a operação."
) {
  return error instanceof Error
    ? error.message
    : typeof error === "string"
      ? error
      : fallbackMessage;
}

function toFriendlyErrorText(message: string) {
  const withoutTechnicalLines = message
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => {
      if (!line) {
        return false;
      }

      return !/(^\s*(at |caused by:|stack trace)|https?:\/\/|\/api\/|endpoint|payload|json|sql|exception|java\.|javascript)/i.test(
        line
      );
    })
    .join(" ");

  const compact =
    withoutTechnicalLines || message.trim();

  return compact
    .replace(/\s+/g, " ")
    .replace(/["{}[\]]/g, "")
    .trim();
}

function getShortErrorMessage(message: string) {
  const friendly =
    toFriendlyErrorText(message) ||
    "Não foi possível concluir a operação.";
  const firstSentence =
    friendly.match(/.*?[.!?](?:\s|$)/)?.[0]?.trim() ??
    friendly;

  return firstSentence.length > 96
    ? `${firstSentence.slice(0, 93).trim()}...`
    : firstSentence;
}

function buildErrorDetails(
  title: string,
  message: string,
  guidance?: string
): ErrorDetails {
  return {
    title,
    explanation:
      toFriendlyErrorText(message) ||
      "A operação não pôde ser concluída.",
    guidance,
  };
}

export function ToastProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [
    toasts,
    setToasts,
  ] = useState<ToastItemData[]>([]);
  const [
    selectedErrorDetails,
    setSelectedErrorDetails,
  ] = useState<ErrorDetails | null>(null);

  const removeToast = useCallback(
    (id: string) => {
      setToasts((current) =>
        current.filter(
          (toast) => toast.id !== id
        )
      );
    },
    []
  );

  const showToast = useCallback(
    ({
      title,
      description,
      variant = "success",
      duration = 4000,
      dedupeKey,
      errorDetails,
    }: ToastInput) => {
      const normalizedTitle = title.trim();

      if (!normalizedTitle) {
        return;
      }

      const normalizedDescription =
        description?.trim();
      const resolvedErrorDetails =
        variant === "error"
          ? buildErrorDetails(
              errorDetails?.title ??
                "Detalhes do erro",
              errorDetails?.explanation ??
                normalizedDescription ??
                normalizedTitle,
              errorDetails?.guidance
            )
          : undefined;
      const resolvedDedupeKey =
        dedupeKey ??
        [
          variant,
          normalizedTitle,
          normalizedDescription ?? "",
        ].join("|");

      setToasts((current) => {
        if (
          current.some(
            (toast) =>
              toast.dedupeKey ===
              resolvedDedupeKey
          )
        ) {
          return current;
        }

        return [
          ...current,
          {
          id: createToastId(),
          title: normalizedTitle,
          description: normalizedDescription,
          variant,
          duration,
          dedupeKey: resolvedDedupeKey,
          errorDetails: resolvedErrorDetails,
          },
        ];
      });
    },
    []
  );

  const showErrorToast = useCallback(
    (
      error: unknown,
      options: ShowErrorToastOptions = {}
    ) => {
      const rawMessage = getRawErrorMessage(
        error,
        options.fallbackMessage
      );
      const shortMessage =
        getShortErrorMessage(rawMessage);

      showToast({
        title:
          options.title ??
          "Não foi possível concluir",
        description: shortMessage,
        variant: "error",
        dedupeKey:
          options.dedupeKey ??
          `error|${options.title ?? ""}|${shortMessage}`,
        errorDetails: {
          title:
            options.title ??
            "Não foi possível concluir",
          explanation: rawMessage,
          guidance: options.guidance,
        },
      });
    },
    [showToast]
  );

  const value = useMemo(
    () => ({
      showToast,
      showErrorToast,
    }),
    [showToast, showErrorToast]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}

      {typeof document !== "undefined"
        ? createPortal(
            <div
              className="toast-viewport fixed right-4 top-20 z-[320] flex w-[min(380px,calc(100vw-32px))] flex-col gap-2"
              aria-live="polite"
              aria-relevant="additions removals"
            >
              {toasts.map((toast) => (
                <ToastItem
                  key={toast.id}
                  toast={toast}
                  onRemove={removeToast}
                  onOpenDetails={
                    setSelectedErrorDetails
                  }
                />
              ))}
            </div>,
            document.body
          )
        : null}

      <ErrorDetailsModal
        details={selectedErrorDetails}
        onClose={() =>
          setSelectedErrorDetails(null)
        }
      />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context =
    useContext(ToastContext);

  if (!context) {
    throw new Error(
      "useToast deve ser usado dentro de ToastProvider."
    );
  }

  return context;
}

function ToastItem({
  toast,
  onRemove,
  onOpenDetails,
}: {
  toast: ToastItemData;
  onRemove: (id: string) => void;
  onOpenDetails: (details: ErrorDetails) => void;
}) {
  const [
    closing,
    setClosing,
  ] = useState(false);

  function closeToast() {
    setClosing(true);
  }

  useEffect(() => {
    const timeoutId = window.setTimeout(
      () => {
        setClosing(true);
      },
      toast.duration
    );

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [
    toast.duration,
  ]);

  const Icon = toastIcons[toast.variant];
  const isDetailsClickable =
    Boolean(toast.errorDetails);

  function openDetails() {
    if (toast.errorDetails) {
      onOpenDetails(toast.errorDetails);
    }
  }

  function handleKeyDown(
    event: KeyboardEvent<HTMLDivElement>
  ) {
    if (
      !isDetailsClickable ||
      (event.key !== "Enter" &&
        event.key !== " ")
    ) {
      return;
    }

    event.preventDefault();
    openDetails();
  }

  return (
    <div
      data-state={
        closing ? "closing" : "open"
      }
      data-variant={toast.variant}
      role={
        toast.variant === "error"
          ? "alert"
          : "status"
      }
      className="app-toast pointer-events-auto rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card-strong)] shadow-[var(--shadow-dropdown)]"
      tabIndex={
        isDetailsClickable ? 0 : undefined
      }
      aria-label={
        isDetailsClickable
          ? `${toast.title}. Abrir detalhes do erro`
          : undefined
      }
      onClick={openDetails}
      onKeyDown={handleKeyDown}
      onAnimationEnd={(event) => {
        if (
          event.target !==
            event.currentTarget ||
          !closing
        ) {
          return;
        }

        onRemove(toast.id);
      }}
    >
      <div className="flex gap-3 p-3">
        <div className="toast-icon mt-0.5 grid h-7 w-7 shrink-0 place-items-center rounded-[4px] border">
          <Icon
            className="h-4 w-4"
            aria-hidden="true"
          />
        </div>

        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-[var(--text-base)]">
            {toast.title}
          </p>

          {toast.description ? (
            <p className="mt-1 text-xs leading-5 text-[var(--text-muted)]">
              {toast.description}
            </p>
          ) : null}
        </div>

        {isDetailsClickable ? (
          <ChevronRight
            className="mt-1 h-4 w-4 shrink-0 text-[var(--text-muted)]"
            aria-hidden="true"
          />
        ) : null}

        <Button
          size="icon"
          variant="ghost"
          onClick={(event) => {
            event.stopPropagation();
            closeToast();
          }}
          leadingIcon={<X />}
          aria-label="Fechar mensagem"
          title="Fechar"
        />
      </div>
    </div>
  );
}

function ErrorDetailsModal({
  details,
  onClose,
}: {
  details: ErrorDetails | null;
  onClose: () => void;
}) {
  return (
    <AnimatedModal
      open={Boolean(details)}
      onClose={onClose}
      labelledBy="shared-error-details-title"
      describedBy="shared-error-details-description"
      backdropClassName="z-[340] p-4"
      panelClassName="w-full max-w-md overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      {details ? (
        <>
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <p
                id="shared-error-details-title"
                className="text-[var(--font-size-page-title)] font-semibold text-[var(--text-base)]"
              >
                {details.title}
              </p>

              <p
                id="shared-error-details-description"
                className="mt-3 text-sm leading-6 text-[var(--text-base)]"
              >
                {details.explanation}
              </p>

              {details.guidance ? (
                <p className="mt-3 text-sm leading-6 text-[var(--text-muted)]">
                  {details.guidance}
                </p>
              ) : null}
            </div>

            <Button
              size="icon"
              variant="ghost"
              onClick={onClose}
              leadingIcon={<X />}
              aria-label="Fechar modal"
              title="Fechar"
            />
          </div>

          <div className="mt-5 flex justify-end">
            <Button
              variant="primary"
              onClick={onClose}
            >
              Fechar
            </Button>
          </div>
        </>
      ) : null}
    </AnimatedModal>
  );
}
