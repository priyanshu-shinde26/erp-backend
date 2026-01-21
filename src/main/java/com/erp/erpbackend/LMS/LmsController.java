package com.erp.erpbackend.LMS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/lms")
public class LmsController {

    @Autowired
    private LmsService lmsService;

    @PostMapping("/notes/{classId}")
    public ResponseEntity<String> uploadNote(@PathVariable String classId,
                                             @RequestParam String title,
                                             @RequestParam(value = "subject", required = false) String subject,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestHeader(value = "uid", required = false) String uid) throws IOException {

        // ✅ Validate file type
        String contentType = file.getContentType();
        if (!Arrays.asList(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "application/vnd.ms-powerpoint"
        ).contains(contentType)) {
            return ResponseEntity.badRequest().body("Only PDF, PPTX, DOCX, DOC allowed");
        }

        // ✅ Validate filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ResponseEntity.badRequest().body("Invalid filename");
        }

        // ✅ Upload + Save handled in service (Cloudinary + Firebase)
        String noteId = lmsService.uploadNote(
                classId,
                subject != null ? subject : "",
                title,
                file,
                uid
        );

        return ResponseEntity.ok(noteId);
    }

    @GetMapping("/notes/{classId}")
    public ResponseEntity<List<Note>> getNotes(@PathVariable String classId,
                                               @RequestHeader("role") String role,
                                               @RequestHeader(value = "class", required = false) String userClass) {
        List<Note> notes = lmsService.getNotesForClass(classId, role, userClass);
        return ResponseEntity.ok(notes);
    }

    @DeleteMapping("/notes/{classId}/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String classId, @PathVariable String noteId) {
        lmsService.deleteNote(classId, noteId);
        return ResponseEntity.ok().build();
    }
}
