package br.com.vitrine7.printeragent;

import org.junit.jupiter.api.Test;

import java.awt.print.Book;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalPrinterTest {
    private static final double POINTS_PER_MM = 72d / 25.4d;

    @Test
    void keepsShortReceiptsOnOneDynamicallySizedPage() {
        ThermalPrinter printer = new ThermalPrinter(config());
        List<String> lines = List.of("VITRINE 7", "TOTAL: R$ 10,00", "FIM");

        Book book = printer.createBook(lines);

        assertEquals(1, book.getNumberOfPages());
        assertTrue(book.getPageFormat(0).getHeight() < 250d * POINTS_PER_MM);
    }

    @Test
    void paginatesLongReceiptsWithoutDroppingOrReorderingLines() {
        ThermalPrinter printer = new ThermalPrinter(config());
        List<String> lines = IntStream.range(0, 250)
                .mapToObj(index -> "LINHA " + index)
                .toList();

        List<List<String>> pages = printer.paginate(lines);
        List<String> flattened = pages.stream()
                .flatMap(List::stream)
                .toList();

        assertTrue(pages.size() > 1);
        assertTrue(pages.stream().allMatch(page -> page.size() <= printer.maxLinesPerPage()));
        assertEquals(lines, flattened);
    }

    @Test
    void capsEveryLongReceiptPageAtSafeHeight() {
        ThermalPrinter printer = new ThermalPrinter(config());
        List<String> lines = IntStream.range(0, 250)
                .mapToObj(index -> "LINHA " + index)
                .toList();

        Book book = printer.createBook(lines);

        assertTrue(book.getNumberOfPages() > 1);
        for (int pageIndex = 0; pageIndex < book.getNumberOfPages(); pageIndex++) {
            assertTrue(book.getPageFormat(pageIndex).getHeight() <= 250d * POINTS_PER_MM);
        }
    }

    private AgentConfig config() {
        return new AgentConfig(
                "https://example.test",
                "test-token",
                "Thermal 80",
                20,
                80d,
                2d,
                7f,
                9f
        );
    }
}
