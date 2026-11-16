package com.Handoff.backend.dto;

import java.util.List;

public record AdminStudentPageResponse(List<AdminStudentItem> items, Meta meta) {
    public record Meta(long totalItems, int currentPage, int totalPages, int pageSize) {
    }
}
