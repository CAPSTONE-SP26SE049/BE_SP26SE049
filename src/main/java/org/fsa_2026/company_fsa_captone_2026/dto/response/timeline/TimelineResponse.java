package org.fsa_2026.company_fsa_captone_2026.dto.response.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineResponse {
    private UUID accountId;
    private LocalDate todayDate;
    private CurrentProgressDto currentProgress;
    private List<TimelineItemDto> timelineItems;
}
