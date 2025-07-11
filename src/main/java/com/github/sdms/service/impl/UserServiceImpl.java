package com.github.sdms.service.impl;

import com.github.sdms.service.UserService;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<AppUser> findByEmailAndLibraryCode(String email, String libraryCode) {
        return userRepository.findByEmailAndLibraryCode(email, libraryCode);
    }

    @Override
    public Optional<AppUser> findByUsernameAndLibraryCode(String username, String libraryCode) {
        return userRepository.findByUsernameAndLibraryCode(username, libraryCode);
    }

    @Override
    public boolean existsByEmailAndLibraryCode(String email, String libraryCode) {
        return userRepository.existsByEmailAndLibraryCode(email, libraryCode);
    }

    @Override
    public Optional<AppUser> findByUidAndLibraryCode(String uid, String libraryCode) {
        return userRepository.findByUidAndLibraryCode(uid, libraryCode);
    }

    @Override
    public boolean existsByUidAndLibraryCode(String uid, String libraryCode) {
        return userRepository.existsByUidAndLibraryCode(uid, libraryCode);
    }

    @Override
    public Optional<AppUser> findByMobileAndLibraryCode(String mobile, String libraryCode) {
        return userRepository.findByMobileAndLibraryCode(mobile, libraryCode);
    }

    @Override
    public boolean existsByMobileAndLibraryCode(String mobile, String libraryCode) {
        return userRepository.existsByMobileAndLibraryCode(mobile, libraryCode);
    }

    @Override
    public Optional<AppUser> findByUsernameOrEmailAndLibraryCode(String username, String email, String libraryCode) {
        return userRepository.findByUsernameOrEmailAndLibraryCode(username, email, libraryCode);
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
