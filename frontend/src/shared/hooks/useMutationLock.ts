import {
  useCallback,
  useRef,
  useState,
} from "react";

export function useMutationLock() {
  const lockRef = useRef(false);

  const [isMutating, setIsMutating] =
    useState(false);

  const runMutation = useCallback(
    async <T,>(
      operation: () => Promise<T>,
      ignoredResult: T
    ): Promise<T> => {
      if (lockRef.current) {
        return ignoredResult;
      }

      lockRef.current = true;
      setIsMutating(true);

      try {
        return await operation();
      } finally {
        lockRef.current = false;
        setIsMutating(false);
      }
    },
    []
  );

  return {
    isMutating,
    runMutation,
  };
}
