package io.github.djaysharma007.cascade.plugins.actions;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Failure;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.registry.TemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Sends an HTTP request (POST/GET/PUT) to an external webhook endpoint.
 */
@Plugin(id = "http-webhook", displayName = "HTTP webhook", category = PluginCategory.ACTION)
public final class HttpWebhookAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(HttpWebhookAction.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final String url;
    private final String method;
    private final String payload;
    private final String contentType;
    private final int timeoutSeconds;
    private final HttpClient httpClient;

    public HttpWebhookAction(String url, String method, String payload, String contentType, Integer timeoutSeconds) {
        this(url, method, payload, contentType, timeoutSeconds, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS))
                .build());
    }

    public HttpWebhookAction(String url, String method, String payload, String contentType, Integer timeoutSeconds, HttpClient httpClient) {
        Objects.requireNonNull(url, "url must not be null");
        this.url = url.trim();
        this.method = method != null && !method.isBlank() ? method.trim().toUpperCase() : "POST";
        this.payload = payload != null ? payload : "";
        this.contentType = contentType != null && !contentType.isBlank() ? contentType.trim() : "application/json";
        this.timeoutSeconds = timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String resolvedUrl = TemplateResolver.resolve(url, event, context, null);
        String resolvedPayload = TemplateResolver.resolve(payload, event, context, null);

        URI uri;
        try {
            uri = URI.create(resolvedUrl);
        } catch (IllegalArgumentException e) {
            return new ExecutionResult.Failed(new Failure.Defect(e, "Invalid webhook URL: " + resolvedUrl));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", contentType);

        switch (method) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(resolvedPayload));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(resolvedPayload));
            case "GET" -> builder.GET();
            default -> {
                return new ExecutionResult.Failed(new Failure.Defect(null, "Unsupported HTTP method: " + method));
            }
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                log.info("HTTP webhook {} {} succeeded with status {}", method, uri, statusCode);
                return new ExecutionResult.Success(Map.of(
                        "statusCode", statusCode,
                        "response", responseBody != null ? responseBody : ""
                ));
            } else if (statusCode >= 500) {
                log.warn("HTTP webhook {} {} failed with server status {}: {}", method, uri, statusCode, responseBody);
                return new ExecutionResult.Failed(
                        new Failure.Transient("HTTP server error " + statusCode + ": " + responseBody)
                );
            } else {
                log.warn("HTTP webhook {} {} returned client error {}: {}", method, uri, statusCode, responseBody);
                return new ExecutionResult.Failed(
                        new Failure.Expected("HTTP request failed with status " + statusCode + ": " + responseBody)
                );
            }
        } catch (Exception e) {
            log.warn("HTTP webhook {} {} failed with network exception: {}", method, uri, e.getMessage());
            return new ExecutionResult.Failed(new Failure.Transient("HTTP request failed: " + e.getMessage()));
        }
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getPayload() {
        return payload;
    }
}
