package com.tickethub.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull
        Long techId) {
}

