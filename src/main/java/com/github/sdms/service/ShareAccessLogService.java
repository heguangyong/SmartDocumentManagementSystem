package com.github.sdms.service;

import com.github.sdms.model.ShareAccessLog;

import java.util.List;

public interface ShareAccessLogService {
    void recordAccess(ShareAccessLog log);
    List<ShareAccessLog> getAllLogs();

}
