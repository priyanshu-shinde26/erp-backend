package com.erp.erpbackend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    // "openrouter/free" auto-picks whichever free model has capacity right now.
    // Specific models below are tried only if the auto-router fails.
    private static final String[] FALLBACK_MODELS = {
            "openrouter/free",
            "google/gemini-2.0-flash-exp:free",
            "deepseek/deepseek-chat-v3-0324:free",
            "mistralai/mistral-small-3.1-24b-instruct:free",
            "qwen/qwen3-8b:free",
            "meta-llama/llama-3.3-70b-instruct:free"
    };

    private static final String VISION_MODEL = "meta-llama/llama-3.2-11b-vision-instruct:free";

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${app.site.url:https://campussync.app}")
    private String siteUrl;

    @Value("${app.site.name:CampusSync ERP}")
    private String siteName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── plain text chat with automatic fallback ─────────────────────────────

    public AiResponse chat(String userMessage, List<ChatMessage> history, String modelOverride) {

        // Build messages array once — reused across fallback attempts
        ArrayNode messages = buildMessages(history, userMessage);

        // If caller specifies a model, try only that
        if (modelOverride != null && !modelOverride.isBlank()) {
            try {
                ObjectNode body = mapper.createObjectNode();
                body.put("model", modelOverride);
                body.set("messages", messages);
                return callOpenRouter(body, modelOverride);
            } catch (Exception e) {
                log.error("AI chat error with override model {}", modelOverride, e);
                return new AiResponse("Failed to reach AI service: " + e.getMessage(), false);
            }
        }

        // Try each fallback model in order
        AiResponse lastResponse = null;
        for (String model : FALLBACK_MODELS) {
            try {
                log.info("Trying model: {}", model);
                ObjectNode body = mapper.createObjectNode();
                body.put("model", model);
                body.set("messages", messages);
                AiResponse response = callOpenRouter(body, model);
                if (response.isSuccess()) {
                    log.info("Success with model: {}", model);
                    return response;
                }
                log.warn("Model {} failed: {}, trying next...", model, response.getError());
                lastResponse = response;
            } catch (Exception e) {
                log.error("Exception with model {}: {}", model, e.getMessage());
                lastResponse = new AiResponse("Failed: " + e.getMessage(), false);
            }
        }

        log.error("All fallback models exhausted");
        return lastResponse != null ? lastResponse
                : new AiResponse("All AI models are currently busy. Please try again in a minute.", false);
    }

    // ─── multimodal (image) chat ──────────────────────────────────────────────

    public AiResponse chatMultimodal(MultimodalRequest req) {
        try {
            String mime = req.getMimeType() != null ? req.getMimeType() : "";
            boolean isImage = mime.startsWith("image/");

            if (isImage) {
                // ── Image: send as image_url with vision model ───────────────
                ObjectNode body = mapper.createObjectNode();
                body.put("model", VISION_MODEL);
                ArrayNode messages = body.putArray("messages");

                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", "You are CampusSync AI. Analyze images and answer academic questions helpfully.");

                if (req.getHistory() != null) {
                    for (ChatMessage h : req.getHistory()) {
                        ObjectNode msg = messages.addObject();
                        msg.put("role", h.getRole());
                        msg.put("content", h.getContent());
                    }
                }

                ObjectNode userMsg = messages.addObject();
                userMsg.put("role", "user");
                ArrayNode contentArr = userMsg.putArray("content");

                ObjectNode textPart = contentArr.addObject();
                textPart.put("type", "text");
                textPart.put("text", req.getMessage() != null ? req.getMessage() : "Describe this image.");

                if (req.getBase64Image() != null && !req.getBase64Image().isBlank()) {
                    ObjectNode imgPart = contentArr.addObject();
                    imgPart.put("type", "image_url");
                    ObjectNode imgUrl = imgPart.putObject("image_url");
                    imgUrl.put("url", "data:" + mime + ";base64," + req.getBase64Image());
                }

                return callOpenRouter(body, VISION_MODEL);

            } else {
                // ── Non-image file (PDF, DOCX, TXT, PPTX etc.) ───────────────
                // Decode base64 to raw bytes and try to extract as text
                String fileContent = "";
                if (req.getBase64Image() != null && !req.getBase64Image().isBlank()) {
                    try {
                        byte[] bytes = java.util.Base64.getDecoder().decode(req.getBase64Image());
                        // For text-based files, try to read as UTF-8 text
                        if (mime.startsWith("text/") || mime.contains("json") || mime.contains("xml")) {
                            fileContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                            // Truncate if too long
                            if (fileContent.length() > 8000) {
                                fileContent = fileContent.substring(0, 8000) + " [truncated]";
                            }
                        } else {
                            // Binary file (PDF, DOCX etc.) — can't extract text server-side without extra libs
                            // Tell AI what type of file it is so it gives a relevant response
                            fileContent = "[Binary file of type: " + mime + " — content cannot be extracted. " +
                                    "Please respond based on the filename and user message only.]";
                        }
                    } catch (Exception e) {
                        log.warn("Could not decode file content: {}", e.getMessage());
                        fileContent = "[File could not be read]";
                    }
                }

                String prompt = (req.getMessage() != null && !req.getMessage().isBlank()
                        ? req.getMessage() : "Analyze this file and provide a useful summary.")
                        + (fileContent.isBlank() ? "" : " File contents: " + fileContent);

                return chat(prompt, req.getHistory(), null);
            }

        } catch (Exception e) {
            log.error("AI multimodal error", e);
            return new AiResponse("File analysis failed: " + e.getMessage(), false);
        }
    }

    // ─── notes summary ────────────────────────────────────────────────────────

    public AiResponse summarizeNotes(NotesSummaryRequest req) {
        String prompt = "Please summarize the following " +
                (req.getSubject() != null ? req.getSubject() + " " : "") +
                "notes in a clear, concise way suitable for exam revision:\n\n" +
                req.getNotesContent();
        return chat(prompt, null, null);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ArrayNode buildMessages(List<ChatMessage> history, String userMessage) {
        ArrayNode messages = mapper.createArrayNode();

        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content",
                "You are CampusSync AI, a helpful academic assistant embedded in the CampusSync ERP platform. " +
                        "Help students and teachers with academic questions, assignments, study tips, and general knowledge. " +
                        "Be concise, friendly, and accurate.");

        if (history != null) {
            for (ChatMessage h : history) {
                ObjectNode msg = messages.addObject();
                msg.put("role", h.getRole());
                msg.put("content", h.getContent());
            }
        }

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        return messages;
    }

    // ─── internal HTTP call ───────────────────────────────────────────────────

    private AiResponse callOpenRouter(ObjectNode body, String model) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);

        log.info("=== OpenRouter REQUEST === model={}", model);
        log.info("API key present: {}", (apiKey != null && !apiKey.isBlank()
                && !apiKey.equals("YOUR_OPENROUTER_KEY_HERE")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", siteUrl)
                .header("X-Title", siteName)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("=== OpenRouter RESPONSE === status={}", response.statusCode());
        log.info("Body: {}", response.body());

        // 429 or 404 — let caller try next model
        if (response.statusCode() == 429 || response.statusCode() == 404) {
            return new AiResponse(
                    "HTTP " + response.statusCode() + " on model " + model + ": " + response.body(),
                    false);
        }

        if (response.statusCode() != 200) {
            log.error("OpenRouter error {}: {}", response.statusCode(), response.body());
            return new AiResponse(
                    "AI API error (HTTP " + response.statusCode() + "): " + response.body(),
                    false);
        }

        JsonNode root = mapper.readTree(response.body());
        String reply = root.path("choices").path(0).path("message").path("content").asText();

        if (reply == null || reply.isBlank()) {
            log.error("Empty reply from model {}. Full body: {}", model, response.body());
            return new AiResponse("AI returned an empty response. Please try again.", false);
        }

        log.info("Success! reply length={} model={}", reply.length(), model);
        return new AiResponse(reply, model);
    }
}