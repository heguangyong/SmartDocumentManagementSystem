package com.github.sdms.repository;

import com.github.sdms.model.AppUser;
import com.github.sdms.model.FilePermission;
import com.github.sdms.model.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {

    FilePermission findByUserAndFile(AppUser user, UserFile file);

    List<FilePermission> findByUser(AppUser user);

    List<FilePermission> findByFile(UserFile file);
}
