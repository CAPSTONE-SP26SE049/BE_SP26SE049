package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;

/**
 * Admin Create Request DTO
 * POST /api/v1/auth/create-admin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCreateRequest implements Serializable {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$", message = "Mật khẩu phải chứa ít nhất một chữ hoa và một ký tự đặc biệt")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
}
