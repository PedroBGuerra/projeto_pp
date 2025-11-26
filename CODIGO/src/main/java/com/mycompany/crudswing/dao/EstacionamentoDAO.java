package com.mycompany.crudswing.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.crudswing.DatabaseConnection;
import com.mycompany.crudswing.Estacionamento;

public class EstacionamentoDAO {
    private Connection connection;
    // Flags to know whether each column is DATETIME or VARCHAR
    private boolean entradaIsDatetime = false;
    private boolean saidaIsDatetime = false;
    private int entradaMaxLen = -1;
    private int saidaMaxLen = -1;

    public EstacionamentoDAO() {
        try {
            connection = DatabaseConnection.getConnection();
            detectColumnTypes();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void detectColumnTypes() {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "estacionamento", "entrada")) {
                if (rs.next()) {
                    int dataType = rs.getInt("DATA_TYPE");
                    entradaIsDatetime = (dataType == Types.TIMESTAMP || dataType == Types.DATE || dataType == Types.TIME);
                    entradaMaxLen = rs.getInt("COLUMN_SIZE");
                }
            }
            try (ResultSet rs = meta.getColumns(null, null, "estacionamento", "saida")) {
                if (rs.next()) {
                    int dataType = rs.getInt("DATA_TYPE");
                    saidaIsDatetime = (dataType == Types.TIMESTAMP || dataType == Types.DATE || dataType == Types.TIME);
                    saidaMaxLen = rs.getInt("COLUMN_SIZE");
                }
            }
        } catch (SQLException ex) {
            // If metadata fails, leave defaults; the DAO will still attempt safe parsing.
            ex.printStackTrace();
        }
    }

    // Adiciona um novo veículo no estacionamento
    public void addVeiculo(Estacionamento estacionamento) {
        String sql = "INSERT INTO estacionamento (marca, modelo, placa, entrada, saida) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, estacionamento.getMarca());
            stmt.setString(2, estacionamento.getModelo());
            stmt.setString(3, estacionamento.getPlaca());
            // Based on column types, set as Timestamp (DATETIME) or as string (VARCHAR) safely
            setColumnFromString(stmt, 4, estacionamento.getEntrada(), entradaIsDatetime, entradaMaxLen);
            setColumnFromString(stmt, 5, estacionamento.getSaida(), saidaIsDatetime, saidaMaxLen);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Retorna todos os veículos cadastrados
    public List<Estacionamento> getAllVeiculos() {
        List<Estacionamento> veiculos = new ArrayList<>();
        String sql = "SELECT * FROM estacionamento";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Read columns depending on type
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String entradaStr = "";
                String saidaStr = "";
                if (entradaIsDatetime) {
                    Timestamp tsEntrada = rs.getTimestamp("entrada");
                    entradaStr = (tsEntrada != null) ? tsEntrada.toLocalDateTime().format(fmt) : "";
                } else {
                    entradaStr = (rs.getString("entrada") != null) ? rs.getString("entrada") : "";
                }
                if (saidaIsDatetime) {
                    Timestamp tsSaida = rs.getTimestamp("saida");
                    saidaStr = (tsSaida != null) ? tsSaida.toLocalDateTime().format(fmt) : "";
                } else {
                    saidaStr = (rs.getString("saida") != null) ? rs.getString("saida") : "";
                }
                Estacionamento estacionamento = new Estacionamento(
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getString("placa"),
                    entradaStr,
                    saidaStr
                );
                veiculos.add(estacionamento);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return veiculos;
    }

    /*
     * Observação: entrada e saida são armazenados na base como DATETIME.
     * São convertidos para String no formato "dd/MM/yyyy HH:mm" para exibição na GUI.
     */

    // Retorna um veículo pelo ID
    public Estacionamento getVeiculoById(int id) {
        String sql = "SELECT * FROM estacionamento WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String entradaStr = "";
                    String saidaStr = "";
                    if (entradaIsDatetime) {
                        Timestamp tsEntrada = rs.getTimestamp("entrada");
                        entradaStr = (tsEntrada != null) ? tsEntrada.toLocalDateTime().format(fmt) : "";
                    } else {
                        entradaStr = (rs.getString("entrada") != null) ? rs.getString("entrada") : "";
                    }
                    if (saidaIsDatetime) {
                        Timestamp tsSaida = rs.getTimestamp("saida");
                        saidaStr = (tsSaida != null) ? tsSaida.toLocalDateTime().format(fmt) : "";
                    } else {
                        saidaStr = (rs.getString("saida") != null) ? rs.getString("saida") : "";
                    }
                    return new Estacionamento(
                            rs.getInt("id"),
                            rs.getString("marca"),
                            rs.getString("modelo"),
                            rs.getString("placa"),
                            entradaStr,
                            saidaStr
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Atualiza dados de um veículo
    public void updateVeiculo(Estacionamento estacionamento) {
        String sql = "UPDATE estacionamento SET marca = ?, modelo = ?, placa = ?, entrada = ?, saida = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, estacionamento.getMarca());
            stmt.setString(2, estacionamento.getModelo());
            stmt.setString(3, estacionamento.getPlaca());
            setColumnFromString(stmt, 4, estacionamento.getEntrada(), entradaIsDatetime, entradaMaxLen);
            setColumnFromString(stmt, 5, estacionamento.getSaida(), saidaIsDatetime, saidaMaxLen);
            stmt.setInt(6, estacionamento.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper that sets a PreparedStatement parameter either as Timestamp (if column is DATETIME) or as String (if column is VARCHAR).
    private void setColumnFromString(PreparedStatement stmt, int parameterIndex, String dateStr, boolean isDatetime, int maxLen) throws SQLException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            if (isDatetime) stmt.setNull(parameterIndex, Types.TIMESTAMP);
            else stmt.setNull(parameterIndex, Types.VARCHAR);
            return;
        }
        if (isDatetime) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            try {
                LocalDateTime dt = LocalDateTime.parse(dateStr, fmt);
                stmt.setTimestamp(parameterIndex, Timestamp.valueOf(dt));
            } catch (DateTimeParseException e) {
                try {
                    LocalDateTime dt = LocalDateTime.parse(dateStr);
                    stmt.setTimestamp(parameterIndex, Timestamp.valueOf(dt));
                } catch (DateTimeParseException e2) {
                    stmt.setNull(parameterIndex, Types.TIMESTAMP);
                }
            }
        } else {
            String toStore = dateStr;
            if (maxLen > 0 && toStore.length() > maxLen) {
                toStore = toStore.substring(0, maxLen);
            }
            stmt.setString(parameterIndex, toStore);
        }
    }

    // Deleta um veículo pelo ID
    public void deleteVeiculo(int id) {
        String sql = "DELETE FROM estacionamento WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Helper: setColumnFromString
     * Converte uma string conforme coluna: se coluna for DATETIME, converte para Timestamp; se for VARCHAR, armazena como string truncada.
     */
}
