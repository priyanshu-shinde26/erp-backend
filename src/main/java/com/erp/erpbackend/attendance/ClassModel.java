package com.erp.erpbackend.attendance;

public class ClassModel {
    public String classId;
    public String name;
    public String course;
    public String year;

    public ClassModel() {}

    @Override
    public String toString() {
        return name != null ? name : classId;
    }
}
