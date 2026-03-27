package org.fsa_2026.company_fsa_captone_2026.dto.response.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentProgressDto {
    private String mienDangHoc;
    private String chuongHienTai;
    private Integer phanTramHoanThanh;
}
