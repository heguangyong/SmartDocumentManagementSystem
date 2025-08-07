package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.LibrarySiteRepository;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.UserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    private LibrarySiteRepository librarySiteRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public Optional<User> findByUsernameAndLibraryCode(String username, String libraryCode) {
        Optional<User> user = userRepository.findByUsernameAndLibraryCode(username, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: username=" + username + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public Optional<User> findByUidAndLibraryCode(String uid, String libraryCode) {
        Optional<User> user = userRepository.findByUidAndLibraryCode(uid, libraryCode);
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
    public Optional<User> findByMobileAndLibraryCode(String mobile, String libraryCode) {
        Optional<User> user = userRepository.findByMobileAndLibraryCode(mobile, libraryCode);
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
    public Optional<User> findByUsernameOrEmailAndLibraryCode(String username, String email, String libraryCode) {
        Optional<User> user = userRepository.findByUsernameOrEmailAndLibraryCode(username, email, libraryCode);
        if (user.isEmpty()) {
            throw new ApiException(404, "用户不存在: username=" + username + ", email=" + email + ", libraryCode=" + libraryCode);
        }
        return user;
    }

    @Override
    public User saveUser(User user) {
        if (user == null) {
            throw new ApiException(400, "保存的用户对象不能为空");
        }

        // 校验绑定的馆点代码是否合法（必须存在且状态为启用）
        if (!isValidLibraryCode(user.getLibraryCode())) {
            throw new ApiException(400, "无效的馆点代码: " + user.getLibraryCode());
        }

        return userRepository.save(user);
    }

    private boolean isValidLibraryCode(String code) {
        return librarySiteRepository.existsByCodeAndStatusTrue(code);
    }


    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException(404, "删除失败，用户ID不存在: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Page<User> findUsersByCriteria(String username, RoleType roleType, String libraryCode, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (roleType != null) {
                predicates.add(cb.equal(root.get("roleType"), roleType));
            }
            if (libraryCode != null && !libraryCode.isEmpty()) {
                predicates.add(cb.equal(root.get("libraryCode"), libraryCode));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable);
    }
}
