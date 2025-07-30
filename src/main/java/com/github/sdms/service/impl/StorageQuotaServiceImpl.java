package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.User;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.StorageQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StorageQuotaServiceImpl implements StorageQuotaService {

    private final UserRepository userRepository;

    private final UserFileRepository userFileRepository;

    // 配额映射表（字节）
    private static final Map<String, Long> ROLE_QUOTA = Map.of(
            "READER", 1L * 1024 * 1024 * 1024,       // 1GB
            "LIBRARIAN", 10L * 1024 * 1024 * 1024,   // 10GB
            "ADMIN", Long.MAX_VALUE                    // 无限
    );

    /**
     * 获取最大配额，根据用户的角色来返回不同的存储配额
     * @param uid 用户ID
     * @param libraryCode 租户标识
     * @return 用户的最大存储配额
     */
    @Override
    public long getMaxQuota(String uid, String libraryCode) {
        RoleType roleType = userRepository.findByUidAndLibraryCode(uid, libraryCode)
                .map(User::getRoleType)
                .orElseThrow(() -> new ApiException(403, "用户未找到或无权限"));

        return ROLE_QUOTA.getOrDefault(roleType.toString().toUpperCase(), 0L);
    }

    /**
     * 获取用户已使用的存储配额
     * @param uid 用户ID
     * @param libraryCode 租户标识
     * @return 用户已使用的存储配额
     */
    @Override
    public long getUsedQuota(String uid, String libraryCode) {
        // 直接用Repository查询，避免依赖UserFileService
        return userFileRepository.findByUidAndDeleteFlagFalseAndLibraryCode(uid, libraryCode)
                .stream()
                .mapToLong(UserFile::getSize)
                .sum();
    }

    /**
     * 判断用户是否可以上传指定大小的文件
     * @param uid 用户ID
     * @param fileSize 文件大小（字节）
     * @param libraryCode 租户标识
     * @return 如果用户可以上传该文件，则返回true，否则返回false
     */
    @Override
    public boolean canUpload(String uid, long fileSize, String libraryCode) {
        long used = getUsedQuota(uid, libraryCode);
        long max = getMaxQuota(uid, libraryCode);
        return (used + fileSize) <= max;
    }

    /**
     * 获取用户剩余的存储配额
     * @param uid 用户ID
     * @param libraryCode 租户标识
     * @return 用户剩余的存储配额
     */
    @Override
    public long getRemainingQuota(String uid, String libraryCode) {
        return getMaxQuota(uid, libraryCode) - getUsedQuota(uid, libraryCode);
    }
}
