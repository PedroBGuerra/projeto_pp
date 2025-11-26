package com.mycompany.crudswing;

import com.mycompany.crudswing.dao.EstacionamentoDAO;
import com.mycompany.crudswing.dao.PrecoDAO;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.SpinnerDateModel;
import java.time.format.DateTimeParseException;
// import java.util.ArrayList; // not used
import java.awt.event.*;
import java.util.List;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
// duplicate imports removed

public class EstacionamentoGUI extends JFrame {
    /**
     * EstacionamentoGUI: simples CRUD para veículos no estacionamento com registro de entrada e saída.
     * - Marca, Modelo, Placa são informados pelo usuário ao adicionar.
     * - Entrada é registrada automaticamente quando um veículo é adicionado (data/hora atual).
     * - Saída não é um campo editável global; cada linha da tabela possui um botão "Registrar Saída" na coluna Ação
     *   que abre um modal para definir o horário de saída (ou usar o horário atual) e calcula a taxa.
     * - Entrada/Saída são armazenadas como DATETIME no banco; a GUI exibe no formato dd/MM/yyyy HH:mm.
     */
    private JTextField txtMarca, txtModelo, txtPlaca;
    private JTextField txtSearch;
    private JLabel lblEntradaValue;
    private JTable table;
    private DefaultTableModel tableModel;
    private EstacionamentoDAO estacionamentoDAO;
    private static final double TAXA_POR_HORA = 5.0; // fallback: R$ por hora if pricing table not available
    private PrecoDAO precoDAO;

    public EstacionamentoGUI() {
        estacionamentoDAO = new EstacionamentoDAO();
        try {
            precoDAO = new com.mycompany.crudswing.dao.PrecoDAO();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initComponents();
        loadVeiculos();
    }

    private void initComponents() {
        setTitle("Controle de Estacionamento - CRUD com Java Swing e MySQL");
        setSize(650, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lblMarca = new JLabel("Marca:");
        lblMarca.setBounds(10, 10, 100, 25);
        panel.add(lblMarca);

        txtMarca = new JTextField();
        txtMarca.setBounds(120, 10, 200, 25);
        panel.add(txtMarca);

        JLabel lblModelo = new JLabel("Modelo:");
        lblModelo.setBounds(10, 45, 100, 25);
        panel.add(lblModelo);

        txtModelo = new JTextField();
        txtModelo.setBounds(120, 45, 200, 25);
        panel.add(txtModelo);

        JLabel lblPlaca = new JLabel("Placa:");
        lblPlaca.setBounds(10, 80, 100, 25);
        panel.add(lblPlaca);
        
        txtPlaca = new JTextField();
        txtPlaca.setBounds(120, 80, 200, 25);
        panel.add(txtPlaca);
        
        // Show Entrada (read-only label) — will be populated when a line is selected
        JLabel lblEntradaDisplay = new JLabel("Entrada:");
        lblEntradaDisplay.setBounds(350, 80, 80, 25);
        panel.add(lblEntradaDisplay);

        lblEntradaValue = new JLabel("");
        lblEntradaValue.setBounds(430, 80, 200, 25);
        panel.add(lblEntradaValue);
        
        JButton btnAdd = new JButton("Adicionar");
        btnAdd.setBounds(10, 190, 100, 25);
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addVeiculo();
            }
        });
        panel.add(btnAdd);
        
        JButton btnUpdate = new JButton("Atualizar");
        btnUpdate.setBounds(120, 190, 100, 25);
        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVeiculo();
            }
        });
        panel.add(btnUpdate);

        JButton btnDelete = new JButton("Deletar");
        btnDelete.setBounds(230, 190, 100, 25);
        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteVeiculo();
            }
        });
        panel.add(btnDelete);

        JLabel lblSearch = new JLabel("Pesquisar (placa/marca/modelo):");
        lblSearch.setBounds(350, 10, 200, 25);
        panel.add(lblSearch);

        txtSearch = new JTextField();
        txtSearch.setBounds(350, 35, 200, 25);
        panel.add(txtSearch);

        JButton btnSearch = new JButton("Pesquisar");
        btnSearch.setBounds(560, 35, 80, 25);
        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterVeiculos(txtSearch.getText());
            }
        });
        panel.add(btnSearch);

        // button to view price rules
        JButton btnViewPrices = new JButton("Ver Tarifas");
        btnViewPrices.setBounds(560, 70, 100, 25);
        btnViewPrices.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPriceRules();
            }
        });
        panel.add(btnViewPrices);

        // The per-row 'Registrar Saída' button will be provided inside the table; no global button needed.

        tableModel = new DefaultTableModel(new Object[]{"ID", "Marca", "Modelo", "Placa", "Entrada", "Ação"}, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(10, 230, 610, 180);
        panel.add(scrollPane);

        // When user clicks a row, populate the form fields with referencia data and show entry datetime.
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    txtMarca.setText(String.valueOf(tableModel.getValueAt(selectedRow, 1)));
                    txtModelo.setText(String.valueOf(tableModel.getValueAt(selectedRow, 2)));
                    txtPlaca.setText(String.valueOf(tableModel.getValueAt(selectedRow, 3)));
                    String entradaVal = String.valueOf(tableModel.getValueAt(selectedRow, 4));
                    try {
                        parseToDateTime(entradaVal);
                        lblEntradaValue.setText(entradaVal);
                    } catch (Exception ex) {
                        lblEntradaValue.setText(entradaVal + " (Inválido)");
                    }
                }
            }
        });

        // Add renderer/editor for the 'Ação' column to show a button per-row for registering saida
        table.getColumn("Ação").setCellRenderer(new ButtonRenderer());
        table.getColumn("Ação").setCellEditor(new ButtonEditor());

        add(panel);
    }

    private void addVeiculo() {
        // Add a new vehicle: Marca, Modelo, Placa come from input fields.
        // Entrada is set automatically to the current date/time and saved in the DB as DATETIME.
        String marca = txtMarca.getText();
        String modelo = txtModelo.getText();
        String placa = txtPlaca.getText();
        // entrada and saida not provided by user; entrada is set to now, saida is empty


        // Set entrada to now and saida empty when creating a new record
        String entradaNow = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Estacionamento veiculo = new Estacionamento(0, marca, modelo, placa, entradaNow, "");
        estacionamentoDAO.addVeiculo(veiculo);
        loadVeiculos();
        limparCampos();
    }

    private void updateVeiculo() {
        int selectedRow = table.getSelectedRow();
        // Update the selected vehicle's non-time fields (marca, modelo, placa).
        // Entrada/saida are preserved by retrieving the current record from DB before update.
        if (selectedRow >= 0) {
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String marca = txtMarca.getText();
            String modelo = txtModelo.getText();
            String placa = txtPlaca.getText();
            // Preserve entrada/saida by fetching existing record from DAO
            Estacionamento atual = estacionamentoDAO.getVeiculoById(id);
            String entrada = (atual != null) ? atual.getEntrada() : "";
            String saida = (atual != null) ? atual.getSaida() : "";

            Estacionamento veiculo = new Estacionamento(id, marca, modelo, placa, entrada, saida);
            estacionamentoDAO.updateVeiculo(veiculo);
            loadVeiculos();
            limparCampos();
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um veículo para atualizar.");
        }
    }

    private void deleteVeiculo() {
        int selectedRow = table.getSelectedRow();
        // Delete selected vehicle from DB.
        if (selectedRow >= 0) {
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            estacionamentoDAO.deleteVeiculo(id);
            loadVeiculos();
            limparCampos();
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um veículo para deletar.");
        }
    }

    private void loadVeiculos() {
        // Populate the table with data from DB. The last column is a button label "Registrar Saída".
        tableModel.setRowCount(0);
        List<Estacionamento> veiculos = estacionamentoDAO.getAllVeiculos();
        for (Estacionamento v : veiculos) {
            tableModel.addRow(new Object[]{v.getId(), v.getMarca(), v.getModelo(), v.getPlaca(), v.getEntrada(), "Registrar Saída"});
        }
    }

    private void limparCampos() {
        txtMarca.setText("");
        txtModelo.setText("");
        txtPlaca.setText("");
        lblEntradaValue.setText("");
    }

    private void filterVeiculos(String query) {
        // Client-side filtering of the loaded vehicles by Marca/Modelo/Placa. For large datasets prefer server-side.
        tableModel.setRowCount(0);
        List<Estacionamento> veiculos = estacionamentoDAO.getAllVeiculos();
        if (query == null || query.trim().isEmpty()) {
            for (Estacionamento v : veiculos) {
                tableModel.addRow(new Object[]{v.getId(), v.getMarca(), v.getModelo(), v.getPlaca(), v.getEntrada(), "Registrar Saída"});
            }
            return;
        }
        String q = query.trim().toLowerCase();
        for (Estacionamento v : veiculos) {
            if ((v.getMarca() != null && v.getMarca().toLowerCase().contains(q)) ||
                (v.getModelo() != null && v.getModelo().toLowerCase().contains(q)) ||
                (v.getPlaca() != null && v.getPlaca().toLowerCase().contains(q))) {
                tableModel.addRow(new Object[]{v.getId(), v.getMarca(), v.getModelo(), v.getPlaca(), v.getEntrada(), "Registrar Saída"});
            }
        }
    }

    private void showPriceRules() {
        if (precoDAO == null) {
            JOptionPane.showMessageDialog(this, "Tabela de preços indisponível.");
            return;
        }
        java.util.List<com.mycompany.crudswing.Preco> rules = precoDAO.getAllPrecos();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Max Horas", "Preço Base", "Extra / Hora"}, 0);
        for (com.mycompany.crudswing.Preco p : rules) {
            model.addRow(new Object[]{p.getId(), p.getMaxHours() == null ? "Mais" : p.getMaxHours(), p.getBasePrice(), p.getExtraPerHour() == null ? "-" : p.getExtraPerHour()});
        }
        JTable t = new JTable(model);
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new java.awt.Dimension(400, 150));
        JOptionPane.showMessageDialog(this, sp, "Tarifas", JOptionPane.PLAIN_MESSAGE);
    }

    // Removed calcularTaxaSelecionada: now calculated on per-row registrar saída action

    // Removed registrarSaidaSelecionada: functionality moved to per-row button editor

    private LocalDateTime parseToDateTime(String s) throws Exception {
        if (s == null || s.trim().isEmpty() || s.equals("null")) {
            throw new Exception("Valor vazio para data/hora.");
        }
        String str = s.trim();

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
            // Full date/time with 2-digit hour/min
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            // Allow single-digit hour formats (e.g., 26/11/2025 2)
            DateTimeFormatter.ofPattern("d/M/yyyy H"),
            DateTimeFormatter.ofPattern("d/M/yyyy H:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy H"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm"),
            // Date-only and time-only
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("HH:mm:ss")
        };

        for (DateTimeFormatter fmt : formatters) {
            // tenta LocalDateTime
            try {
                return LocalDateTime.parse(str, fmt);
            } catch (DateTimeParseException ignored) {
            }
            // tenta LocalDate
            try {
                return java.time.LocalDate.parse(str, fmt).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
            // tenta LocalTime
            try {
                return LocalDate.now().atTime(LocalTime.parse(str, fmt));
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException e) {
            throw new Exception("Formato de data/hora desconhecido: " + str);
        }
    }

    // Renderer for table button column
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    // Editor for table button column
    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private String label;
        private int row;

        public ButtonEditor() {
            this.button = new JButton();
            // No listener added here to avoid leaking 'this' during construction.
            // We attach the listener dynamically when the editor component is created in getTableCellEditorComponent.
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.label = (value == null) ? "" : value.toString();
            this.row = row;
            this.button.setText(this.label);
            // Remove any previous listeners and add a fresh one for this editor instance
            for (ActionListener al : this.button.getActionListeners()) {
                this.button.removeActionListener(al);
            }
            this.button.addActionListener(e -> actionPerformed(e));
            return this.button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }

        // Called when the per-row "Registrar Saída" button is clicked. It opens a small modal
        // with a date/time spinner. If confirmed, it sets the 'saida' timestamp on the DB for that record
        // and calculates the fee based on the difference between entrance and exit.
        @Override
        public void actionPerformed(ActionEvent e) {
            // when button is clicked, open modal to register saida for this.row
            int selectedRow = this.row;
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            Estacionamento atual = estacionamentoDAO.getVeiculoById(id);
            if (atual == null) {
                JOptionPane.showMessageDialog(EstacionamentoGUI.this, "Veículo não encontrado no banco.");
                fireEditingStopped();
                return;
            }

            SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE);
            JSpinner spinner = new JSpinner(model);
            JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy HH:mm");
            spinner.setEditor(editor);

            int option = JOptionPane.showOptionDialog(EstacionamentoGUI.this, new Object[] {"Data e hora de saída:", spinner}, "Registrar saída",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {
                Date date = (Date) spinner.getValue();
                LocalDateTime saida = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                String saidaStr = saida.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                Estacionamento atualizado = new Estacionamento(atual.getId(), atual.getMarca(), atual.getModelo(), atual.getPlaca(), atual.getEntrada(), saidaStr);
                estacionamentoDAO.updateVeiculo(atualizado);
                loadVeiculos();

                try {
                    LocalDateTime dtEntrada = parseToDateTime(atual.getEntrada());
                    long minutes = Duration.between(dtEntrada, saida).toMinutes();
                    double taxa;
                    if (precoDAO != null) {
                        double p = precoDAO.calculatePrice(minutes);
                        if (p >= 0) taxa = p; else taxa = Math.ceil(minutes / 60.0) * TAXA_POR_HORA;
                    } else {
                        taxa = Math.ceil(minutes / 60.0) * TAXA_POR_HORA;
                    }
                    String msg = String.format("Saída registrada: %s\nTaxa: R$ %.2f", saidaStr, taxa);
                    JOptionPane.showMessageDialog(EstacionamentoGUI.this, msg);
                } catch (Exception ex) {
                    // Entrada has invalid format. Offer user to set entrada to now to allow fee calculation.
                    String entradaStr = atual.getEntrada();
                    int choice = JOptionPane.showConfirmDialog(EstacionamentoGUI.this,
                            "Formato de entrada inválido: '" + entradaStr + "'.\nDeseja substituir a entrada por data/hora atual e calcular a taxa?",
                            "Entrada inválida", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        LocalDateTime now = LocalDateTime.now();
                        String entradaNow = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        // Update entrada to now and keep the recorded saida
                        Estacionamento atualizadoEntrada = new Estacionamento(atual.getId(), atual.getMarca(), atual.getModelo(), atual.getPlaca(), entradaNow, saidaStr);
                        estacionamentoDAO.updateVeiculo(atualizadoEntrada);
                        loadVeiculos();

                        // Recompute with new entrada using configured price table when available
                        long minutesNew = Duration.between(now, saida).toMinutes();
                        double taxa;
                        if (precoDAO != null) {
                            double p = precoDAO.calculatePrice(minutesNew);
                            if (p >= 0) taxa = p; else taxa = Math.ceil(minutesNew / 60.0) * TAXA_POR_HORA;
                        } else {
                            taxa = Math.ceil(minutesNew / 60.0) * TAXA_POR_HORA;
                        }
                        String msg = String.format("Saída registrada: %s\nEntrada corrigida para: %s\nTaxa: R$ %.2f", saidaStr, entradaNow, taxa);
                        JOptionPane.showMessageDialog(EstacionamentoGUI.this, msg);
                    } else {
                        JOptionPane.showMessageDialog(EstacionamentoGUI.this, "Saída registrada, mas não foi possível calcular a taxa devido ao formato inválido da entrada.");
                    }
                }
            }

            fireEditingStopped();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EstacionamentoGUI().setVisible(true));
    }
}
