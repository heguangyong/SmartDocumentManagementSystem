package com.github.sdms.service;

import com.github.sdms.dto.UserResourcePermissionDTO;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<User> findByUsernameAndLibraryCode(String username, String libraryCode);

    Optional<User> findByUidAndLibraryCode(String uid, String libraryCode);

    boolean existsByUidAndLibraryCode(String uid, String libraryCode);

    Optional<User> findByMobileAndLibraryCode(String mobile, String libraryCode);

    boolean existsByMobileAndLibraryCode(String mobile, String libraryCode);

    Optional<User> findByUsernameOrEmailAndLibraryCode(String username, String email, String libraryCode);

    User saveUser(User user);

    void deleteUser(Long id);

    Page<User> findUsersByCriteria(String username, RoleType roleType, String libraryCode, Pageable pageable);

    List<UserResourcePermissionDTO> getUserPermissions(Long userId);

    void updateUserPermissions(Long userId, List<UserResourcePermissionDTO> permissions);
}
