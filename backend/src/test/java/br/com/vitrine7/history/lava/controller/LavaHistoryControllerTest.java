package br.com.vitrine7.history.lava.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LavaHistoryControllerTest {

    @Test
    void paginatedHistoryEndpointRequiresLavaAccess() throws Exception {
        Method listMethod = LavaHistoryController.class
                .getDeclaredMethod(
                        "list",
                        int.class,
                        int.class,
                        String.class,
                        br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus.class,
                        java.time.OffsetDateTime.class,
                        java.time.OffsetDateTime.class,
                        br.com.vitrine7.history.bar.dto.HistoryPaymentMethod.class
                );

        PreAuthorize preAuthorize =
                listMethod.getAnnotation(PreAuthorize.class);

        assertEquals(
                "hasAuthority('lava:access')",
                preAuthorize.value()
        );
    }
}
