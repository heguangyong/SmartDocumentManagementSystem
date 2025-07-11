package com.github.sdms.service;

import com.github.sdms.model.AppUser;

import java.util.Optional;

public interface UserService {

    Optional<AppUser> findByEmailAndLibraryCode(String email, String libraryCode);

    Optional<AppUser> findByUsernameAndLibraryCode(String username, String libraryCode);

    boolean existsByEmailAndLibraryCode(String email, String libraryCode);

    Optional<AppUser> findByUidAndLibraryCode(String uid, String libraryCode);

    boolean existsByUidAndLibraryCode(String uid, String libraryCode);

    Optional<AppUser> findByMobileAndLibraryCode(String mobile, String libraryCode);

    boolean existsByMobileAndLibraryCode(String mobile, String libraryCode);

    Optional<AppUser> findByUsernameOrEmailAndLibraryCode(String username, String email, String libraryCode);

    AppUser saveUser(AppUser user);

    void deleteUser(Long id);
}
