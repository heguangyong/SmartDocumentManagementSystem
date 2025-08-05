package com.github.sdms.repository;

import com.github.sdms.model.ShareAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareAccessRepository extends JpaRepository<ShareAccess, Long> {
    Optional<ShareAccess> findByTokenHash(String tokenHash);

    Optional<ShareAccess> findByTokenAndLibraryCode(String token, String libraryCode);

    List<ShareAccess> findByOwnerIdAndTargetTypeAndLibraryCodeAndEnabledTrue(
            Long ownerId, String targetType, String libraryCode);
}
