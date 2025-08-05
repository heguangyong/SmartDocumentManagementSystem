package com.github.sdms.repository;

import com.github.sdms.model.Bucket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BucketRepository extends JpaRepository<Bucket, Long> {
    boolean existsByName(String name);

    Optional<Bucket> findByName(String name);

    List<Bucket> findByOwnerUid(String uid);

    Optional<Bucket> findFirstByOwnerUidAndLibraryCode(String uid, String libraryCode);

    @Query("SELECT b FROM Bucket b WHERE " +
            "(:keyword IS NULL OR b.name LIKE %:keyword% OR b.ownerUid LIKE %:keyword%)")
    Page<Bucket> findByNameOrOwnerUidLike(@Param("keyword") String keyword, Pageable pageable);

}
