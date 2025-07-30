package com.github.sdms.service;

import com.github.sdms.model.User;

import java.util.Optional;

public interface UserService {

    Optional<User> findByEmailAndLibraryCode(String email, String libraryCode);

    Optional<User> findByUsernameAndLibraryCode(String username, String libraryCode);

    boolean existsByEmailAndLibraryCode(String email, String libraryCode);

    Optional<User> findByUidAndLibraryCode(String uid, String libraryCode);

    boolean existsByUidAndLibraryCode(String uid, String libraryCode);

    Optional<User> findByMobileAndLibraryCode(String mobile, String libraryCode);

    boolean existsByMobileAndLibraryCode(String mobile, String libraryCode);

    Optional<User> findByUsernameOrEmailAndLibraryCode(String username, String email, String libraryCode);

    User saveUser(User user);

    void deleteUser(Long id);
}
