import {
  useCallback,
  useState,
} from "react";

import {
  createReceiptPreviewImage,
  type ReceiptDocument,
} from "./receiptDocument";

export function useReceiptViewer() {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  const openReceipt = useCallback((receipt: ReceiptDocument) => {
    setPreviewUrl(createReceiptPreviewImage(receipt));
    setIsOpen(true);
  }, []);

  const closeReceipt = useCallback(() => {
    setIsOpen(false);

    window.setTimeout(() => {
      setPreviewUrl(null);
    }, 160);
  }, []);

  return {
    isOpen,
    previewUrl,
    openReceipt,
    closeReceipt,
  };
}
