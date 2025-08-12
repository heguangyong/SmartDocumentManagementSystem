package com.github.sdms.service.impl;

import com.github.sdms.dto.*;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.*;
import com.github.sdms.model.enums.BucketAction;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.*;
import com.github.sdms.service.BucketPermissionService;
import com.github.sdms.service.BucketService;
import com.github.sdms.service.MinioService;
import com.github.sdms.util.BucketUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketServiceImpl implements BucketService {

    private final BucketRepository bucketRepository;
    private final MinioClient minioClient;
    private final MinioService minioService;
    private final BucketPermissionService bucketPermissionService;
    private final BucketPermissionRepository bucketPermissionRepository;
    private final UserRepository userRepository;
    private final PermissionResourceRepository permissionResourceRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * 创建存储桶（同时在数据库和 MinIO 创建）
     */
    @Transactional
    @Override
    public Bucket createBucket(Bucket bucket) {
        if (bucketRepository.existsByName(bucket.getName())) {
            throw new ApiException(400, "桶名已存在");
        }
        if (bucket.getMaxCapacity() == null) {
            bucket.setMaxCapacity(1073741824L); // 1GB,默认1G，需要时可调整，因为桶于用户是配置关系，这里不考虑根据角色定死最大容量。避免一个桶对应多个不同角色时出现逻辑不清的情况
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket.getName()).build()
                );
                log.info("MinIO 创建桶成功：{}", bucket.getName());
            } else {
                log.warn("MinIO 中桶已存在：{}", bucket.getName());
            }
        } catch (MinioException e) {
            log.error("MinIO 创建桶失败", e);
            throw new ApiException(500, "创建 MinIO 桶失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("未知错误：MinIO 创建桶失败", e);
            throw new ApiException(500, "MinIO 创建桶异常：" + e.getMessage());
        }
        return bucketRepository.save(bucket);
    }

    /**
     * 根据 ID 查询桶（同时校验 MinIO 中是否存在）
     */
    @Override
    public Bucket getBucketById(Long id) {
        Bucket bucket = bucketRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getName()).build()
            );
            if (!exists) {
                throw new ApiException(404, "MinIO 中桶不存在：" + bucket.getName());
            }
        } catch (Exception e) {
            log.error("MinIO 校验桶存在性失败", e);
            throw new ApiException(500, "MinIO 查询异常：" + e.getMessage());
        }

        return bucket;
    }

    /**
     * 获取所有桶（从 MinIO 获取后与数据库比对，只保留存在的）
     */
    @Override
    public List<Bucket> getAllBuckets() {
        List<Bucket> all = bucketRepository.findAll();
        return all.stream().filter(bucket -> {
            try {
                return minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket.getName()).build()
                );
            } catch (Exception e) {
                log.warn("MinIO 检查桶 {} 失败: {}", bucket.getName(), e.getMessage());
                return false;
            }
        }).toList();
    }

    /**
     * 更新桶信息（校验桶名唯一 & MinIO 存在）
     */
    @Transactional
    @Override
    public Bucket updateBucket(Bucket bucket) {
        Bucket existing = bucketRepository.findById(bucket.getId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        if (!existing.getName().equals(bucket.getName()) &&
                bucketRepository.existsByName(bucket.getName())) {
            throw new ApiException(400, "桶名已存在");
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(existing.getName()).build()
            );
            if (!exists) {
                throw new ApiException(404, "MinIO 中不存在桶：" + existing.getName());
            }
        } catch (Exception e) {
            log.error("MinIO 校验桶失败", e);
            throw new ApiException(500, "MinIO 查询异常：" + e.getMessage());
        }

        existing.setName(bucket.getName());
        existing.setDescription(bucket.getDescription());
        return bucketRepository.save(existing);
    }

    /**
     * 删除桶（从 MinIO 和数据库中都删除）
     */
    @Transactional
    @Override
    public void deleteBucket(Long id) {
        Bucket bucket = bucketRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getName()).build()
            );
            if (exists) {
                log.info("尝试删除 MinIO 中桶：{}", bucket.getName());
                // 不建议直接删除 MinIO 桶，这里可根据你业务安全策略启用
                // minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket.getName()).build());
            } else {
                log.warn("MinIO 中桶不存在，不执行删除：{}", bucket.getName());
            }
        } catch (Exception e) {
            log.error("删除桶时 MinIO 校验失败", e);
            throw new ApiException(500, "MinIO 查询异常：" + e.getMessage());
        }

        bucketRepository.deleteById(id);
    }

    @Override
    public List<Bucket> getAccessibleBuckets(Long userId) {
        // 获取用户拥有权限的桶ID列表
        List<Long> accessibleBucketIds = bucketPermissionService.getAccessibleBucketIds(userId);

        // 获取用户自己拥有的桶
        List<Bucket> ownBuckets = bucketRepository.findByOwnerId(userId);

        // 查询所有桶，合并并去重
        List<Bucket> authorizedBuckets = bucketRepository.findAllById(accessibleBucketIds);

        // 合并ownBuckets和authorizedBuckets，去重
        Set<Bucket> resultSet = new HashSet<>();
        resultSet.addAll(ownBuckets);
        resultSet.addAll(authorizedBuckets);

        // 过滤存在于MinIO中的桶
        return resultSet.stream().filter(bucket -> {
            try {
                return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket.getName()).build());
            } catch (Exception e) {
                log.warn("检查MinIO桶失败: {}", bucket.getName(), e);
                return false;
            }
        }).toList();
    }

    @Override
    public Bucket getUserDefaultBucket(Long userId, String libraryCode) {
        String bucketName = BucketUtil.getBucketName(userId, libraryCode);
        return bucketRepository.findByName(bucketName)
                .orElseThrow(() -> new ApiException(404, "未找到用户默认桶: " + bucketName));
    }

    @Override
    public List<String> findBucketNamesByIds(Set<Long> bucketIds) {
        if (bucketIds == null || bucketIds.isEmpty()) {
            return Collections.emptyList();
        }
        return bucketRepository.findAllById(bucketIds)
                .stream()
                .map(Bucket::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Bucket> getOptionalBucketByName(String name) {
        return bucketRepository.findByName(name);
    }

    @Override
    public Page<BucketSummaryDTO> pageBuckets(BucketPageRequest request) {
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());

        Page<Bucket> page = bucketRepository.findByNameLike(request.getKeyword(), pageable);

        List<BucketSummaryDTO> dtos = page.getContent().stream().map(bucket -> {
            int userCount = bucketPermissionRepository.countByBucketId(bucket.getId());
            long usedCapacity = minioService.calculateUsedCapacity(bucket.getName()); // 使用 MinioService 实现
            return BucketSummaryDTO.builder()
                    .id(bucket.getId())
                    .name(bucket.getName())
                    .ownerId(bucket.getOwnerId())
                    .createTime(bucket.getCreatedAt())
                    .maxCapacity(bucket.getMaxCapacity())
                    .usedCapacity(usedCapacity)
                    .accessUserCount(userCount)
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public void batchAssignPermissions(BatchAssignBucketPermissionRequest request) {
        Long bucketId = request.getBucketId();
        String permissionStr = String.join(",", request.getPermissions());

        for (Long userId : request.getUserIds()) {
            BucketPermission bp = bucketPermissionRepository
                    .findByUserIdAndBucketId(userId, bucketId)
                    .orElse(new BucketPermission());

            bp.setUserId(userId);
            bp.setBucketId(bucketId);
            bp.setPermission(permissionStr);
            bp.setUpdatedAt(new Date());
            bucketPermissionRepository.save(bp);
        }
    }


    @Override
    @Transactional
    public void removeBucketPermission(Long bucketId, Long userId) {
        bucketPermissionRepository.deleteByUserIdAndBucketId(userId, bucketId);
    }


    @Override
    @Transactional
    public void updateBucketCapacity(Long bucketId, Long maxCapacity) {
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));
        bucket.setMaxCapacity(maxCapacity);
        bucketRepository.save(bucket);
    }

    @Override
    @Transactional
    public Bucket createBucketByAdmin(CreateBucketRequest request) {
        try {
            // 参数校验（业务错误使用400）
            if (request.getOwnerId() == null) {
                throw new ApiException("必须指定 ownerId"); // 使用默认400状态
            }

            User user = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ApiException(404, "用户不存在")); // 明确404状态

            // 生成桶名
            String bucketName = generateBucketName(request, user);

            // 双重校验（业务冲突使用400）
            checkBucketExistence(bucketName);

            // 创建MinIO存储桶（基础设施错误使用500）
            createMinioBucket(bucketName);

            // 构建并保存实体
            return saveBucketEntity(request, user, bucketName);

        } catch (ApiException e) {
            throw e; // 直接传递已封装的异常
        } catch (Exception e) {
            // 未知异常统一转为500错误
            throw new ApiException(500, "系统内部错误: " + e.getMessage());
        }
    }

    // 桶名生成逻辑
    private String generateBucketName(CreateBucketRequest request, User user) {
        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().toLowerCase();
            if (!name.matches("^[a-z0-9-]{3,}$")) {
                throw new ApiException("桶名只能包含小写字母、数字和横线");
            }
            return name;
        }
        return BucketUtil.getBucketName(request.getOwnerId(), user.getLibraryCode());
    }

    // 存在性校验（复用逻辑）
    private void checkBucketExistence(String bucketName) {
        // 数据库校验
        if (bucketRepository.existsByName(bucketName)) {
            throw new ApiException("桶名已存在");
        }
        // MinIO校验
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (exists) {
                throw new ApiException("MinIO中桶名已存在");
            }
        } catch (Exception e) {
            throw new ApiException(500, "存储服务校验失败: " + e.getMessage());
        }
    }

    // MinIO操作封装
    private void createMinioBucket(String bucketName) {
        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
        } catch (Exception e) {
            throw new ApiException(500, "存储桶创建失败: " + e.getMessage());
        }
    }

    // 实体保存
    private Bucket saveBucketEntity(CreateBucketRequest request, User user, String bucketName) {
        Bucket bucket = new Bucket();
        bucket.setName(bucketName);
        bucket.setDescription(request.getDescription());
        bucket.setMaxCapacity(request.getMaxCapacity() == null ? 1073741824L : request.getMaxCapacity());
        bucket.setOwnerId(request.getOwnerId());
        bucket.setLibraryCode(user.getLibraryCode());
        bucket.setCreatedAt(new Date());
        bucket.setUpdatedAt(new Date());
        return bucketRepository.save(bucket);
    }

    @Override
    @Transactional
    public Bucket updateBucketInfo(Long id, Bucket bucket) {
        Bucket dbBucket = bucketRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));
        dbBucket.setName(bucket.getName());
        dbBucket.setDescription(bucket.getDescription());
        dbBucket.setMaxCapacity(bucket.getMaxCapacity());
        dbBucket.setUpdatedAt(new Date());
        return bucketRepository.save(dbBucket);
    }


    @Override
    public List<BucketUserPermissionDTO> getBucketUserPermissionsWithSource(Long bucketId) {
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        List<BucketUserPermissionDTO> result = new ArrayList<>();

        // 1. 查询桶直接授权权限
        List<BucketPermission> directPermissions = bucketPermissionRepository.findByBucketId(bucketId);
        Map<Long, BucketUserPermissionDTO> userPermMap = new HashMap<>();

        for (BucketPermission bp : directPermissions) {
            User user = userRepository.findById(bp.getUserId())
                    .orElseThrow(() -> new ApiException(404, "用户不存在"));

            BucketUserPermissionDTO dto = new BucketUserPermissionDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRoleType(user.getRoleType());
            dto.setPermissions(parsePermissionString(bp.getPermission()));
            dto.setUpdatedAt(bp.getUpdatedAt());
            userPermMap.put(user.getId(), dto);
        }

        // 2. 查询所有用户，推算角色权限覆盖（角色权限存在rolePermissionRepository）
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            // 跳过已有直接权限的用户
            if (userPermMap.containsKey(user.getId())) {
                continue;
            }

            RoleType role = user.getRoleType();

            // 管理员拥有所有权限
            if (role == RoleType.ADMIN) {
                BucketUserPermissionDTO dto = new BucketUserPermissionDTO();
                dto.setUserId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setRoleType(role);
                dto.setPermissions(List.of(BucketAction.READ, BucketAction.WRITE, BucketAction.DELETE, BucketAction.MANAGE));
                dto.setUpdatedAt(null);
                userPermMap.put(user.getId(), dto);
                continue;
            }

            // 馆员角色，根据角色权限表获取权限
            if (role == RoleType.LIBRARIAN) {
                // 先查找 PermissionResource 是否匹配当前bucketId
                Optional<PermissionResource> prOpt = permissionResourceRepository.findById(bucketId);
                if (prOpt.isEmpty()) continue;

                PermissionResource pr = prOpt.get();
                if (!"BUCKET".equalsIgnoreCase(pr.getResourceType())) continue;

                // 查询角色权限
                List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleTypeAndResourceId(role, bucketId);
                if (rolePermissions.isEmpty()) continue;

                // 合并权限
                Set<BucketAction> actions = new HashSet<>();
                for (RolePermission rp : rolePermissions) {
                    actions.addAll(parsePermissionString(rp.getPermission().name()));
                }

                BucketUserPermissionDTO dto = new BucketUserPermissionDTO();
                dto.setUserId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setRoleType(role);
                dto.setPermissions(new ArrayList<>(actions));
                dto.setUpdatedAt(null);
                userPermMap.put(user.getId(), dto);
            }

            // 普通用户 READER 角色，默认无权限，除非直接授权已覆盖，不做处理
        }

        result.addAll(userPermMap.values());
        return result;
    }





    /**
     * 将逗号分隔的权限字符串转为 BucketAction 枚举列表
     */
    private List<BucketAction> parsePermissionString(String permStr) {
        if (permStr == null || permStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(permStr.split(","))
                .map(String::trim)
                .map(s -> BucketAction.valueOf(s.toUpperCase()))
                .toList();
    }


    /**
     * 计算用户对桶的最终有效权限（合并直接授权与角色授权）
     */
    @Override
    public List<String> getEffectiveBucketPermission(Long userId, Long bucketId) {
        if (userId == null || bucketId == null) {
            throw new ApiException(400, "参数错误");
        }

        Set<String> perms = new HashSet<>();

        // 1. 直接授权
        Optional<BucketPermission> directOpt = bucketPermissionRepository.findByBucketIdAndUserId(bucketId, userId);
        directOpt.ifPresent(bp -> {
            if (bp.getPermission() != null) {
                perms.addAll(Arrays.asList(bp.getPermission().split(",")));
            }
        });

        // 2. 角色权限 - 先获取用户角色
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("用户不存在"));

        RoleType roleType = user.getRoleType();

        // 查询角色对该bucket资源的权限列表
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleTypeAndResourceId(roleType, bucketId);
        for (RolePermission rp : rolePermissions) {
            if (rp.getPermission() != null) {
                perms.add(rp.getPermission().name());
            }
        }

        return perms.stream().sorted().toList();
    }



}
