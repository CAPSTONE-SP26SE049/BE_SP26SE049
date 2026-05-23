package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * User Profile Update Request DTO
 * PUT /api/v1/users/me
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequest implements Serializable {

    private String fullName;
    private String phone;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private String region;

}
