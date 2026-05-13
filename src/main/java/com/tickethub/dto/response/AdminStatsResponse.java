package com.tickethub.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalTickets;
    private long openTickets;
    private long resolvedToday;
    private long criticalSLA;
    private Map<String, Long> ticketsByCategory;
    private long totalUsers;
    private String avgResolutionTime;
}
