package com.github.sdms.repository;

import com.github.sdms.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    // 查询用户根目录
    List<Folder> findByUidAndParentIdIsNull(String uid);

    // 查询用户某个父目录下的子目录
    List<Folder> findByUidAndParentId(String uid, Long parentId);

    // 查询指定 ID 的目录，且属于指定用户
    Folder findByIdAndUid(Long id, String uid);

    // 获取用户所有目录（可用于构建目录树）
    List<Folder> findByUid(String uid);

    Optional<Folder> findByShareToken(String token);

}
