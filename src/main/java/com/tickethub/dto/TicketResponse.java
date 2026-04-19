package com.tickethub.dto;

import com.tickethub.model.Priority;
import com.tickethub.model.TicketCategory;
import com.tickethub.model.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        TicketCategory category,
        LocalDateTime createdAt,
        String authorName) {
}

