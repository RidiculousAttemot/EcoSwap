package com.example.ecoswap.models;

public class EcoSave {
    private String id;
    private String userId;
    private double co2Saved; // in kg
    private double waterSaved; // in liters
    private double wasteDiverted; // in kg
    private int itemsSwapped;
    private int itemsDonated;
    private String calculatedAt;
    
    public EcoSave() {
    }
    
    public EcoSave(String id, String userId, double co2Saved, double waterSaved, 
                   double wasteDiverted, int itemsSwapped, int itemsDonated, String calculatedAt) {
        this.id = id;
        this.userId = userId;
        this.co2Saved = co2Saved;
        this.waterSaved = waterSaved;
        this.wasteDiverted = wasteDiverted;
        this.itemsSwapped = itemsSwapped;
        this.itemsDonated = itemsDonated;
        this.calculatedAt = calculatedAt;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public double getCo2Saved() {
        return co2Saved;
    }
    
    public void setCo2Saved(double co2Saved) {
        this.co2Saved = co2Saved;
    }
    
    public double getWaterSaved() {
        return waterSaved;
    }
    
    public void setWaterSaved(double waterSaved) {
        this.waterSaved = waterSaved;
    }
    
    public double getWasteDiverted() {
        return wasteDiverted;
    }
    
    public void setWasteDiverted(double wasteDiverted) {
        this.wasteDiverted = wasteDiverted;
    }
    
    public int getItemsSwapped() {
        return itemsSwapped;
    }
    
    public void setItemsSwapped(int itemsSwapped) {
        this.itemsSwapped = itemsSwapped;
    }
    
    public int getItemsDonated() {
        return itemsDonated;
    }
    
    public void setItemsDonated(int itemsDonated) {
        this.itemsDonated = itemsDonated;
    }
    
    public String getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(String calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    public double getTreesEquivalent() {
        // Approximate: 1 tree absorbs ~21.77 kg CO2 per year
        return co2Saved / 21.77;
    }
}
