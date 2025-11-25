package com.erp.erpbackend.timetable;

public class TimetableDtos {

    // ================== RESPONSE DTO ==================
    public static class TimetableEntryResponse {
        private Long id;
        private String className;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String subjectName;
        private String teacherName;
        private String classroom;

        public TimetableEntryResponse() {
        }

        public TimetableEntryResponse(TimetableEntry entity) {
            this.id = entity.getId();
            this.className = entity.getClassName();
            this.dayOfWeek = entity.getDayOfWeek();
            this.startTime = entity.getStartTime();
            this.endTime = entity.getEndTime();
            this.subjectName = entity.getSubjectName();
            this.teacherName = entity.getTeacherName();
            this.classroom = entity.getClassroom();
        }

        // getters & setters

        public Long getId() {
            return id;
        }

        public String getClassName() {
            return className;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getClassroom() {
            return classroom;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public void setTeacherName(String teacherName) {
            this.teacherName = teacherName;
        }

        public void setClassroom(String classroom) {
            this.classroom = classroom;
        }
    }

    // ================== CREATE REQUEST DTO ==================
    public static class CreateTimetableEntryRequest {
        private String className;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String subjectName;
        private String teacherName;
        private String classroom;

        public CreateTimetableEntryRequest() {
        }

        // getters & setters

        public String getClassName() {
            return className;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getClassroom() {
            return classroom;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public void setTeacherName(String teacherName) {
            this.teacherName = teacherName;
        }

        public void setClassroom(String classroom) {
            this.classroom = classroom;
        }
    }

    // ================== UPDATE REQUEST DTO ==================
    public static class UpdateTimetableEntryRequest {
        private String className;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String subjectName;
        private String teacherName;
        private String classroom;

        public UpdateTimetableEntryRequest() {
        }

        // getters & setters

        public String getClassName() {
            return className;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getClassroom() {
            return classroom;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public void setTeacherName(String teacherName) {
            this.teacherName = teacherName;
        }

        public void setClassroom(String classroom) {
            this.classroom = classroom;
        }
    }
}
