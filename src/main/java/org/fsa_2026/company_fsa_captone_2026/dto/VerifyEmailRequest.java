package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Verify Email Request DTO
 * POST /api/v1/auth/verify-email
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyEmailRequest implements Serializable {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Mã xác nhận không được để trống")
    @Size(min = 6, max = 6, message = "Mã xác nhận phải gồm 6 ký tự")
    private String code;
}

