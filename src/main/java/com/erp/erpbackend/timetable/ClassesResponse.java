package com.erp.erpbackend.timetable;

import java.util.List;

public class ClassesResponse {
    private List<String> classes;

    public ClassesResponse() {}

    public ClassesResponse(List<String> classes) {
        this.classes = classes;
    }

    public List<String> getClasses() { return classes; }
    public void setClasses(List<String> classes) { this.classes = classes; }
}
