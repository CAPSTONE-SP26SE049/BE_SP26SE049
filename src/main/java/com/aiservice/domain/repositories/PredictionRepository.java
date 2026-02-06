package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    Page<Prediction> findByUserId(Long userId, Pageable pageable);
}
