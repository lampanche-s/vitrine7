package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.service.BarTabService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.print.PrintDocumentKind;
import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperationalOrderPrintServiceTest {

    private final BarTabService barTabService = mock(BarTabService.class);
    private final OperationalOrderRenderer renderer = mock(OperationalOrderRenderer.class);
    private final PrintJobService printJobService = mock(PrintJobService.class);
    private final VitrineUserPrincipal actor = mock(VitrineUserPrincipal.class);
    private final OperationalOrderPrintService service =
            new OperationalOrderPrintService(barTabService, renderer, printJobService);

    @Test
    void printsOnlyItemsFromOpenTabWithoutMutatingIt() {
        BarTabLineResponse item = line(1L, "ITEM", 2);
        BarTabLineResponse serviceLine = line(2L, "SERVICE", 1);
        BarTabResponse tab = tab("OPEN", List.of(item, serviceLine));
        List<BarTabLineResponse> originalLines = tab.lines();
        when(barTabService.findById(44L)).thenReturn(tab);
        when(renderer.renderItems(tab, List.of(item))).thenReturn("ITENS\n");

        service.printAllItems(44L, actor);

        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.ITEM_ORDER, "ITENS\n", actor
        );
        verify(renderer, never()).renderServices(any(), any());
        assertSame(originalLines, tab.lines());
        assertEquals(List.of(item, serviceLine), tab.lines());
    }

    @Test
    void printsOnlyServicesFromOpenTab() {
        BarTabLineResponse item = line(1L, "ITEM", 2);
        BarTabLineResponse serviceLine = line(2L, "SERVICE", 1);
        BarTabResponse tab = tab("OPEN", List.of(item, serviceLine));
        when(barTabService.findById(44L)).thenReturn(tab);
        when(renderer.renderServices(tab, List.of(serviceLine))).thenReturn("SERVIÇOS\n");

        service.printAllServices(44L, actor);

        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.SERVICE_ORDER, "SERVIÇOS\n", actor
        );
        verify(renderer, never()).renderItems(any(), any());
    }

    @Test
    void printsSpecificItemAndSpecificServiceWithCurrentQuantity() {
        BarTabLineResponse item = line(1L, "ITEM", 4);
        BarTabLineResponse serviceLine = line(2L, "SERVICE", 3);
        BarTabResponse tab = tab("OPEN", List.of(item, serviceLine));
        when(barTabService.findById(44L)).thenReturn(tab);
        when(renderer.renderSingleItem(tab, item)).thenReturn("4x ITEM\n");
        when(renderer.renderSingleService(tab, serviceLine)).thenReturn("3x SERVIÇO\n");

        service.printLine(44L, 1L, actor);
        service.printLine(44L, 2L, actor);

        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.ITEM_ORDER, "4x ITEM\n", actor
        );
        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.SERVICE_ORDER, "3x SERVIÇO\n", actor
        );
    }

    @Test
    void rejectsLineThatDoesNotBelongToTab() {
        when(barTabService.findById(44L))
                .thenReturn(tab("OPEN", List.of(line(1L, "ITEM", 1))));

        assertThrows(NotFoundException.class, () -> service.printLine(44L, 99L, actor));
        verifyNoInteractions(renderer, printJobService);
    }

    @Test
    void rejectsMissingCategoryWithFunctionalMessages() {
        when(barTabService.findById(44L)).thenReturn(tab("OPEN", List.of()));

        BusinessException items = assertThrows(
                BusinessException.class,
                () -> service.printAllItems(44L, actor)
        );
        BusinessException services = assertThrows(
                BusinessException.class,
                () -> service.printAllServices(44L, actor)
        );

        assertEquals("A comanda não possui itens para imprimir.", items.getMessage());
        assertEquals("A comanda não possui serviços para imprimir.", services.getMessage());
        verifyNoInteractions(renderer, printJobService);
    }

    @Test
    void rejectsEveryStatusOutsideOpen() {
        for (String status : List.of("PAYMENT_PENDING", "CLOSED", "CANCELLED")) {
            when(barTabService.findById(44L))
                    .thenReturn(tab(status, List.of(line(1L, "ITEM", 1))));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> service.printAllItems(44L, actor)
            );
            assertEquals(
                    "Não é possível imprimir pedidos de uma comanda que não está aberta.",
                    exception.getMessage()
            );
        }
        verifyNoInteractions(renderer, printJobService);
    }

    @Test
    void twoClicksAlwaysRequestTwoNewJobs() {
        BarTabLineResponse item = line(1L, "ITEM", 2);
        BarTabResponse tab = tab("OPEN", List.of(item));
        when(barTabService.findById(44L)).thenReturn(tab);
        when(renderer.renderItems(tab, List.of(item))).thenReturn("ITENS\n");
        when(printJobService.createOperationalOrder(any(), any(), any(), any()))
                .thenReturn(new PrintJobDtos.Created(UUID.randomUUID(), "PENDING"));

        service.printAllItems(44L, actor);
        service.printAllItems(44L, actor);

        verify(printJobService, times(2)).createOperationalOrder(
                eq(44L), eq(PrintDocumentKind.ITEM_ORDER), eq("ITENS\n"), eq(actor)
        );
    }

    @Test
    void keepsFourActionsSeparateForTabWithTwoItemsAndTwoServices() {
        BarTabLineResponse itemOne = line(1L, "ITEM", 2);
        BarTabLineResponse serviceOne = line(2L, "SERVICE", 1);
        BarTabLineResponse itemTwo = line(3L, "ITEM", 4);
        BarTabLineResponse serviceTwo = line(4L, "SERVICE", 3);
        BarTabResponse tab = tab(
                "OPEN",
                List.of(itemOne, serviceOne, itemTwo, serviceTwo)
        );
        when(barTabService.findById(44L)).thenReturn(tab);
        when(renderer.renderItems(tab, List.of(itemOne, itemTwo)))
                .thenReturn("TODOS OS ITENS\n");
        when(renderer.renderServices(tab, List.of(serviceOne, serviceTwo)))
                .thenReturn("TODOS OS SERVIÇOS\n");
        when(renderer.renderSingleItem(tab, itemTwo)).thenReturn("ITEM 3\n");
        when(renderer.renderSingleService(tab, serviceTwo)).thenReturn("SERVIÇO 4\n");

        service.printAllItems(44L, actor);
        service.printAllServices(44L, actor);
        service.printLine(44L, 3L, actor);
        service.printLine(44L, 4L, actor);

        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.ITEM_ORDER, "TODOS OS ITENS\n", actor
        );
        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.SERVICE_ORDER, "TODOS OS SERVIÇOS\n", actor
        );
        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.ITEM_ORDER, "ITEM 3\n", actor
        );
        verify(printJobService).createOperationalOrder(
                44L, PrintDocumentKind.SERVICE_ORDER, "SERVIÇO 4\n", actor
        );
    }

    private BarTabLineResponse line(Long id, String type, int quantity) {
        return new BarTabLineResponse(
                id, id + 10, type, type + " " + id, 500L,
                quantity, 500L * quantity
        );
    }

    private BarTabResponse tab(String status, List<BarTabLineResponse> lines) {
        return new BarTabResponse(
                44L, "Mesa 4", null, null, status, null, null,
                1500L, 0L, 1500L, null, null, false,
                null, null, null, false, lines, 9L, null, null
        );
    }
}
