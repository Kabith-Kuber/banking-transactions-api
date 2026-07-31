package com.brainridge.banking.dto.response;

import java.util.List;

/**
 * A generic "one page of results" wrapper for list endpoints.
 *
 * <p>Transaction history could grow large, so instead of returning every
 * record at once we return one page plus enough information for the caller to
 * request the next one. {@code <T>} makes this reusable for any element type
 * (here it wraps {@link TransactionResponse}).
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code content} — the items on this page.</li>
 *   <li>{@code page} — which page this is (0-based).</li>
 *   <li>{@code size} — how many items each page holds.</li>
 *   <li>{@code totalElements} — how many items exist across all pages.</li>
 *   <li>{@code totalPages} — computed from the two numbers above.</li>
 * </ul>
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        // Round up: 21 items at size 20 needs 2 pages. Guard against divide-by-zero.
        this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
