package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.SystemConfig;
import org.fsa_2026.company_fsa_captone_2026.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Transactional(readOnly = true)
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(key);
            if (configOpt.isPresent()) {
                String val = configOpt.get().getConfigValue();
                if (val != null && !val.isBlank()) {
                    return val.trim();
                }
            }
        } catch (Exception e) {
            log.warn("Error looking up system config for key '{}': {}", key, e.getMessage());
        }
        return defaultValue;
    }

    @Transactional
    public void updateConfigs(Map<String, String> configMap) {
        if (configMap == null || configMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key == null || key.isBlank()) {
                continue;
            }

            Optional<SystemConfig> existingOpt = systemConfigRepository.findByConfigKey(key);
            if (existingOpt.isPresent()) {
                SystemConfig existing = existingOpt.get();
                existing.setConfigValue(value != null ? value : "");
                systemConfigRepository.save(existing);
                log.info("Updated system config for key '{}'", key);
            } else {
                SystemConfig newConfig = SystemConfig.builder()
                        .configKey(key)
                        .configValue(value != null ? value : "")
                        .description("Được tạo động qua trang cấu hình hệ thống")
                        .build();
                systemConfigRepository.save(newConfig);
                log.info("Created system config for key '{}'", key);
            }
        }
    }
}
