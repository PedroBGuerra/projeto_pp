package com.mycompany.crudswing;

public class Estacionamento {
    private int id;
    private String marca;
    private String modelo;
    private String placa;
    // entrada/saida are stored and displayed as strings formatted dd/MM/yyyy HH:mm
    // In the database they are saved as DATETIME; DAO converts between format and Timestamp.
    private String entrada;
    private String saida;

    public Estacionamento(int id, String marca, String modelo, String placa, String entrada, String saida) {
        this.id = id;
        this.marca = marca;
        this.modelo = modelo;
        this.placa = placa;
        this.entrada = entrada;
        this.saida = saida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getEntrada() {
        return entrada;
    }

    public void setEntrada(String entrada) {
        this.entrada = entrada;
    }

    public void setSaida(String saida) {
        this.saida = saida;
    }

        public String getSaida() {
        return saida;
    }
}
