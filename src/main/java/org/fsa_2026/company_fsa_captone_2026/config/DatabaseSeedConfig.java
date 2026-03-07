package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Dialect;
import org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Database Seed Configuration
 * ISO Reference Tables: Language (ISO 639-3), Country (ISO 3166-1), Dialect
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeedConfig {

        /**
         * Database Seed - DISABLED after initial setup
         * Seeding has already been completed on first run
         * To re-seed, uncomment @Bean and @Order(1), then restart
         */
        // @Bean
        // @Order(1)
        public CommandLineRunner seedIsoReferenceTables(
                        DialectRepository dialectRepository) {
                return args -> {
                        log.info("Seeding Dialect reference table...");

                        // Seed Dialects
                        if (dialectRepository.count() == 0) {
                                log.info("Seeding Dialect table...");

                                // Vietnamese dialects
                                dialectRepository.save(Dialect.builder()
                                                .name("Northern (Hanoi)")
                                                .description("Standard Northern Vietnamese accent").build());

                                dialectRepository.save(Dialect.builder()
                                                .name("Central (Hue)")
                                                .description("Central Vietnamese accent").build());

                                dialectRepository.save(Dialect.builder()
                                                .name("Southern (Saigon)")
                                                .description("Southern Vietnamese accent").build());

                                log.info("Seeded {} dialects", dialectRepository.count());
                        }

                        log.info("Database seeding completed!");
                };
        }
}
