package com.tickethub.dto.request;

import com.tickethub.model.Priority;

public record TicketUpdateRequest(
        String title,
        String description,
        Priority priority
) {}
