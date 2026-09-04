package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.service.BarTabService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.print.PrintDocumentKind;
import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationalOrderPrintService {

    private final BarTabService barTabService;
    private final OperationalOrderRenderer renderer;
    private final PrintJobService printJobService;

    @Transactional
    public PrintJobDtos.Created printAllItems(
            Long tabId,
            VitrineUserPrincipal actor
    ) {
        BarTabResponse tab = getOpenTab(tabId);
        List<BarTabLineResponse> lines = linesOfType(tab, "ITEM");

        if (lines.isEmpty()) {
            throw new BusinessException(
                    "BAR_TAB_HAS_NO_ITEMS_TO_PRINT",
                    "A comanda não possui itens para imprimir."
            );
        }

        return printJobService.createOperationalOrder(
                tabId,
                PrintDocumentKind.ITEM_ORDER,
                renderer.renderItems(tab, lines),
                actor
        );
    }

    @Transactional
    public PrintJobDtos.Created printAllServices(
            Long tabId,
            VitrineUserPrincipal actor
    ) {
        BarTabResponse tab = getOpenTab(tabId);
        List<BarTabLineResponse> lines = linesOfType(tab, "SERVICE");

        if (lines.isEmpty()) {
            throw new BusinessException(
                    "BAR_TAB_HAS_NO_SERVICES_TO_PRINT",
                    "A comanda não possui serviços para imprimir."
            );
        }

        return printJobService.createOperationalOrder(
                tabId,
                PrintDocumentKind.SERVICE_ORDER,
                renderer.renderServices(tab, lines),
                actor
        );
    }

    @Transactional
    public PrintJobDtos.Created printLine(
            Long tabId,
            Long lineId,
            VitrineUserPrincipal actor
    ) {
        BarTabResponse tab = getOpenTab(tabId);
        BarTabLineResponse line = tab.lines().stream()
                .filter(candidate -> candidate.id().equals(lineId))
                .filter(candidate -> candidate.quantity() != null && candidate.quantity() > 0)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "BAR_TAB_LINE_NOT_FOUND",
                        "Item ou serviço não encontrado nesta comanda."
                ));

        boolean item = "ITEM".equals(line.entryType());
        boolean service = "SERVICE".equals(line.entryType());
        if (!item && !service) {
            throw new BusinessException(
                    "BAR_TAB_LINE_TYPE_NOT_PRINTABLE",
                    "O tipo da linha não pode ser impresso."
            );
        }

        return printJobService.createOperationalOrder(
                tabId,
                item
                        ? PrintDocumentKind.ITEM_ORDER
                        : PrintDocumentKind.SERVICE_ORDER,
                item
                        ? renderer.renderSingleItem(tab, line)
                        : renderer.renderSingleService(tab, line),
                actor
        );
    }

    private BarTabResponse getOpenTab(Long tabId) {
        BarTabResponse tab = barTabService.findById(tabId);
        if (!"OPEN".equals(tab.status())) {
            throw new BusinessException(
                    "BAR_TAB_NOT_OPEN_FOR_OPERATIONAL_PRINT",
                    "Não é possível imprimir pedidos de uma comanda que não está aberta."
            );
        }
        return tab;
    }

    private List<BarTabLineResponse> linesOfType(
            BarTabResponse tab,
            String type
    ) {
        return tab.lines().stream()
                .filter(line -> type.equals(line.entryType()))
                .filter(line -> line.quantity() != null && line.quantity() > 0)
                .toList();
    }
}
