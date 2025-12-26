package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> getByYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() == year)
                .toList();
    }

    public Movie findById(long id) {
        return movies.get(id);
    }

    public Movie save(Movie movie) {
        long id = idGenerator.getAndIncrement();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        idGenerator.set(1);
    }
}