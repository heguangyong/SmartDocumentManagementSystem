package com.github.sdms.repository;

import com.github.sdms.model.LibrarySite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface LibrarySiteRepository extends JpaRepository<LibrarySite, Long>, JpaSpecificationExecutor<LibrarySite> {

    Optional<LibrarySite> findByCode(String code);

    boolean existsByCode(String code);

    List<LibrarySite> findAllByStatusTrue(Sort sortOrder);

    Page<LibrarySite> findByStatusTrueAndNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<LibrarySite> findByStatusTrue(Pageable pageable);

    boolean existsByCodeAndStatusTrue(String code);
}

