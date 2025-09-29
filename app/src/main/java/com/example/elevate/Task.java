package com.example.elevate;

public class Task {
    private String icon;
    private String name;
    private boolean done;

    public Task(String icon, String name, boolean done) {
        this.icon = icon;
        this.name = name;
        this.done = done;
    }

    public String getIcon() { return icon; }
    public String getName() { return name; }
    public boolean isDone() { return done; }

    public void setIcon(String icon) { this.icon = icon; }
    public void setName(String name) { this.name = name; }
    public void setDone(boolean done) { this.done = done; }
}
