package com.tickethub.controller;

import com.tickethub.dto.response.AdminStatsResponse;
import com.tickethub.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private final TicketService ticketService;

    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<AdminStatsResponse> getGlobalStats() {
        return ResponseEntity.ok(ticketService.getAdminGlobalStats());
    }
}

