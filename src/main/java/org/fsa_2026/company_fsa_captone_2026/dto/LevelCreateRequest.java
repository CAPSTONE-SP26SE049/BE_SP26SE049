package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelCreateRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "Level 1")
    private String name;

    @NotBlank(message = "Type is required")
    @Schema(example = "LEVEL")
    private String type;

    @NotNull(message = "Parent ID is required")
    @JsonProperty("parent_id")
    @Schema(example = "00000000-0000-0000-0001-000000000001")
    private UUID parentId;

    @NotNull(message = "Metadata JSON is required")
    @JsonProperty("metadata_json")
    @Schema(example = "{\"status\":\"APPROVED\",\"audio_url\":null,\"level_order\":2,\"ai_threshold\":75,\"error_tag\":\"L/N\",\"rejection_reason\":null,\"min_stars_required\":3}")
    private Map<String, Object> metadataJson;

    @JsonProperty("metadataJson")
    public void setMetadataJsonCamelCase(Map<String, Object> metadataJson) {
        this.metadataJson = metadataJson;
    }

    @Schema(example = "Educator updated content description")
    private String comment;
}
