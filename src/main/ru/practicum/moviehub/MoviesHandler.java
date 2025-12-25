package ru.practicum.moviehub;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.http.BaseHttpHandler;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final int YEAR_RANGE = 1888;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/movies")) {
            if ("GET".equalsIgnoreCase(method)) {
                handleGetAll(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                exchange.close();
            }
        } else if (path.matches("/movies/\\d+")) {
            if ("GET".equalsIgnoreCase(method)) {
                handleGetById(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        // Проверка параметра ?year=...
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.startsWith("year=")) {
            String yearParam = query.substring(5);
            int year = Integer.parseInt(yearParam);
            int currentYear = java.time.Year.now().getValue();
            try {
                if (year < YEAR_RANGE || year > currentYear + 1) {
                    sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                    return;
                }
                List<Movie> movies = store.getByYear(year);
                sendJson(exchange, 200, movies);
            } catch (NumberFormatException e) {
                sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
            }
            return;
        }

        // Обычный GET /movies
        List<Movie> movies = store.getAll();
        sendJson(exchange, 200, movies);
    }

    private void handleGetById(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        String idStr = parts[parts.length - 1];
        try {
            long id = Long.parseLong(idStr);
            Movie movie = store.findById(id);
            if (movie == null) {
                sendError(exchange, 404, "Фильм не найден");
            } else {
                sendJson(exchange, 200, movie);
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            exchange.sendResponseHeaders(415, -1);
            exchange.close();
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(body, Movie.class);

            // Валидация
            String title = movie.getTitle();
            int year = movie.getYear();
            int currentYear = java.time.Year.now().getValue();

            boolean valid = true;
            java.util.List<String> errors = new java.util.ArrayList<>();

            if (title == null || title.trim().isEmpty()) {
                errors.add("название не должно быть пустым");
                valid = false;
            } else if (title.length() > 100) {
                errors.add("название не должно превышать 100 символов");
                valid = false;
            }

            if (year < YEAR_RANGE || year > currentYear + 1) {
                errors.add("год должен быть между 1888 и " + (currentYear + 1));
                valid = false;
            }

            if (!valid) {
                sendError(exchange, 422, "Ошибка валидации", errors.toArray(new String[0]));
                return;
            }

            movie.setTitle(title.trim());
            Movie saved = store.save(movie);
            sendJson(exchange, 201, saved);

        } catch (JsonSyntaxException e) {
            sendError(exchange, 422, "Ошибка валидации", "некорректный JSON");
        } catch (Exception e) {
            sendError(exchange, 422, "Ошибка валидации", "некорректные данные");
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        String idStr = parts[parts.length - 1];
        try {
            long id = Long.parseLong(idStr);
            if (store.delete(id)) {
                sendNoContent(exchange);
            } else {
                sendError(exchange, 404, "Фильм не найден");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
        }
    }
}