package com.github.sdms.service;

import com.github.sdms.util.UserInfoCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时刷新机制（主动更新）
 * 扫描所有缓存用户 uid，批量刷新
 */
@Component
public class UserInfoCacheRefresher {

    private final OAuthUserInfoService userInfoService;

    public UserInfoCacheRefresher(OAuthUserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    // 每 30 分钟执行一次，可调
    @Scheduled(fixedRate = 30 * 60 * 1000L)
    public void refreshCache() {
        System.out.println(">>> 进入 UserInfoCacheRefresher.refreshCache");
        for (String uid : UserInfoCache.allKeys()) {
            UserInfoCache.refresh(uid, () -> userInfoService.getUserInfoByUid(uid));
        }
    }
}
