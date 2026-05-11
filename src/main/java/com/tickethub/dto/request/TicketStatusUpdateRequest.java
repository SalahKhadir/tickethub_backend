package com.tickethub.dto.request;

import com.tickethub.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusUpdateRequest(
        @NotNull
                TicketStatus newStatus,
        String solution) {
}
