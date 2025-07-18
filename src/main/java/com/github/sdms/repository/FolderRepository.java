package com.github.sdms.repository;

import com.github.sdms.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUidAndParentIdIsNullAndLibraryCode(String uid, String libraryCode);

    List<Folder> findByUidAndParentIdAndLibraryCode(String uid, Long parentId, String libraryCode);

    Folder findByIdAndUidAndLibraryCode(Long id, String uid, String libraryCode);

    List<Folder> findByUidAndLibraryCode(String uid, String libraryCode);

    List<Folder> findByParentIdAndUidAndLibraryCode(Long parentId, String uid, String libraryCode);
}
