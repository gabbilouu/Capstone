package com.example.elevate;

import java.io.Serializable;
import java.util.List;

public class Task implements Serializable {
    private String id;            // Firestore document ID
    private String name;
    private String emoji;
    private String repeatType;    // Daily, Weekly, Monthly, Select Days
    private List<String> repeatDays; // Only used if repeatType == "Select Days"
    private String startDate;     // Format: "Sep 8, 2025"
    private String startTime;     // Format: "00:00AM"
    private String endDate;
    private String endTime;
    private String taskType;      // Sleep, Exercise, etc.
    private String notes;
    private boolean completed;

    public Task() {
        // Firestore requires empty constructor
    }

    public Task(String id, String name, String emoji, String repeatType, List<String> repeatDays,
                String startDate, String startTime, String endDate, String endTime,
                String taskType, String notes, boolean completed) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.repeatType = repeatType;
        this.repeatDays = repeatDays;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.taskType = taskType;
        this.notes = notes;
        this.completed = completed;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }

    public List<String> getRepeatDays() { return repeatDays; }
    public void setRepeatDays(List<String> repeatDays) { this.repeatDays = repeatDays; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
