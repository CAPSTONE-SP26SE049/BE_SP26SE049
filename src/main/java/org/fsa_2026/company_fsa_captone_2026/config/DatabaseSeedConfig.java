package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Database Seed Configuration
 * Đã cập nhật: dùng LearningUnit (type=DIALECT) thay cho Dialect
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeedConfig {

    /**
     * Database Seed - DISABLED after initial setup
     * To re-seed: uncomment @Bean and @Order(1), then restart
     */
    // @Bean
    // @Order(1)
    public CommandLineRunner seedIsoReferenceTables(LearningUnitRepository learningUnitRepository) {
        return args -> {
            log.info("Seeding LearningUnit (DIALECT) reference table...");

            boolean hasDialects = !learningUnitRepository.findByType("DIALECT").isEmpty();
            if (!hasDialects) {
                log.info("Seeding Dialect entries into learning_unit table...");

                learningUnitRepository.save(LearningUnit.builder()
                        .name("Northern (Hanoi)")
                        .type("DIALECT")
                        .metadataJson("{\"description\":\"Standard Northern Vietnamese accent\"}")
                        .build());

                learningUnitRepository.save(LearningUnit.builder()
                        .name("Central (Hue)")
                        .type("DIALECT")
                        .metadataJson("{\"description\":\"Central Vietnamese accent\"}")
                        .build());

                learningUnitRepository.save(LearningUnit.builder()
                        .name("Southern (Saigon)")
                        .type("DIALECT")
                        .metadataJson("{\"description\":\"Southern Vietnamese accent\"}")
                        .build());

                log.info("Seeded 3 dialects into learning_unit.");
            }

            log.info("Database seeding completed!");
        };
    }
}
