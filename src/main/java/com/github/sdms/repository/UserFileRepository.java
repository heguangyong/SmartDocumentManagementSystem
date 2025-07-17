package com.github.sdms.repository;

import com.github.sdms.model.UserFile;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {

    // 根据用户ID和deleteFlag查询文件列表
    List<UserFile> findByUidAndDeleteFlagFalseAndLibraryCode(String uid, String libraryCode);

    // 批量删除文件
    void deleteByUidAndNameInAndLibraryCode(String uid, List<String> names, String libraryCode);

    // 查询被标记为删除的文件
    List<UserFile> findByUidAndDeleteFlagTrueAndCreatedDateAfterAndLibraryCode(String uid, Date after, String libraryCode);

    // 根据UID和文件名查询文件
    UserFile findByUidAndNameAndLibraryCode(String uid, String name, String libraryCode);

    // 查询被删除且创建时间在指定日期之前的文件
    List<UserFile> findByDeleteFlagTrueAndCreatedDateBeforeAndLibraryCode(Date before, String libraryCode);

    // 根据UID和文件夹ID查询文件列表
    List<UserFile> findByUidAndFolderIdAndDeleteFlagFalseAndLibraryCode(String uid, Long folderId, String libraryCode);

    // 根据文件ID查询文件
    Optional<UserFile> findByIdAndDeleteFlagFalseAndLibraryCode(Long fileId, String libraryCode);

    //返回某个文档 ID 在指定馆下的所有版本
    List<UserFile> findByDocIdAndLibraryCodeOrderByVersionNumberDesc(Long docId, String libraryCode);

    Optional<UserFile> findByNameAndLibraryCode(String name, String libraryCode);

    Optional<UserFile> findFirstByDocIdAndUidAndLibraryCodeAndIsLatestTrueAndDeleteFlagFalse(Long docId, String uid, String libraryCode);

    Optional<UserFile> findFirstByDocIdAndUidAndIsLatestTrue(Long docId, String uid);

    Optional<UserFile> findByUidAndOriginFilenameAndDeleteFlagFalse(String uid, String originFilename);


    @Modifying
    @Transactional
    @Query("UPDATE UserFile uf SET uf.isLatest = false WHERE uf.docId = :docId AND uf.isLatest = true AND uf.libraryCode = :libraryCode")
    int markAllOldVersionsNotLatest(@Param("docId") Long docId, @Param("libraryCode") String libraryCode);

}
