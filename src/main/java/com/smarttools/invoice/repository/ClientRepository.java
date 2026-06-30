package com.smarttools.invoice.repository;

import com.smarttools.invoice.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByCompanyId(Long companyId);

    Optional<Client> findByIdAndCompanyId(Long id, Long companyId);

    void deleteByIdAndCompanyId(Long id, Long companyId);
}
