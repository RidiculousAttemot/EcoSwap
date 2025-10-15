package com.example.ecoswap.models;

public class Bid {
    private String id;
    private String itemId;
    private String bidderId;
    private String bidderName;
    private double amount;
    private String timestamp;
    
    public Bid() {
    }
    
    public Bid(String id, String itemId, String bidderId, String bidderName, double amount, String timestamp) {
        this.id = id;
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getBidderId() {
        return bidderId;
    }
    
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    
    public String getBidderName() {
        return bidderName;
    }
    
    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
