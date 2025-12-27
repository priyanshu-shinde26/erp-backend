package com.erp.erpbackend.attendance;

public class StudentDto {

    private String rollNumber;
    private String name;

    public StudentDto() {}

    public StudentDto(String rollNumber, String name) {
        this.rollNumber = rollNumber;
        this.name = name;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
