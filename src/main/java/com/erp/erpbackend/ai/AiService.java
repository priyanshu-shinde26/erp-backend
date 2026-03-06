package com.erp.erpbackend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

@Service
public class AiService {

    private static final String GEMINI_API_KEY = "AIzaSyCWbem3gf1EOsduRqf8D2Ws1vPUO8UhvXY";
    private static final String GEMINI_TEXT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
    private static final String SYSTEM_PROMPT_BASE =
            "You are CampusSync AI, a smart educational assistant integrated into the CampusSync ERP app. " +
                    "You help students understand concepts, clear doubts, summarize notes, and assist teachers and admins with academic queries. " +
                    "Be concise, friendly, and educational. Format your responses clearly using bullet points or numbered lists when appropriate. ";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Plain text chat ───────────────────────────────────────────────────────

    public AiResponse chat(String userMessage, String role, List<ChatMessage> history) {
        try {
            String systemContext = buildSystemContext(role);
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();

            // System instruction as first user turn
            ObjectNode systemTurn = objectMapper.createObjectNode();
            systemTurn.put("role", "user");
            ArrayNode systemParts = objectMapper.createArrayNode();
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", systemContext);
            systemParts.add(systemPart);
            systemTurn.set("parts", systemParts);
            contents.add(systemTurn);

            // Fake model ack
            ObjectNode modelAck = objectMapper.createObjectNode();
            modelAck.put("role", "model");
            ArrayNode ackParts = objectMapper.createArrayNode();
            ObjectNode ackPart = objectMapper.createObjectNode();
            ackPart.put("text", "Understood. I'm CampusSync AI, ready to help!");
            ackParts.add(ackPart);
            modelAck.set("parts", ackParts);
            contents.add(modelAck);

            // Add conversation history
            if (history != null) {
                for (ChatMessage msg : history) {
                    ObjectNode turn = objectMapper.createObjectNode();
                    turn.put("role", msg.getRole());
                    ArrayNode parts = objectMapper.createArrayNode();
                    ObjectNode part = objectMapper.createObjectNode();
                    part.put("text", msg.getContent());
                    parts.add(part);
                    turn.set("parts", parts);
                    contents.add(turn);
                }
            }

            // Current user message
            ObjectNode userTurn = objectMapper.createObjectNode();
            userTurn.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", userMessage);
            userParts.add(userPart);
            userTurn.set("parts", userParts);
            contents.add(userTurn);

            requestBody.set("contents", contents);
            addGenerationConfig(requestBody);

            String responseText = callGeminiApi(requestBody);
            return new AiResponse(responseText, true);

        } catch (Exception e) {
            return new AiResponse(null, false, "AI service error: " + e.getMessage());
        }
    }

    // ─── Multimodal (file + text) ──────────────────────────────────────────────

    public AiResponse analyzeFileWithText(String userMessage, String fileBase64,
                                          String mimeType, String role) {
        try {
            String systemContext = buildSystemContext(role);
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();

            // Single turn with both text and inline file data
            ObjectNode userTurn = objectMapper.createObjectNode();
            userTurn.put("role", "user");
            ArrayNode parts = objectMapper.createArrayNode();

            // System context as text part
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", systemContext + "\n\nUser request: " + userMessage);
            parts.add(systemPart);

            // File part
            ObjectNode filePart = objectMapper.createObjectNode();
            ObjectNode inlineData = objectMapper.createObjectNode();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", fileBase64);
            filePart.set("inlineData", inlineData);
            parts.add(filePart);

            userTurn.set("parts", parts);
            contents.add(userTurn);
            requestBody.set("contents", contents);
            addGenerationConfig(requestBody);

            String responseText = callGeminiApi(requestBody);
            return new AiResponse(responseText, true);

        } catch (Exception e) {
            return new AiResponse(null, false, "File analysis error: " + e.getMessage());
        }
    }

    // ─── Notes summarization ───────────────────────────────────────────────────

    public AiResponse summarizeNotes(String fileBase64, String mimeType,
                                     String noteTitle, String role) {
        String prompt = "Please provide a comprehensive summary of this note titled '" + noteTitle + "'. " +
                "Include: 1) Main topics covered, 2) Key concepts and definitions, " +
                "3) Important points to remember, 4) Any formulas or rules mentioned. " +
                "Format it in a clear, student-friendly way.";
        return analyzeFileWithText(prompt, fileBase64, mimeType, role);
    }

    // ─── URL-based summarization (fetch from Cloudinary) ─────────────────────

    public AiResponse summarizeNoteFromUrl(String noteUrl, String noteTitle, String role) {
        try {
            // Download file bytes from Cloudinary URL
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(noteUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());

            byte[] fileBytes = response.body();
            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            // Detect mime type from URL
            String mimeType = detectMimeType(noteUrl);
            return summarizeNotes(base64, mimeType, noteTitle, role);

        } catch (Exception e) {
            return new AiResponse(null, false, "URL fetch error: " + e.getMessage());
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private String callGeminiApi(ObjectNode requestBody) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_TEXT_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error " + response.statusCode()
                    + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText("No response generated.");
    }

    private void addGenerationConfig(ObjectNode requestBody) {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("temperature", 0.7);
        config.put("topK", 40);
        config.put("topP", 0.95);
        config.put("maxOutputTokens", 2048);
        requestBody.set("generationConfig", config);
    }

    private String buildSystemContext(String role) {
        String roleContext = switch (role == null ? "student" : role.toLowerCase()) {
            case "teacher" -> "You are assisting a teacher. Help with lesson planning, explaining topics to students, creating quiz questions, and academic content creation.";
            case "admin"   -> "You are assisting an admin. Help with academic management, student queries, institutional FAQs, and administrative tasks.";
            default        -> "You are assisting a student. Help them understand concepts, clear doubts, explain topics simply, and summarize their study materials.";
        };
        return SYSTEM_PROMPT_BASE + roleContext;
    }

    private String detectMimeType(String url) {
        url = url.toLowerCase();
        if (url.contains(".pdf"))  return "application/pdf";
        if (url.contains(".ppt"))  return "application/vnd.ms-powerpoint";
        if (url.contains(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (url.contains(".doc"))  return "application/msword";
        if (url.contains(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (url.contains(".png"))  return "image/png";
        if (url.contains(".jpg") || url.contains(".jpeg")) return "image/jpeg";
        if (url.contains(".txt"))  return "text/plain";
        return "application/octet-stream";
    }
}