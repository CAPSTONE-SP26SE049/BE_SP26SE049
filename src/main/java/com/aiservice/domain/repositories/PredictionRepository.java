package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.Prediction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByUserId(Long userId, Pageable pageable);
}
