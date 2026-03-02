package tn.esprit.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs one-time schema migrations at application startup.
 * Called from Main / App before the JavaFX stage is shown.
 */
public class DbMigration {

    public static void run() {
        Connection conn = MyDB.getInstance().getConx();
        if (conn == null) {
            System.out.println("[Migration] Skipped – no DB connection.");
            return;
        }

        runStatement(conn,
            "ALTER TABLE `contract_request` " +
            "ADD COLUMN IF NOT EXISTS `boldsign_document_id` VARCHAR(255) DEFAULT NULL",
            "contract_request.boldsign_document_id");
    }

    private static void runStatement(Connection conn, String sql, String description) {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("[Migration] OK  : " + description);
        } catch (SQLException e) {
            // Column may already exist on MySQL < 10.x that doesn't support IF NOT EXISTS
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate column")) {
                System.out.println("[Migration] SKIP: " + description + " (already exists)");
            } else {
                System.out.println("[Migration] WARN: " + description + " – " + e.getMessage());
            }
        }
    }
}

