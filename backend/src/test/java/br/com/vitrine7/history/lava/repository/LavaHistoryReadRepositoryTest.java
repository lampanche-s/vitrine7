package br.com.vitrine7.history.lava.repository;

import br.com.vitrine7.history.lava.dto.LavaHistoryFilters;
import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LavaHistoryReadRepositoryTest {

    @Test
    void historyQueryAcceptsCompletedWorkOrdersWithSettledPayment()
            throws Exception {
        String sql = readBaseSql();

        assertTrue(sql.contains("WHERE wo.status = 'COMPLETED'"));
        assertTrue(
                sql.contains(
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
        assertTrue(sql.contains("AND p.id IS NOT NULL"));
        assertFalse(sql.contains("wo.status = 'PAID'"));
        assertFalse(sql.contains("CONCLUDED"));
        assertTrue(
                LavaWorkOrderStatus.valueOf("COMPLETED")
                        == LavaWorkOrderStatus.COMPLETED
        );
    }

    @Test
    void historyPageIsOrderedFromNewestToOldest() {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);
        LavaHistoryReadRepository repository =
                new LavaHistoryReadRepository(jdbcTemplate);

        when(jdbcTemplate.query(
                anyString(),
                any(MapSqlParameterSource.class),
                any(RowMapper.class)
        )).thenReturn(List.of());

        repository.findPage(new LavaHistoryFilters(
                0,
                20,
                null,
                null,
                LavaWorkOrderStatus.COMPLETED,
                null,
                null,
                null
        ));

        ArgumentCaptor<String> sqlCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(MapSqlParameterSource.class),
                any(RowMapper.class)
        );

        assertTrue(
                sqlCaptor.getValue()
                        .contains("ORDER BY event_at DESC, work_order_id DESC")
        );
    }

    private String readBaseSql() throws Exception {
        Field field = LavaHistoryReadRepository.class
                .getDeclaredField("BASE_SQL");
        field.setAccessible(true);

        return (String) field.get(null);
    }
}
