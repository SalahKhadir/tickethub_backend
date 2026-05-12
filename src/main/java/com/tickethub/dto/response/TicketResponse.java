package com.tickethub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        LocalDateTime updatedAt,
        LocalDateTime slaDeadline,
        String solution,
        @JsonProperty("author_name") String authorName,
        String assigneeName) {
}
