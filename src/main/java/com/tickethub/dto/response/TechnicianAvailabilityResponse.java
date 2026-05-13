package com.tickethub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianAvailabilityResponse {
    private Long id;
    private String email;
    private String fullName;
    private long activeTicketsCount;
}

