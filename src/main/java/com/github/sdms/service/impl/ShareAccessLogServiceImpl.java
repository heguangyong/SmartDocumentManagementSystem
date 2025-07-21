package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.repository.ShareAccessLogRepository;
import com.github.sdms.service.ShareAccessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareAccessLogServiceImpl implements ShareAccessLogService {

    private final ShareAccessLogRepository repository;

    @Override
    public void recordAccess(ShareAccessLog log) {
        try {
            repository.save(log);
        } catch (Exception e) {
            // 捕获异常，统一抛出业务异常
            throw new ApiException("记录访问日志失败");
        }
    }

    @Override
    public List<ShareAccessLog> getAllLogs() {
        try {
            return repository.findAll(Sort.by(Sort.Direction.DESC, "accessTime"));
        } catch (Exception e) {
            throw new ApiException("查询访问日志失败");
        }
    }
}
