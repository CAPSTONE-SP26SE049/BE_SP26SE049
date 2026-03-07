package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Register Response DTO
 * Returned after successful registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse implements Serializable {

    private String id;
    private String email;
    private String fullName;
    private String role;
}

