package com.example.music2;

public class WorkSession {
    public int id;
    public int workMinutes;
    public int breakMinutes;
    public int sessionCount;

    public WorkSession(int id, int workMinutes, int breakMinutes, int sessionCount) {
        this.id = id;
        this.workMinutes = workMinutes;
        this.breakMinutes = breakMinutes;
        this.sessionCount = sessionCount;
    }
}
