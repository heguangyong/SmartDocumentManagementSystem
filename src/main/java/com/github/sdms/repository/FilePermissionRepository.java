package com.github.sdms.repository;

import com.github.sdms.model.User;
import com.github.sdms.model.FilePermission;
import com.github.sdms.model.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {

    FilePermission findByUserAndFile(User user, UserFile file);

    List<FilePermission> findByUser(User user);

    List<FilePermission> findByFile(UserFile file);
}
