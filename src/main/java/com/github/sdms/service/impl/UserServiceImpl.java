package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.UserService;
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
        Optional<AppUser> user = userRepository.findByEmailAndLibraryCode(email, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: email=" + email + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public Optional<AppUser> findByUsernameAndLibraryCode(String username, String libraryCode) {
        Optional<AppUser> user = userRepository.findByUsernameAndLibraryCode(username, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: username=" + username + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public boolean existsByEmailAndLibraryCode(String email, String libraryCode) {
        return userRepository.existsByEmailAndLibraryCode(email, libraryCode);
    }

    @Override
    public Optional<AppUser> findByUidAndLibraryCode(String uid, String libraryCode) {
        Optional<AppUser> user = userRepository.findByUidAndLibraryCode(uid, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: uid=" + uid + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public boolean existsByUidAndLibraryCode(String uid, String libraryCode) {
        return userRepository.existsByUidAndLibraryCode(uid, libraryCode);
    }

    @Override
    public Optional<AppUser> findByMobileAndLibraryCode(String mobile, String libraryCode) {
        Optional<AppUser> user = userRepository.findByMobileAndLibraryCode(mobile, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: mobile=" + mobile + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public boolean existsByMobileAndLibraryCode(String mobile, String libraryCode) {
        return userRepository.existsByMobileAndLibraryCode(mobile, libraryCode);
    }

    @Override
    public Optional<AppUser> findByUsernameOrEmailAndLibraryCode(String username, String email, String libraryCode) {
        Optional<AppUser> user = userRepository.findByUsernameOrEmailAndLibraryCode(username, email, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: username=" + username + ", email=" + email + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public AppUser saveUser(AppUser user) {
        if (user == null) {
            throw new ApiException(400, "保存的用户对象不能为空");
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException(404, "删除失败，用户ID不存在: " + id);
        }
        userRepository.deleteById(id);
    }
}
