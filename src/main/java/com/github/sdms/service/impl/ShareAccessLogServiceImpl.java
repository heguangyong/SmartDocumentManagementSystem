package com.github.sdms.service.impl;

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
        repository.save(log);
    }

    @Override
    public List<ShareAccessLog> getAllLogs() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "accessTime"));
    }

}
