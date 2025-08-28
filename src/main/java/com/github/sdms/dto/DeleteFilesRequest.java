package com.github.sdms.dto;

// 请求参数对象
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class DeleteFilesRequest {
    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> fileIds;

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }
}

