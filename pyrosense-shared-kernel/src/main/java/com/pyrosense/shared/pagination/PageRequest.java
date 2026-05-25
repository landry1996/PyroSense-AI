package com.pyrosense.shared.pagination;

/**
 * Application-level page request (framework-agnostic).
 */
public record PageRequest(int page, int size, String sortBy, SortDirection direction) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0");
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE);
        }
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, SortDirection.ASC);
    }

    public static PageRequest first(int size) {
        return of(0, size);
    }

    public static PageRequest defaults() {
        return of(0, DEFAULT_SIZE);
    }

    public long offset() {
        return (long) page * size;
    }

    public enum SortDirection {
        ASC, DESC
    }
}
