package com.erp.erpbackend.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * POST /api/ai/chat
     * Body: { "message": "...", "history": [ { "role": "user"/"assistant", "content": "..." } ] }
     */
    @PostMapping("/chat")
    public ResponseEntity<AiResponse> chat(@RequestBody AiRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AiResponse("Message cannot be empty", false));
        }
        AiResponse response = aiService.chat(
                request.getMessage(),
                request.getHistory(),
                request.getModel()
        );
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/ai/multimodal
     * Body: { "message": "...", "base64Image": "...", "mimeType": "image/jpeg", "history": [...] }
     */
    @PostMapping("/multimodal")
    public ResponseEntity<AiResponse> multimodal(@RequestBody MultimodalRequest request) {
        if (request.getMessage() == null && request.getBase64Image() == null) {
            return ResponseEntity.badRequest()
                    .body(new AiResponse("Provide a message or image.", false));
        }
        AiResponse response = aiService.chatMultimodal(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/ai/summarize
     * Body: { "notesContent": "...", "subject": "Mathematics" }
     */
    @PostMapping("/summarize")
    public ResponseEntity<AiResponse> summarize(@RequestBody NotesSummaryRequest request) {
        if (request.getNotesContent() == null || request.getNotesContent().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AiResponse("Notes content cannot be empty", false));
        }
        AiResponse response = aiService.summarizeNotes(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/ai/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI service is running");
    }
}