package com.stevennovak.llmmod.command;

import com.stevennovak.llmmod.config.LlmConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ModelDownloader {
    public static Path download(String repo, String file) throws Exception {
        Path modelsDir = LlmConfig.getModelsDir();
        Files.createDirectories(modelsDir);
        Path dest = modelsDir.resolve(file);
        String url = "https://huggingface.co/" + repo + "/resolve/main/" + file;
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(dest));
        if (resp.statusCode() != 200) {
            Files.deleteIfExists(dest);
            throw new RuntimeException("HTTP " + resp.statusCode() + " downloading " + url);
        }
        return dest;
    }
}