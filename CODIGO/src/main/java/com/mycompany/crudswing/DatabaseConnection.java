package com.mycompany.crudswing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:h2:./crud_db;DB_CLOSE_DELAY=-1";
    private static final String USER = "";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        initializeDatabase(conn);
        return conn;
    }

    private static void initializeDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create estacionamento table if not exists
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS estacionamento (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "marca VARCHAR(255), " +
                "modelo VARCHAR(255), " +
                "placa VARCHAR(20), " +
                "entrada VARCHAR(20), " +
                "saida VARCHAR(20))");
        }
    }
}
