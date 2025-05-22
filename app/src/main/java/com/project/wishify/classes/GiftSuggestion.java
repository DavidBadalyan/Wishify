package com.project.wishify.classes;

public class GiftSuggestion {
    private String title;
    private String description;

    public GiftSuggestion(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}