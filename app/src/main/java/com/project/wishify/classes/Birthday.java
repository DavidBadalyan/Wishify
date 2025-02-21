package com.project.wishify.classes;

public class Birthday {
    private String id;
    private String name;
    private String date;

    public Birthday() {}

    public Birthday(String id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }
}

