package com.tickethub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianStatsResponse {
    private long assignedTickets;
    private long inProgress;
    private long criticalPriority;
    private long resolvedToday;
}

