package com.tickethub.service;

import com.tickethub.dto.request.TicketRequest;
import com.tickethub.dto.request.TicketUpdateRequest;
import com.tickethub.dto.response.TicketResponse;
import com.tickethub.model.Priority;
import com.tickethub.model.TicketCategory;
import com.tickethub.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {
    TicketResponse createTicket(TicketRequest request);

    Page<TicketResponse> getAllTickets(Pageable pageable, TicketStatus status, Priority priority, TicketCategory category);

    TicketResponse getTicketById(Long id);

    TicketResponse assignTechnician(Long ticketId, Long techId);

    TicketResponse updateTicketStatus(Long id, TicketStatus newStatus, String solution);

    TicketResponse updateTicket(Long id, TicketUpdateRequest request);

    void deleteTicket(Long id);
}
