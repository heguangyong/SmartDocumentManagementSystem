package com.github.sdms.repository;

import com.github.sdms.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long>, JpaSpecificationExecutor<Folder> {

    // 查询用户根目录
    List<Folder> findByUserIdAndParentIdIsNullAndLibraryCode(Long userId, String libraryCode);

    // 查询用户某个父目录下的子目录
    List<Folder> findByUserIdAndParentIdAndLibraryCode(Long userId, Long parentId, String libraryCode);

    // 查询指定 ID 的目录，且属于指定用户
    Folder findByIdAndUserIdAndLibraryCode(Long id, Long userId, String libraryCode);

    // 获取用户所有目录（可用于构建目录树）
    List<Folder> findByUserIdAndLibraryCode(Long userId, String libraryCode);

    // 修改这里，userId 就是 ownerId
    List<Folder> findByParentIdAndUserIdAndLibraryCode(Long parentId, Long userId, String libraryCode);

    List<Folder> findByParentId(Long parentId);

    /**
     * 根据用户ID、桶ID和库代码查找根级文件夹
     */
    List<Folder> findByUserIdAndBucketIdAndParentIdIsNullAndLibraryCode(Long userId, Long bucketId, String libraryCode);

    /**
     * 根据用户ID、桶ID和库代码查找所有文件夹
     */
    List<Folder> findByUserIdAndBucketIdAndLibraryCode(Long userId, Long bucketId, String libraryCode);
}

