import {
  type AnimationEvent,
  type MouseEvent,
  type ReactNode,
  useEffect,
  useRef,
  useState,
} from "react";

import {
  createPortal,
} from "react-dom";

import {
  useModalBehavior,
} from "../../shared/hooks/useModalBehavior";

type AnimatedModalProps = {
  open: boolean;
  onClose: () => void;

  labelledBy: string;
  describedBy?: string;

  children: ReactNode;

  backdropClassName?: string;
  panelClassName?: string;

  closeOnBackdrop?: boolean;
  closeOnEscape?: boolean;
};

export function AnimatedModal({
  open,
  onClose,

  labelledBy,
  describedBy,

  children,

  backdropClassName = "",
  panelClassName = "",

  closeOnBackdrop = true,
  closeOnEscape = true,
}: AnimatedModalProps) {
  const [
    isMounted,
    setIsMounted,
  ] = useState(open);

  const [
    isClosing,
    setIsClosing,
  ] = useState(false);

  const closeRequestedRef =
    useRef(false);

  const [
    renderedChildren,
    setRenderedChildren,
  ] = useState(children);

  const panelRef =
    useRef<HTMLDivElement>(null);

  const previousFocusRef =
    useRef<HTMLElement | null>(null);

  useEffect(() => {
    let cancelled = false;

    if (open) {
      queueMicrotask(() => {
        if (cancelled) {
          return;
        }

        setRenderedChildren(children);
        setIsMounted(true);
        setIsClosing(false);
        closeRequestedRef.current = false;
      });

      return () => {
        cancelled = true;
      };
    }

    if (isMounted) {
      queueMicrotask(() => {
        if (cancelled) {
          return;
        }

        setIsClosing(true);
      });
    }

    return () => {
      cancelled = true;
    };
  }, [
    open,
    isMounted,
    children,
  ]);

  useEffect(() => {
    if (
      !isMounted ||
      isClosing
    ) {
      return;
    }

    previousFocusRef.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    const animationFrame =
      window.requestAnimationFrame(() => {
        const target =
          panelRef.current?.querySelector<HTMLElement>("[autofocus]") ??
          panelRef.current;

        target?.focus({
          preventScroll: true,
        });
      });

    return () => {
      window.cancelAnimationFrame(
        animationFrame
      );
    };
  }, [
    isMounted,
    isClosing,
  ]);

  function requestClose() {
    if (
      !isMounted ||
      isClosing
    ) {
      return;
    }

    closeRequestedRef.current = true;
    setIsClosing(true);
  }

  useModalBehavior(
    isMounted,
    requestClose,
    closeOnEscape
  );

  function finishClosing() {
    const shouldNotifyParent =
      closeRequestedRef.current;

    closeRequestedRef.current = false;

    setIsMounted(false);
    setIsClosing(false);

    previousFocusRef.current?.focus({
      preventScroll: true,
    });

    if (shouldNotifyParent) {
      onClose();
    }
  }

  function handlePanelAnimationEnd(
    event: AnimationEvent<HTMLDivElement>
  ) {
    if (
      event.target !==
        event.currentTarget ||
      !isClosing
    ) {
      return;
    }

    finishClosing();
  }

  function handleBackdropClick(
    event: MouseEvent<HTMLDivElement>
  ) {
    if (
      !closeOnBackdrop ||
      event.target !==
        event.currentTarget
    ) {
      return;
    }

    requestClose();
  }

  if (
    !isMounted ||
    typeof document === "undefined"
  ) {
    return null;
  }

  const animationState =
    isClosing
      ? "closing"
      : "open";

  return createPortal(
    <div
      data-state={animationState}
      className={[
        "site-modal-backdrop fixed inset-0 z-[200] flex items-center justify-center bg-[rgba(6,8,12,0.78)] p-4",
        backdropClassName,
      ].join(" ")}
      onMouseDown={
        handleBackdropClick
      }
    >
      <div
        ref={panelRef}
        data-state={animationState}
        role="dialog"
        aria-modal="true"
        aria-labelledby={
          labelledBy
        }
        aria-describedby={
          describedBy
        }
        tabIndex={-1}
        className={[
          "site-modal-panel outline-none",
          panelClassName,
        ].join(" ")}
        onAnimationEnd={
          handlePanelAnimationEnd
        }
        onMouseDown={(event) => {
          event.stopPropagation();
        }}
      >
        {renderedChildren}
      </div>
    </div>,
    document.body
  );
}
