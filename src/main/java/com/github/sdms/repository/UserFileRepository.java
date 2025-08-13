package com.github.sdms.repository;

import com.github.sdms.model.UserFile;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> , JpaSpecificationExecutor<UserFile> {

    // 根据用户ID和deleteFlag查询文件列表
    List<UserFile> findByUserIdAndDeleteFlagFalseAndLibraryCode(Long userId, String libraryCode);

    // 批量删除文件
    void deleteByUserIdAndNameInAndLibraryCode(Long userId, List<String> names, String libraryCode);

    // 查询被标记为删除的文件
    List<UserFile> findByUserIdAndDeleteFlagTrueAndCreatedDateAfterAndLibraryCode(Long userId, Date after, String libraryCode);

    // 根据UserId和文件名查询文件
    UserFile findByUserIdAndNameAndLibraryCode(Long userId, String name, String libraryCode);


    // 查询被删除且创建时间在指定日期之前的文件
    List<UserFile> findByDeleteFlagTrueAndCreatedDateBeforeAndLibraryCode(Date before, String libraryCode);

    // 根据UserId和文件夹ID查询文件列表
    List<UserFile> findByUserIdAndFolderIdAndDeleteFlagFalseAndLibraryCode(Long userId, Long folderId, String libraryCode);

    // 根据文件ID查询文件
    Optional<UserFile> findByIdAndDeleteFlagFalseAndLibraryCode(Long fileId, String libraryCode);

    //返回某个文档 ID 在指定馆下的所有版本
    List<UserFile> findByDocIdAndLibraryCodeOrderByVersionNumberDesc(Long docId, String libraryCode);

    Optional<UserFile> findByNameAndLibraryCode(String name, String libraryCode);

    Optional<UserFile> findFirstByDocIdAndUserIdAndLibraryCodeAndIsLatestTrueAndDeleteFlagFalse(Long docId, Long userId, String libraryCode);

    Optional<UserFile> findFirstByDocIdAndUserIdAndIsLatestTrue(Long docId, Long userId);

    Optional<UserFile> findByUserIdAndOriginFilenameAndDeleteFlagFalse(Long userId, String originFilename);


    @Modifying
    @Transactional
    @Query("UPDATE UserFile uf SET uf.isLatest = false WHERE uf.docId = :docId AND uf.isLatest = true AND uf.libraryCode = :libraryCode")
    int markAllOldVersionsNotLatest(@Param("docId") Long docId, @Param("libraryCode") String libraryCode);

    List<UserFile> findByFolderIdAndUserIdAndLibraryCode(Long id, Long ownerId, String libraryCode);

    List<UserFile> findByBucketInAndDeleteFlagFalse(Set<String> bucketNames);

    List<UserFile> findByDeleteFlagFalse();

    List<UserFile> findByLibraryCodeAndDeleteFlagFalse(String libraryCode);

    List<UserFile> findByUserIdAndLibraryCodeAndDeleteFlagFalse(Long userId, String libraryCode);

    @Query("SELECT MAX(f.versionNumber) FROM UserFile f WHERE f.originFilename = :originFilename AND f.folderId = :folderId AND f.userId = :userId AND f.libraryCode = :libraryCode AND f.deleteFlag = false")
    Integer findMaxVersionNumber(@Param("originFilename") String originFilename,
                                 @Param("folderId") Long folderId,
                                 @Param("userId") Long userId,
                                 @Param("libraryCode") String libraryCode);

    @Modifying
    @Query("UPDATE UserFile f SET f.isLatest = false WHERE f.originFilename = :originFilename AND f.folderId = :folderId AND f.userId = :userId AND f.libraryCode = :libraryCode AND f.deleteFlag = false")
    void markOldVersionsNotLatest(@Param("originFilename") String originFilename,
                                  @Param("folderId") Long folderId,
                                  @Param("userId") Long userId,
                                  @Param("libraryCode") String libraryCode);

    List<UserFile> findByDocIdAndUserIdOrderByVersionNumberDesc(Long docId, Long userId);

    @Query("SELECT MAX(u.versionNumber) FROM UserFile u WHERE u.docId = :docId")
    Integer findMaxVersionByDocId(@Param("docId") Long docId);

    @Modifying
    @Query("UPDATE UserFile u SET u.isLatest = false WHERE u.docId = :docId AND u.isLatest = true")
    void clearLatestVersionFlag(@Param("docId") Long docId);

    Optional<UserFile> findByOriginFilenameAndUserIdAndLibraryCodeAndDeleteFlagFalse(String filename, Long userId, String libraryCode);

    Optional<UserFile> findByIdAndDeleteFlagFalse(Long fileId);
}
