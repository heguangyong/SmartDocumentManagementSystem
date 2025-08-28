package com.github.sdms.service.impl;

import com.github.sdms.dto.UserResourcePermissionDTO;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.*;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.*;
import com.github.sdms.service.UserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    private BucketPermissionRepository bucketPermissionRepository;

    @Autowired
    private FilePermissionRepository filePermissionRepository;

    @Autowired
    private LibrarySiteRepository librarySiteRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private UserFileRepository userFileRepository;

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

    @Override
    public List<UserResourcePermissionDTO> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        List<UserResourcePermissionDTO> result = new ArrayList<>();

        // 1. 个性化桶权限
        List<BucketPermission> bucketPerms = bucketPermissionRepository.findByUserId(userId);
        Map<Long, UserResourcePermissionDTO> customBucketMap = new HashMap<>();
        for (BucketPermission bp : bucketPerms) {
            UserResourcePermissionDTO dto = UserResourcePermissionDTO.builder()
                    .resourceId(bp.getBucketId())
                    .resourceType("BUCKET")
                    .resourceName(bp.getBucket().getName())
                    .canRead(bp.getPermission().toLowerCase().contains("read"))
                    .canWrite(bp.getPermission().toLowerCase().contains("write"))
                    .canDelete(bp.getPermission().toLowerCase().contains("delete"))
                    .permissionSource("CUSTOM")
                    .build();
            result.add(dto);
            customBucketMap.put(bp.getBucketId(), dto);
        }

        // 2. 个性化文件权限
        List<FilePermission> filePerms = filePermissionRepository.findByUser(user);
        for (FilePermission fp : filePerms) {
            result.add(UserResourcePermissionDTO.builder()
                    .resourceId(fp.getFile().getId())
                    .resourceType("FILE")
                    .resourceName(fp.getFile().getOriginFilename())
                    .canRead(fp.getPermission().toLowerCase().contains("read"))
                    .canWrite(fp.getPermission().toLowerCase().contains("write"))
                    .canDelete(fp.getPermission().toLowerCase().contains("delete"))
                    .permissionSource("CUSTOM")
                    .build());
        }

        // 3. 角色权限
        List<UserResourcePermissionDTO> rolePermissions = fetchRoleInheritedPermissions(user);
        for (UserResourcePermissionDTO roleDto : rolePermissions) {
            if (!roleDto.getResourceType().equals("BUCKET")) continue;

            // 非管理员用户只保留自己有自定义权限或自己创建的桶
            if (user.getRoleType() != RoleType.ADMIN) {
                if (!customBucketMap.containsKey(roleDto.getResourceId()) &&
                        !isOwnBucket(userId, roleDto.getResourceId())) {
                    continue;
                }
            }

            // 如果已有自定义权限，覆盖角色权限
            if (!customBucketMap.containsKey(roleDto.getResourceId())) {
                result.add(roleDto);
            }
        }

        // 4. 返回结果
        return result;
    }

    private boolean isOwnBucket(Long userId, Long bucketId) {
        return bucketRepository.findById(bucketId)
                .map(bucket -> Objects.equals(bucket.getOwnerId(), userId))
                .orElse(false);
    }



    public List<UserResourcePermissionDTO> fetchRoleInheritedPermissions(User user) {
        List<UserResourcePermissionDTO> result = new ArrayList<>();
        RoleType role = user.getRoleType();
        String libraryCode = user.getLibraryCode();

        switch (role) {
            case READER:
                // 查询用户自身桶权限
                List<BucketPermission> perms = bucketPermissionRepository.findByUserId(user.getId());
                for (BucketPermission bp : perms) {
                    result.add(UserResourcePermissionDTO.builder()
                            .resourceId(bp.getBucketId())
                            .resourceType("BUCKET")
                            .resourceName(bp.getBucket() != null ? bp.getBucket().getName() : "")
                            .canRead(bp.getPermission().toLowerCase().contains("read"))
                            .canWrite(false) // 读者默认无写删权限
                            .canDelete(false)
                            .permissionSource("ROLE")
                            .build());
                }
                break;

            case LIBRARIAN:
                // 查询馆点下所有桶，默认读写权限
                List<Bucket> buckets = bucketRepository.findByLibraryCode(libraryCode);
                for (Bucket bucket : buckets) {
                    result.add(UserResourcePermissionDTO.builder()
                            .resourceId(bucket.getId())
                            .resourceType("BUCKET")
                            .resourceName(bucket.getName())
                            .canRead(true)
                            .canWrite(true)
                            .canDelete(false) // 根据业务决定是否允许删除
                            .permissionSource("ROLE")
                            .build());
                }
                break;

            case ADMIN:
                // 管理员权限：所有桶全权限
                List<Bucket> allBuckets = bucketRepository.findAll();
                for (Bucket bucket : allBuckets) {
                    result.add(UserResourcePermissionDTO.builder()
                            .resourceId(bucket.getId())
                            .resourceType("BUCKET")
                            .resourceName(bucket.getName())
                            .canRead(true)
                            .canWrite(true)
                            .canDelete(true)
                            .permissionSource("ROLE")
                            .build());
                }
                break;

            default:
                // 其他角色不赋权限
                break;
        }

        return result;
    }




    @Override
    @Transactional
    public void updateUserPermissions(Long userId, List<UserResourcePermissionDTO> permissions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        // 删除旧权限
        bucketPermissionRepository.deleteAll(bucketPermissionRepository.findByUserId(userId));
        filePermissionRepository.deleteAll(filePermissionRepository.findByUser(user));

        // 新增权限
        for (UserResourcePermissionDTO dto : permissions) {
            String permStr = buildPermissionString(dto.isCanRead(), dto.isCanWrite(), dto.isCanDelete());
            if ("BUCKET".equalsIgnoreCase(dto.getResourceType())) {
                BucketPermission bp = new BucketPermission();
                bp.setUserId(userId);
                bp.setBucketId(dto.getResourceId());
                bp.setPermission(permStr);
                bucketPermissionRepository.save(bp);
            } else if ("FILE".equalsIgnoreCase(dto.getResourceType())) {
                UserFile file = userFileRepository.findById(dto.getResourceId())
                        .orElseThrow(() -> new ApiException(404, "文件不存在，ID：" + dto.getResourceId()));

                FilePermission fp = new FilePermission();
                fp.setUser(user);
                fp.setFile(file);
                fp.setPermission(permStr);
                filePermissionRepository.save(fp);
            }
        }
    }


    private String buildPermissionString(boolean read, boolean write, boolean delete) {
        List<String> perms = new ArrayList<>();
        if (read) perms.add("read");
        if (write) perms.add("write");
        if (delete) perms.add("delete");
        return String.join(",", perms);
    }
}
