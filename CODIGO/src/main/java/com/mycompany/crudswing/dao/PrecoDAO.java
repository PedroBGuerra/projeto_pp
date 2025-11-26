package com.mycompany.crudswing.dao;

import java.sql.Connection;
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
