package nl.serverpapi.standupCommentator.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OllamaClient {
    private final String url, model;
    private static final Gson gson = new Gson();

    public OllamaClient(String url, String model) {
        this.url = url;
        this.model = model;
    }

    // Sync function
    public String doOllamaRequest(String prompt) throws IOException, InterruptedException {
        // Maak het request JSON-object
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false); // no streaming

        // Bouw de HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody), StandardCharsets.UTF_8))
                .build();

        // Verstuur en ontvang de response
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            return json.has("response") ? json.get("response").getAsString() : "";
        } else {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }
    }
}
