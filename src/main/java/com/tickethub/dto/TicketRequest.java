package com.tickethub.dto;

import com.tickethub.model.Priority;
import com.tickethub.model.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String description,

        @NotNull
        Priority priority,

        @NotNull
        TicketCategory category) {
}

