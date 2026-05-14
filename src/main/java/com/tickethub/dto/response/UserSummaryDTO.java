package com.tickethub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private Long id;
    private String email;
    private String fullName;
    private String nom;
    private String prenom;
    private String tel;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
