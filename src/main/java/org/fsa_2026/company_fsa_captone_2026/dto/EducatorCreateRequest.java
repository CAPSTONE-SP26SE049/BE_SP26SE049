package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;

/**
 * Educator Create Request DTO
 * POST /api/v1/admin/educators
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducatorCreateRequest implements Serializable {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 50, message = "Email không quá 50 ký tự")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 3, max = 50, message = "Tên phải từ 3 đến 50 ký tự")
    private String fullName;
}

