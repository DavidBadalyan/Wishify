package com.project.wishify.classes;

public class Birthday {
    private String id;
    private String name;
    private String date;
    private String phoneNumber;

    public Birthday() {}

    public Birthday(String id, String name, String date, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.phoneNumber = phoneNumber;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }
}