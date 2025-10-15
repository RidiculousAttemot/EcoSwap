package com.example.ecoswap.models;

public class Item {
    private String id;
    private String name;
    private String description;
    private String category;
    private String imageUrl;
    private String ownerId;
    private String status; // available, swapped, donated
    private String type; // swap, donation, bidding
    private double currentBid;
    private String createdAt;
    
    public Item() {
    }
    
    public Item(String id, String name, String description, String category, String imageUrl, 
                String ownerId, String status, String type, double currentBid, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
        this.status = status;
        this.type = type;
        this.currentBid = currentBid;
        this.createdAt = createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public double getCurrentBid() {
        return currentBid;
    }
    
    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
