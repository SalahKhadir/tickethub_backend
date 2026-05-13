package com.tickethub.service;

import com.tickethub.dto.response.TicketResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tickethub.repository.UserRepository;
import com.tickethub.model.Role;
import com.tickethub.model.User;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final UserRepository userRepository;

    // Store active emitters by user email
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String email) {
        // Force complete existing emitter if a new connection comes in
        if (emitters.containsKey(email)) {
            try {
                emitters.get(email).complete();
            } catch (Exception e) {
                // ignore
            }
            emitters.remove(email);
        }

        // 0 for no timeout (keep-alive)
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(email, emitter);
        log.info("SSE Connection established for user: {}", email);

        emitter.onCompletion(() -> {
            log.info("SSE Connection completed for user: {}", email);
            emitters.remove(email);
        });

        emitter.onTimeout(() -> {
            log.info("SSE Connection timed out for user: {}", email);
            emitters.remove(email);
        });

        emitter.onError((e) -> {
            log.error("SSE Connection error for user: {}", email, e);
            emitters.remove(email);
        });

        // Send an initial event to keep the connection alive
        try {
            emitter.send(SseEmitter.event().name("connected").data("ready"));
        } catch (IOException e) {
            log.debug("Cleaning up dead connection for user: {}", email);
            emitters.remove(email);
        }

        return emitter;
    }

    public void push(String email, TicketResponse ticket) {
        System.out.println("DEBUG: Pushing to " + email);
        SseEmitter emitter = emitters.get(email);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(ticket));
                log.info("Pushed notification to user: {}", email);
            } catch (IOException e) {
                log.debug("Cleaning up dead connection for user: {}", email);
                emitters.remove(email);
            }
        } else {
            log.debug("No active SSE connection for user: {}", email);
        }
    }

    public void broadcastToAdmins(TicketResponse ticket) {
        List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
        Set<String> adminEmails = admins.stream().map(User::getEmail).collect(Collectors.toSet());
        
        emitters.forEach((email, emitter) -> {
            if (adminEmails.contains(email)) {
                System.out.println("DEBUG: Pushing to " + email);
                try {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(ticket));
                    log.info("Pushed notification to admin: {}", email);
                } catch (IOException e) {
                    log.debug("Cleaning up dead connection for admin: {}", email);
                    emitters.remove(email);
                }
            }
        });
    }

    @Scheduled(fixedRate = 15000) // Every 15 seconds
    public void sendHeartbeat() {
        emitters.forEach((email, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("PING").data("Heartbeat"));
            } catch (IOException e) {
                log.debug("Cleaning up dead connection for user: {}", email);
                emitters.remove(email);
            } catch (Exception e) {
                log.debug("Cleaning up dead connection for user: {}", email);
                emitters.remove(email);
            }
        });
    }
}
