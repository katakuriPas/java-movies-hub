package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    protected final Gson gson;

    protected BaseHttpHandler() {
        this.gson = new Gson();
    }

    protected void sendJson(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] response = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_JSON);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    protected void sendJson(HttpExchange exchange, int statusCode, Object obj) throws IOException {
        sendJson(exchange, statusCode, gson.toJson(obj));
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1); // No Content
        exchange.close();
    }

    protected void sendError(HttpExchange exchange, int statusCode, String error, String... details) throws IOException {
        ErrorResponse response = new ErrorResponse(error, List.of(details));
        sendJson(exchange, statusCode, response);
    }
}