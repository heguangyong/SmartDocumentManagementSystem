package com.github.sdms.repository;

import com.github.sdms.model.Bucket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BucketRepository extends JpaRepository<Bucket, Long> {
    boolean existsByName(String name);

    Optional<Bucket> findByName(String name);

    List<Bucket> findByOwnerUid(String uid);
}
