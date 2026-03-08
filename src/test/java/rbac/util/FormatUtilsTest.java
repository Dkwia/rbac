package test.java.rbac.util;

import org.junit.jupiter.api.Test;
import rbac.util.FormatUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void formatsTableWithHeaders() {
        String[] headers = {"A", "B"};
        List<String[]> rows = java.util.Collections.singletonList(new String[]{"1", "2"});
        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("A"));
        assertTrue(table.contains("B"));
        assertTrue(table.contains("| 1 | 2 |"));
    }

    @Test
    void formatsBox() {
        String box = FormatUtils.formatBox("Hello");
        assertTrue(box.contains("Hello"));
        assertTrue(box.startsWith("+"));
    }
}
