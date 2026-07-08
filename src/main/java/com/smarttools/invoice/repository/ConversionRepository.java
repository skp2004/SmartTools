package com.smarttools.invoice.repository;

import com.smarttools.invoice.entity.Conversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {
    Page<Conversion> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Conversion> findByUserIdOrderByCreatedAtDesc(Long userId);
}
