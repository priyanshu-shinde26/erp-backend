package com.erp.erpbackend.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * Plain text chat endpoint
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<AiResponse> chat(@RequestBody AiRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AiResponse(null, false, "Message cannot be empty"));
        }
        AiResponse response = aiService.chat(
                request.getMessage(),
                request.getRole(),
                request.getConversationHistory()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Multimodal chat: JSON with base64 file
     * POST /api/ai/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<AiResponse> analyzeFile(@RequestBody MultimodalRequest request) {
        if (request.getFileBase64() == null || request.getFileBase64().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AiResponse(null, false, "File data is required"));
        }
        AiResponse response = aiService.analyzeFileWithText(
                request.getMessage() != null ? request.getMessage() : "Analyze this file",
                request.getFileBase64(),
                request.getMimeType(),
                request.getRole()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Upload file directly (multipart) for analysis
     * POST /api/ai/analyze-upload
     */
    @PostMapping("/analyze-upload")
    public ResponseEntity<AiResponse> analyzeUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", defaultValue = "Analyze this file") String message,
            @RequestParam(value = "role", defaultValue = "student") String role) {
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = file.getContentType();

            AiResponse response = aiService.analyzeFileWithText(message, base64, mimeType, role);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new AiResponse(null, false, "Upload error: " + e.getMessage()));
        }
    }

    /**
     * Summarize a note from base64
     * POST /api/ai/summarize
     */
    @PostMapping("/summarize")
    public ResponseEntity<AiResponse> summarizeNote(@RequestBody NotesSummaryRequest request) {
        AiResponse response;
        if (request.getNoteUrl() != null && !request.getNoteUrl().isBlank()) {
            response = aiService.summarizeNoteFromUrl(
                    request.getNoteUrl(),
                    request.getNoteTitle() != null ? request.getNoteTitle() : "Note",
                    request.getRole()
            );
        } else if (request.getFileBase64() != null && !request.getFileBase64().isBlank()) {
            response = aiService.summarizeNotes(
                    request.getFileBase64(),
                    request.getMimeType(),
                    request.getNoteTitle() != null ? request.getNoteTitle() : "Note",
                    request.getRole()
            );
        } else {
            return ResponseEntity.badRequest()
                    .body(new AiResponse(null, false, "Either noteUrl or fileBase64 is required"));
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Health check for AI module
     * GET /api/ai/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CampusSync AI is running ✓");
    }
}