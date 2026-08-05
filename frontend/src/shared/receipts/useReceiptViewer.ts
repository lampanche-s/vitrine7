import {
  useCallback,
  useState,
} from "react";

import {
  createReceiptPrintHtml,
  createReceiptPreviewImage,
  type ReceiptDocument,
} from "./receiptDocument";

export function useReceiptViewer() {
  const [printHtml, setPrintHtml] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  const openReceipt = useCallback((receipt: ReceiptDocument) => {
    setPrintHtml(createReceiptPrintHtml(receipt));
    setPreviewUrl(createReceiptPreviewImage(receipt));
    setIsOpen(true);
  }, []);

  const closeReceipt = useCallback(() => {
    setIsOpen(false);

    window.setTimeout(() => {
      setPreviewUrl(null);
      setPrintHtml(null);
    }, 160);
  }, []);

  return {
    isOpen,
    printHtml,
    previewUrl,
    openReceipt,
    closeReceipt,
  };
}
