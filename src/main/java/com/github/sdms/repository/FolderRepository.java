package com.github.sdms.repository;

import com.github.sdms.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    // 查询用户根目录
    List<Folder> findByUidAndParentIdIsNullAndLibraryCode(String uid, String libraryCode);

    // 查询用户某个父目录下的子目录
    List<Folder> findByUidAndParentIdAndLibraryCode(String uid, Long parentId, String libraryCode);

    // 查询指定 ID 的目录，且属于指定用户
    Folder findByIdAndUidAndLibraryCode(Long id, String uid, String libraryCode);

    // 获取用户所有目录（可用于构建目录树）
    List<Folder> findByUidAndLibraryCode(String uid, String libraryCode);

    // 根据分享链接查询目录
    Optional<Folder> findByShareTokenAndLibraryCode(String token, String libraryCode);
}
