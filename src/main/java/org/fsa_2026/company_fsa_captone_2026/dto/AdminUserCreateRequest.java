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
 * Request DTO — POST /api/v1/admin/users
 * Fix A-01/A-02: validate email và fullName trước khi vào service (tránh NPE).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserCreateRequest implements Serializable {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 50, message = "Email không quá 50 ký tự")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 3, max = 50, message = "Tên phải từ 3 đến 50 ký tự")
    private String fullName;

    /** USER hoặc EDUCATOR; mặc định USER nếu null/blank */
    private String role;
}
