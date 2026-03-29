package test.java.rbac;

import org.junit.jupiter.api.Test;
import rbac.AuditLog;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test
    void logStoresEntriesAndFilters() {
        AuditLog log = new AuditLog();
        log.log("USER_CREATE", "admin", "john", "created");
        log.log("ROLE_ASSIGN", "admin", "john", "role=Manager");

        assertEquals(2, log.getAll().size());
        assertEquals(2, log.getByPerformer("admin").size());
        assertEquals(1, log.getByAction("user_create").size());
    }
}
