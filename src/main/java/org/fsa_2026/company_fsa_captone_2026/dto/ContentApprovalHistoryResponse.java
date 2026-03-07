package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentApprovalHistory;

import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonRawValue;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentApprovalHistoryResponse implements Serializable {

    private String id;
    private String contentType;
    private String contentId;
    private String status;
    private String comment;
    @JsonRawValue
    private String contentSnapshot;
    private Instant createdAt;
    private String createdBy;

    public static ContentApprovalHistoryResponse fromEntity(ContentApprovalHistory history) {
        if (history == null)
            return null;
        return ContentApprovalHistoryResponse.builder()
                .id(history.getId().toString())
                .contentType(history.getContentType())
                .contentId(history.getContentId().toString())
                .status(history.getStatus().name())
                .comment(history.getComment())
                .contentSnapshot(history.getContentSnapshot())
                .createdAt(history.getCreatedAt())
                .createdBy(history.getCreatedBy())
                .build();
    }
}
