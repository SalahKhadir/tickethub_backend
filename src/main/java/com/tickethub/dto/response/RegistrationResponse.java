package com.tickethub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistrationResponse {
    private String message;
    private boolean pendingApproval;
}

