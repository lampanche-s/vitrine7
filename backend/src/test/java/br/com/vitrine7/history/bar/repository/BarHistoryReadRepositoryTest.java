package br.com.vitrine7.history.bar.repository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarHistoryReadRepositoryTest {

    @Test
    void historyKeepsReversedPaymentsVisible()
            throws Exception {
        Field field =
                BarHistoryReadRepository.class
                        .getDeclaredField(
                                "BASE_SQL"
                        );

        field.setAccessible(true);

        String sql =
                (String) field.get(null);

        assertEquals(
                1,
                occurrences(
                        sql,
                        "pay.status IN"
                )
        );

        assertTrue(
                sql.contains(
                        "'REVERSAL_PENDING'"
                )
        );

        assertTrue(
                sql.contains(
                        "'REVERSED'"
                )
        );

        assertTrue(
                sql.contains(
                        "payment_reversed_at"
                )
        );

        assertTrue(
                sql.contains(
                        "payment_reversal_reason"
                )
        );

    }

    private int occurrences(
            String value,
            String search
    ) {
        int count = 0;
        int index = 0;

        while (
                (
                        index =
                                value.indexOf(
                                        search,
                                        index
                                )
                ) >= 0
        ) {
            count++;
            index += search.length();
        }

        return count;
    }
}
