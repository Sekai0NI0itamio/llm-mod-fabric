package com.stevennovak.llmmod.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stevennovak.llmmod.config.LlmConfig;
import com.stevennovak.llmmod.inference.LlamaEngine;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LlmHttpServer {
    private static HttpServer server;
    private static final Gson GSON = new Gson();
    public static final LlamaEngine ENGINE = new LlamaEngine();
    private static final Map<String, Long> lastRequest = new ConcurrentHashMap<>();

    public static LlamaEngine getEngine() { return ENGINE; }

    public static void start() {
        LlmConfig cfg = LlmConfig.get();
        try {
            if (server != null) return;
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", cfg.port), 0);
            server.createContext("/v1/chat/completions", new ChatCompletionsHandler());
            server.createContext("/v1/completions", new CompletionsHandler());
            server.createContext("/v1/models", new ModelsHandler());
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
            server.start();
            System.out.println("[LLMMod] HTTP server started on port " + cfg.port);
        } catch (IOException e) {
            System.err.println("[LLMMod] Failed to start HTTP server: " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        ENGINE.unload();
    }

    static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        byte[] buf = is.readAllBytes();
        return new String(buf, StandardCharsets.UTF_8);
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static boolean checkAuth(HttpExchange ex) {
        LlmConfig cfg = LlmConfig.get();
        String token = cfg.apiKey;
        if (token == null || token.isEmpty()) return false;
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null) return false;
        if (!auth.startsWith("Bearer ")) return false;
        return auth.substring(7).trim().equals(token);
    }

    static boolean rateLimit(HttpExchange ex) {
        String ip = ex.getRemoteAddress().getAddress().getHostAddress();
        long now = System.currentTimeMillis();
        Long last = lastRequest.get(ip);
        if (last != null && (now - last) < 1000) return false;
        lastRequest.put(ip, now);
        return true;
    }

    static class ChatCompletionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            if (!checkAuth(ex)) {
                sendJson(ex, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            if (!rateLimit(ex)) {
                sendJson(ex, 429, "{\"error\":\"rate limited\"}");
                return;
            }
            String body = readBody(ex);
            JsonObject req;
            try {
                req = JsonParser.parseString(body).getAsJsonObject();
            } catch (Exception e) {
                sendJson(ex, 400, "{\"error\":\"invalid json\"}");
                return;
            }
            String model = req.has("model") ? req.get("model").getAsString() : LlmConfig.get().modelName;
            int maxTokens = req.has("max_tokens") ? req.get("max_tokens").getAsInt() : LlmConfig.get().maxTokens;
            float temp = req.has("temperature") ? req.get("temperature").getAsFloat() : LlmConfig.get().temperature;
            List<Map<String, String>> msgs = new ArrayList<>();
            if (req.has("messages") && req.get("messages").isJsonArray()) {
                JsonArray arr = req.getAsJsonArray("messages");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject m = arr.get(i).getAsJsonObject();
                    Map<String, String> mm = new HashMap<>();
                    mm.put("role", m.has("role") ? m.get("role").getAsString() : "user");
                    mm.put("content", m.has("content") ? m.get("content").getAsString() : "");
                    msgs.add(mm);
                }
            }
            String systemPrompt = LlmConfig.get().systemPrompt;
            // Allow system message from request to override/augment? Keep config system prompt as base; if request has system msg, use it.
            // Extract system message from messages if present
            StringBuilder sysFromReq = new StringBuilder();
            List<Map<String, String>> chatMsgs = new ArrayList<>();
            for (Map<String, String> m : msgs) {
                if ("system".equals(m.get("role"))) {
                    if (sysFromReq.length() > 0) sysFromReq.append("\n");
                    sysFromReq.append(m.get("content"));
                } else {
                    chatMsgs.add(m);
                }
            }
            String sys = sysFromReq.length() > 0 ? sysFromReq.toString() : systemPrompt;

            String prompt = LlamaEngine.buildChatML(sys, chatMsgs);

            if (!ENGINE.isLoaded()) {
                sendJson(ex, 503, "{\"error\":\"no model loaded\"}");
                return;
            }
            String content;
            try {
                content = ENGINE.complete(prompt, maxTokens, temp);
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                return;
            }
            // strip trailing im_end if present
            content = content.replace("<|im_end|>", "").trim();

            JsonObject choiceMsg = new JsonObject();
            choiceMsg.addProperty("role", "assistant");
            choiceMsg.addProperty("content", content);
            JsonObject choice = new JsonObject();
            choice.addProperty("index", 0);
            choice.add("message", choiceMsg);
            choice.addProperty("finish_reason", "stop");
            JsonArray choices = new JsonArray();
            choices.add(choice);

            JsonObject usage = new JsonObject();
            usage.addProperty("prompt_tokens", 0);
            usage.addProperty("completion_tokens", 0);
            usage.addProperty("total_tokens", 0);

            JsonObject resp = new JsonObject();
            resp.addProperty("id", "chatcmpl-" + System.currentTimeMillis());
            resp.addProperty("object", "chat.completion");
            resp.addProperty("created", System.currentTimeMillis() / 1000);
            resp.addProperty("model", model);
            resp.add("choices", choices);
            resp.add("usage", usage);

            sendJson(ex, 200, GSON.toJson(resp));
        }
    }

    static class CompletionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            if (!checkAuth(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
            if (!rateLimit(ex)) { sendJson(ex, 429, "{\"error\":\"rate limited\"}"); return; }
            String body = readBody(ex);
            JsonObject req;
            try { req = JsonParser.parseString(body).getAsJsonObject(); }
            catch (Exception e) { sendJson(ex, 400, "{\"error\":\"invalid json\"}"); return; }
            String prompt = req.has("prompt") ? req.get("prompt").getAsString() : "";
            int maxTokens = req.has("max_tokens") ? req.get("max_tokens").getAsInt() : LlmConfig.get().maxTokens;
            float temp = req.has("temperature") ? req.get("temperature").getAsFloat() : LlmConfig.get().temperature;
            if (!ENGINE.isLoaded()) { sendJson(ex, 503, "{\"error\":\"no model loaded\"}"); return; }
            String content;
            try { content = ENGINE.complete(prompt, maxTokens, temp); }
            catch (Exception e) { sendJson(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}"); return; }
            content = content.replace("<|im_end|>", "").trim();
            JsonObject choice = new JsonObject();
            choice.addProperty("text", content);
            choice.addProperty("index", 0);
            choice.addProperty("finish_reason", "stop");
            JsonArray choices = new JsonArray();
            choices.add(choice);
            JsonObject resp = new JsonObject();
            resp.addProperty("id", "cmpl-" + System.currentTimeMillis());
            resp.addProperty("object", "text_completion");
            resp.addProperty("created", System.currentTimeMillis()/1000);
            resp.addProperty("model", LlmConfig.get().modelName);
            resp.add("choices", choices);
            sendJson(ex, 200, GSON.toJson(resp));
        }
    }

    static class ModelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!checkAuth(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
            JsonObject m = new JsonObject();
            m.addProperty("id", LlmConfig.get().modelName.isEmpty() ? "llm-mod" : LlmConfig.get().modelName);
            m.addProperty("object", "model");
            JsonArray data = new JsonArray();
            data.add(m);
            JsonObject resp = new JsonObject();
            resp.addProperty("object", "list");
            resp.add("data", data);
            sendJson(ex, 200, GSON.toJson(resp));
        }
    }
}