package br.com.vitrine7.printeragent;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Arrays;
import java.util.List;

public final class ThermalPrinter {
    private static final double POINTS_PER_MM = 72d / 25.4d;

    private final AgentConfig config;

    public ThermalPrinter(AgentConfig config) {
        this.config = config;
    }

    public void print(String receiptText) throws PrinterException {
        PrintService printService = findPrintService(config.printerName());
        List<String> lines = receiptText.lines().toList();

        if (lines.isEmpty()) {
            throw new PrinterException("Comprovante vazio.");
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(printService);
        job.setJobName("Vitrine 7 - Comprovante");
        job.setCopies(1);

        PageFormat pageFormat = createPageFormat(lines.size());
        job.setPrintable(new ReceiptPrintable(lines, config), pageFormat);
        job.print();
    }

    public static List<String> availablePrinterNames() {
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .map(PrintService::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private PrintService findPrintService(String expectedName) throws PrinterException {
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(service -> service.getName().equalsIgnoreCase(expectedName))
                .findFirst()
                .orElseThrow(() -> new PrinterException(
                        "Impressora '" + expectedName + "' nao encontrada. Disponiveis: "
                                + String.join(", ", availablePrinterNames())
                ));
    }

    private PageFormat createPageFormat(int lineCount) {
        double width = config.paperWidthMm() * POINTS_PER_MM;
        double margin = config.marginMm() * POINTS_PER_MM;
        double minimumHeight = 60d * POINTS_PER_MM;
        double contentHeight = lineCount * config.lineHeightPt() + margin * 2d + 12d;
        double height = Math.max(minimumHeight, contentHeight);

        Paper paper = new Paper();
        paper.setSize(width, height);
        paper.setImageableArea(
                margin,
                margin,
                Math.max(1d, width - margin * 2d),
                Math.max(1d, height - margin * 2d)
        );

        PageFormat format = new PageFormat();
        format.setOrientation(PageFormat.PORTRAIT);
        format.setPaper(paper);
        return format;
    }

    private static final class ReceiptPrintable implements Printable {
        private final List<String> lines;
        private final AgentConfig config;

        private ReceiptPrintable(List<String> lines, AgentConfig config) {
            this.lines = lines;
            this.config = config;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D graphics2D = (Graphics2D) graphics;
            graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            Font normal = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(config.fontSizePt()));
            Font bold = new Font(Font.MONOSPACED, Font.BOLD, Math.round(config.fontSizePt()));

            float y = config.lineHeightPt();
            for (String line : lines) {
                graphics2D.setFont(isBold(line) ? bold : normal);
                graphics2D.drawString(line, 0f, y);
                y += config.lineHeightPt();
            }

            return PAGE_EXISTS;
        }

        private boolean isBold(String line) {
            String trimmed = line.trim();
            return trimmed.startsWith("TOTAL:")
                    || (!trimmed.isEmpty() && trimmed.chars().allMatch(ch -> ch == '='));
        }
    }
}
