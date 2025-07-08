package com.Ayush.sdms_backend.auth.service.impl;

import com.Ayush.sdms_backend.auth.service.UserService;
import com.Ayush.sdms_backend.model.AppUser;
import com.Ayush.sdms_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<AppUser> findByUid(String uid) {
        return userRepository.findByUid(uid);
    }

    @Override
    public boolean existsByUid(String uid) {
        return userRepository.existsByUid(uid);
    }

    @Override
    public Optional<AppUser> findByMobile(String mobile) {
        return userRepository.findByMobile(mobile);
    }

    @Override
    public boolean existsByMobile(String mobile) {
        return userRepository.existsByMobile(mobile);
    }

    @Override
    public Optional<AppUser> findByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username, email);
    }

    @Override
    public AppUser saveUser(AppUser user) {
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
