package com.umlytics.db;

import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Deque;

public class ConnectionPool {
    private final Deque<Connection> idleConnections = new ArrayDeque<>();

    public synchronized Connection borrow() {
        return idleConnections.pollFirst();
    }

    public synchronized void release(Connection connection) {
        if (connection != null) {
            idleConnections.offerLast(connection);
        }
    }
}
