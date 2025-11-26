package com.mycompany.crudswing;

public class Preco {
    private int id;
    private Integer maxHours; // nullable; null means unlimited (catch-all rule)
    private double basePrice;
    private Double extraPerHour; // nullable; only used when maxHours is null

    public Preco(int id, Integer maxHours, double basePrice, Double extraPerHour) {
        this.id = id;
        this.maxHours = maxHours;
        this.basePrice = basePrice;
        this.extraPerHour = extraPerHour;
    }

    public int getId() { return id; }
    public Integer getMaxHours() { return maxHours; }
    public double getBasePrice() { return basePrice; }
    public Double getExtraPerHour() { return extraPerHour; }
}
