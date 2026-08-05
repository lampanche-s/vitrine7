import {
  useEffect,
  useRef,
} from "react";

const activeModalStack: symbol[] = [];

function registerModal(
  modalId: symbol
): void {
  const existingIndex =
    activeModalStack.indexOf(modalId);

  if (existingIndex >= 0) {
    activeModalStack.splice(
      existingIndex,
      1
    );
  }

  activeModalStack.push(modalId);
}

function unregisterModal(
  modalId: symbol
): void {
  const index =
    activeModalStack.indexOf(modalId);

  if (index >= 0) {
    activeModalStack.splice(index, 1);
  }
}

function isTopModal(
  modalId: symbol
): boolean {
  return (
    activeModalStack[
      activeModalStack.length - 1
    ] === modalId
  );
}

export function useModalBehavior(
  active: boolean,
  onClose: () => void,
  closeOnEscape = true
): void {
  const onCloseRef =
    useRef(onClose);

  const modalIdRef =
    useRef(Symbol("modal-layer"));

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    if (!active) {
      return;
    }

    const modalId =
      modalIdRef.current;

    const previousOverflow =
      document.body.style.overflow;

    registerModal(modalId);

    document.body.style.overflow =
      "hidden";

    function handleKeyDown(
      event: KeyboardEvent
    ): void {
      if (
        event.key !== "Escape" ||
        !isTopModal(modalId)
      ) {
        return;
      }

      event.preventDefault();

      if (!closeOnEscape) {
        return;
      }

      onCloseRef.current();
    }

    document.addEventListener(
      "keydown",
      handleKeyDown
    );

    return () => {
      unregisterModal(modalId);

      document.body.style.overflow =
        previousOverflow;

      document.removeEventListener(
        "keydown",
        handleKeyDown
      );
    };
  }, [active, closeOnEscape]);
}
