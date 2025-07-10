package com.github.sdms.service.impl;

import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StorageQuotaServiceImpl implements StorageQuotaService {

    private final UserRepository userRepository;
    private final UserFileService userFileService;

    // 配额映射表（字节）
    private static final Map<String, Long> ROLE_QUOTA = Map.of(
            "READER", 1L * 1024 * 1024 * 1024, // 1GB
            "LIBRARIAN", 10L * 1024 * 1024 * 1024, // 10GB
            "ADMIN", Long.MAX_VALUE // 无限
    );

    @Override
    public long getMaxQuota(String uid) {
        Role role = userRepository.findByUid(uid)
                .map(AppUser::getRole)
                .orElse(Role.READER); // 默认角色

        return ROLE_QUOTA.getOrDefault(role.toString().toUpperCase(), 0L);
    }

    @Override
    public long getUsedQuota(String uid) {
        return userFileService.getUserStorageUsage(uid);
    }

    @Override
    public boolean canUpload(String uid, long fileSize) {
        long used = getUsedQuota(uid);
        long max = getMaxQuota(uid);
        return (used + fileSize) <= max;
    }

    @Override
    public long getRemainingQuota(String uid) {
        return getMaxQuota(uid) - getUsedQuota(uid);
    }
}

