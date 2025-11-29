package com.erp.erpbackend.assignment;

public class AssignmentDtos {

    public static class CreateAssignmentRequest {
        private String title;
        private String description;
        private String classId;
        private String subject;
        private long dueDate; // epoch millis

        public CreateAssignmentRequest() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public long getDueDate() { return dueDate; }
        public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    }

    public static class UpdateAssignmentRequest {
        private String title;
        private String description;
        private String classId;
        private String subject;
        private long dueDate;

        public UpdateAssignmentRequest() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public long getDueDate() { return dueDate; }
        public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    }

    public static class GradeSubmissionRequest {
        private Integer marks;
        private String feedback;

        public GradeSubmissionRequest() {}

        public Integer getMarks() { return marks; }
        public void setMarks(Integer marks) { this.marks = marks; }

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }
}
