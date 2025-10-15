package com.example.ecoswap.models;

public class Post {
    private String id;
    private String title;
    private String content;
    private String author;
    private String authorId;
    private String imageUrl;
    private String date;
    private int likes;
    private int comments;
    
    public Post() {
    }
    
    public Post(String id, String title, String content, String author, String authorId, 
                String imageUrl, String date, int likes, int comments) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.authorId = authorId;
        this.imageUrl = imageUrl;
        this.date = date;
        this.likes = likes;
        this.comments = comments;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public int getLikes() {
        return likes;
    }
    
    public void setLikes(int likes) {
        this.likes = likes;
    }
    
    public int getComments() {
        return comments;
    }
    
    public void setComments(int comments) {
        this.comments = comments;
    }
}
