package com.tickethub.service;

import com.tickethub.dto.TicketRequest;
import com.tickethub.dto.TicketResponse;
import com.tickethub.model.TicketStatus;
import java.util.List;

public interface TicketService {
    TicketResponse createTicket(TicketRequest request);

    List<TicketResponse> getAllTickets();

    TicketResponse updateTicketStatus(Long id, TicketStatus newStatus);
}
