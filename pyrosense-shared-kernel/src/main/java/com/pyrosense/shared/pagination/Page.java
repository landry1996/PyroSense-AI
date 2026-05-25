package com.pyrosense.shared.pagination;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Application-level page model (framework-agnostic).
 * Wraps a slice of results with pagination metadata.
 */
public record Page<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public Page {
        Objects.requireNonNull(content, "Content must not be null");
        if (pageNumber < 0) throw new IllegalArgumentException("Page number must be >= 0");
        if (pageSize < 1) throw new IllegalArgumentException("Page size must be >= 1");
        if (totalElements < 0) throw new IllegalArgumentException("Total elements must be >= 0");
    }

    public static <T> Page<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new Page<>(List.copyOf(content), pageNumber, pageSize, totalElements, totalPages);
    }

    public static <T> Page<T> empty(int pageSize) {
        return new Page<>(List.of(), 0, pageSize, 0, 0);
    }

    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        List<R> mapped = content.stream().map(mapper).toList();
        return new Page<>(mapped, pageNumber, pageSize, totalElements, totalPages);
    }
}
