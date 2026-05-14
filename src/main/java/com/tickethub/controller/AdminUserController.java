package com.tickethub.controller;

import com.tickethub.dto.request.RegisterRequest;
import com.tickethub.dto.response.UserSummaryDTO;
import com.tickethub.model.User;
import com.tickethub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserSummaryDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<UserSummaryDTO>> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Void> createUserByAdmin(@Valid @RequestBody RegisterRequest request) {
        userService.createUserByAdmin(request);
        return ResponseEntity.ok().build();
    }
}
