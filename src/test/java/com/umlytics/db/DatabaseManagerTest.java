package com.umlytics.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest {
    @Test
    void connectionAndSimpleQueryWork() throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        db.initialize();

        Connection connection = db.getConnection();
        assertNotNull(connection);

        db.executeUpdate("CREATE TABLE IF NOT EXISTS test_table(id INTEGER)");
        int inserted = db.executeUpdate("INSERT INTO test_table(id) VALUES (1)");
        assertTrue(inserted >= 1);

        ResultSet resultSet = db.executeQuery("SELECT COUNT(*) AS c FROM test_table");
        assertTrue(resultSet.next());
        assertTrue(resultSet.getInt("c") >= 1);
    }
}
