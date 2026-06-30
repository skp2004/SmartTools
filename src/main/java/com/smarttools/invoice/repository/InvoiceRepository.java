package com.smarttools.invoice.repository;

import com.smarttools.invoice.entity.Invoice;
import com.smarttools.invoice.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByCompanyId(Long companyId);

    Optional<Invoice> findByIdAndCompanyId(Long id, Long companyId);

    List<Invoice> findAllByCompanyIdAndStatus(Long companyId, InvoiceStatus status);

    /**
     * Find the highest invoice number for a company to auto-generate the next one.
     * Returns the count of existing invoices — the next number will be count + 1.
     */
    long countByCompanyId(Long companyId);

    /**
     * Count invoices created this month for plan-limit enforcement.
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.company.id = :companyId AND i.createdAt >= :startOfMonth")
    long countByCompanyIdAndCreatedAtAfter(@Param("companyId") Long companyId,
                                           @Param("startOfMonth") java.time.OffsetDateTime startOfMonth);

    /**
     * Find overdue invoices for the scheduled status-flip job.
     */
    List<Invoice> findAllByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);
}
