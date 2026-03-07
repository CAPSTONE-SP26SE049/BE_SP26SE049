package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.ClassroomMember;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomMemberResponse implements Serializable {

    private String classroomId;
    private String accountId;
    private String joinedAt;

    // We can embed the user's name/avatar for convenience but keeping it simple for
    // now

    public static ClassroomMemberResponse fromEntity(ClassroomMember member) {
        if (member == null)
            return null;
        return ClassroomMemberResponse.builder()
                .classroomId(member.getClassroom() != null ? member.getClassroom().getId().toString() : null)
                .accountId(member.getStudent() != null ? member.getStudent().getId().toString() : null)
                .joinedAt(member.getJoinedAt() != null ? member.getJoinedAt().toString() : null)
                .build();
    }
}
