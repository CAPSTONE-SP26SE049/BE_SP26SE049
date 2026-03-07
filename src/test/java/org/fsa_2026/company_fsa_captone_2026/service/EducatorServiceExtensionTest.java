package org.fsa_2026.company_fsa_captone_2026.service;

import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.PlacementRule;
import org.fsa_2026.company_fsa_captone_2026.repository.PlacementRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EducatorServiceExtensionTest {

    @Mock
    private PlacementRuleRepository placementRuleRepository;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.ErrorTagRepository errorTagRepository;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository dialectRepository;

    @InjectMocks
    private EducatorService educatorService;

    @Test
    public void testUpdateOrCreatePlacementRule() {
        java.util.UUID errorTagId = java.util.UUID.randomUUID();
        java.util.UUID dialectId = java.util.UUID.randomUUID();

        PlacementRuleRequest request = PlacementRuleRequest.builder()
                .errorTagId(errorTagId)
                .threshold(30)
                .dialectId(dialectId)
                .checkpoint("level-1")
                .priority(1)
                .build();

        org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag mockErrorTag = new org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag();
        mockErrorTag.setId(errorTagId);
        mockErrorTag.setTagCode("L_N");

        org.fsa_2026.company_fsa_captone_2026.entity.Dialect mockDialect = new org.fsa_2026.company_fsa_captone_2026.entity.Dialect();
        mockDialect.setId(dialectId);
        mockDialect.setName("Northern");

        when(errorTagRepository.findById(errorTagId)).thenReturn(java.util.Optional.of(mockErrorTag));
        when(dialectRepository.findById(dialectId)).thenReturn(java.util.Optional.of(mockDialect));
        when(placementRuleRepository.findAll()).thenReturn(new ArrayList<>());
        when(placementRuleRepository.save(any(PlacementRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlacementRule result = educatorService.updateOrCreatePlacementRule(request);

        assertNotNull(result);
        assertEquals(errorTagId, result.getErrorTag().getId());
        assertEquals(30, result.getThreshold());
        assertEquals(dialectId, result.getTargetDialect().getId());
    }
}
