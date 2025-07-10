package com.github.sdms.repository;

import com.github.sdms.model.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    List<UserFile> findByUidAndDeleteFlagFalse(String uid);

    void deleteByUidAndNameIn(String uid, List<String> names);

    List<UserFile> findByUidAndDeleteFlagTrueAndCreatedDateAfter(String uid, Date after);

    UserFile findByUidAndName(String uid, String name);

    List<UserFile> findByDeleteFlagTrueAndCreatedDateBefore(Date before);

    List<UserFile> findByOwnerIdAndFolderIdAndDeletedFalse(String ownerId, Long folderId);

    Optional<UserFile> findByIdAndDeleteFlagFalse(Long fileId);

}