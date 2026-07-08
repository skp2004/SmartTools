package com.smarttools.invoice.repository;

import com.smarttools.invoice.entity.Usage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsageRepository extends JpaRepository<Usage, Long> {
    Optional<Usage> findByUserIdAndToolAndDate(Long userId, String tool, LocalDate date);
}
