package com.cineman;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

/** Bất biến kiến trúc (AGENTS.md §2, ARCHITECTURE.md §3, §5): 13 bảng, 14 khóa ngoại, 1 RESTController. */
@SpringBootTest
class ArchitectureInvariantTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationContext context;

    @Test
    void coDung13BangVa14KhoaNgoai() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name LIKE 'tbl%'""",
                Integer.class)).isEqualTo(13);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND constraint_type = 'FOREIGN KEY'""",
                Integer.class)).isEqualTo(14);
    }

    @Test
    void chiCoMotRESTController() {
        assertThat(context.getBeansWithAnnotation(RestController.class).values())
                .extracting(bean -> ClassUtils.getUserClass(bean).getSimpleName())
                .containsExactly("RESTController");
    }
}
