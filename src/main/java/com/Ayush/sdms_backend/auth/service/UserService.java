package com.Ayush.sdms_backend.auth.service;

import com.Ayush.sdms_backend.model.AppUser;

import java.util.Optional;

public interface UserService {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByUid(String uid);

    boolean existsByUid(String uid);

    Optional<AppUser> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    Optional<AppUser> findByUsernameOrEmail(String username, String email);

    AppUser saveUser(AppUser user);

    void deleteUser(Long id);

}
