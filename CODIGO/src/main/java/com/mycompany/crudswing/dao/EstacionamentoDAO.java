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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mycompany.crudswing.DatabaseConnection;
import com.mycompany.crudswing.Estacionamento;

public class EstacionamentoDAO {
    private static final Logger LOGGER = Logger.getLogger(EstacionamentoDAO.class.getName());
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
            LOGGER.log(Level.SEVERE, "Erro ao conectar ao banco de dados", e);
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
            LOGGER.log(Level.SEVERE, "Erro ao detectar tipos de coluna", ex);
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
            LOGGER.log(Level.SEVERE, "Erro ao adicionar veículo", e);
        }
    }

    // Retorna todos os veículos cadastrados
    public List<Estacionamento> getAllVeiculos() {
        List<Estacionamento> veiculos = new ArrayList<>();
        String sql = "SELECT * FROM estacionamento";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                veiculos.add(new Estacionamento(
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getString("placa"),
                    rs.getString("entrada"),
                    rs.getString("saida")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar veículos", e);
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
                    return new Estacionamento(
                            rs.getInt("id"),
                            rs.getString("marca"),
                            rs.getString("modelo"),
                            rs.getString("placa"),
                            rs.getString("entrada"),
                            rs.getString("saida")
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar veículo por ID", e);
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
            LOGGER.log(Level.SEVERE, "Erro ao atualizar veículo", e);
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
                // Tentar interpretar como LocalDateTime com o formato esperado
                LocalDateTime dt = LocalDateTime.parse(dateStr, fmt);
                stmt.setTimestamp(parameterIndex, Timestamp.valueOf(dt));
            } catch (DateTimeParseException e) {
                // Tentar corrigir horas ambíguas (e.g., "dd/MM/yyyy 2")
                if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}")) {
                    try {
                        String correctedDateStr = dateStr + ":00"; // Adicionar minutos padrão
                        LocalDateTime dt = LocalDateTime.parse(correctedDateStr, fmt);
                        stmt.setTimestamp(parameterIndex, Timestamp.valueOf(dt));
                        return;
                    } catch (DateTimeParseException ignored) {
                    }
                }
                // Se falhar, definir como NULL
                stmt.setNull(parameterIndex, Types.TIMESTAMP);
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
            LOGGER.log(Level.SEVERE, "Erro ao deletar veículo", e);
        }
    }

    // Deleta todos os veículos
    public void deleteAllVeiculos() {
        String sql = "DELETE FROM estacionamento";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao apagar todos os registros", e);
        }
    }

    /*
     * Helper: setColumnFromString
     * Converte uma string conforme coluna: se coluna for DATETIME, converte para Timestam  p; se for VARCHAR, armazena como string truncada.
     */
}
