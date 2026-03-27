package org.fsa_2026.company_fsa_captone_2026.dto.response.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineItemDto {
    private LocalDate date;
    private String status; // completed | in_progress | upcoming
    private String title;
    private String tooltipDetails;
    private boolean locked;
}
