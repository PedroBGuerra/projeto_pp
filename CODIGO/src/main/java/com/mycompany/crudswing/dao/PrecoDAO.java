package com.mycompany.crudswing.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.crudswing.DatabaseConnection;
import com.mycompany.crudswing.Preco;

public class PrecoDAO {
    private Connection connection;

    public PrecoDAO() throws SQLException {
        connection = DatabaseConnection.getConnection();
        ensurePrecoTableExists();
    }

    private void ensurePrecoTableExists() {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            try (ResultSet rs = meta.getTables(catalog, null, "precos", null)) {
                if (!rs.next()) {
                    // table doesn't exist, create and insert defaults
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("CREATE TABLE IF NOT EXISTS precos (id INT AUTO_INCREMENT PRIMARY KEY, max_hours INT NULL, base_price DOUBLE NOT NULL, extra_per_hour DOUBLE NULL)");
                        st.executeUpdate("INSERT INTO precos (max_hours, base_price, extra_per_hour) VALUES (1, 15, NULL)");
                        st.executeUpdate("INSERT INTO precos (max_hours, base_price, extra_per_hour) VALUES (2, 25, NULL)");
                        st.executeUpdate("INSERT INTO precos (max_hours, base_price, extra_per_hour) VALUES (NULL, 30, 5)");
                    }
                }
            }
        } catch (SQLException ex) {
            // If something fails, print stack and continue; getAllPrecos will return empty list.
            System.err.println("Warning: could not ensure precos table exists: " + ex.getMessage());
        }
    }

    // Load all pricing rules ordered by maxHours asc (nulls last)
    public List<Preco> getAllPrecos() {
        List<Preco> list = new ArrayList<>();
        String sql = "SELECT id, max_hours, base_price, extra_per_hour FROM precos ORDER BY CASE WHEN max_hours IS NULL THEN 1 ELSE 0 END, max_hours";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Integer max = rs.getObject("max_hours") == null ? null : rs.getInt("max_hours");
                Double extra = rs.getObject("extra_per_hour") == null ? null : rs.getDouble("extra_per_hour");
                Preco p = new Preco(rs.getInt("id"), max, rs.getDouble("base_price"), extra);
                list.add(p);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    // Calculate price for a duration in minutes using rules
    public double calculatePrice(long minutes) {
        List<Preco> rules = getAllPrecos();
        if (rules.isEmpty()) return -1.0; // signal that no rules are defined
        double hours = minutes / 60.0;
        int prevMax = 0;
        for (Preco r : rules) {
            Integer max = r.getMaxHours();
            if (max != null) {
                if (hours <= max) return r.getBasePrice();
                prevMax = max;
            } else {
                // catch-all rule
                double extra = r.getExtraPerHour() == null ? 0.0 : r.getExtraPerHour();
                double extraHours = Math.ceil(Math.max(0, hours - prevMax));
                return r.getBasePrice() + extra * extraHours;
            }
        }
        // default fallback: last rule
        Preco last = rules.get(rules.size() - 1);
        if (last.getMaxHours() == null) {
            double extra = last.getExtraPerHour() == null ? 0.0 : last.getExtraPerHour();
            double extraHours = Math.ceil(Math.max(0, hours - prevMax));
            return last.getBasePrice() + extra * extraHours;
        }
        return 0.0;
    }
}
