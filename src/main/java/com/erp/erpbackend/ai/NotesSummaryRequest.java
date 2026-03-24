package com.erp.erpbackend.ai;

public class NotesSummaryRequest {
    private String notesContent;
    private String subject;

    public NotesSummaryRequest() {}

    public String getNotesContent() { return notesContent; }
    public void setNotesContent(String notesContent) { this.notesContent = notesContent; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}