package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Folder;
import com.github.sdms.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/folder")
@RequiredArgsConstructor
public class PublicFolderController {

    private final FolderService folderService;

    @GetMapping("/view")
    @Operation(summary = "通过分享 Token 访问文件夹及内容")
    public ApiResponse<SharedFolderView> viewSharedFolder(
            @RequestParam String token
    ) {
        Folder folder = folderService.getFolderByShareToken(token);
        if (folder == null || !Boolean.TRUE.equals(folder.getShared())) {
            return ApiResponse.failure("分享链接无效或已失效");
        }

        // 可选扩展：返回该目录下的文件或子目录（只读）
        List<Folder> children = folderService.listFolders(folder.getOwnerId(), folder.getId());

        SharedFolderView result = new SharedFolderView(folder.getName(), folder.getId(), children);
        return ApiResponse.success("访问成功", result);
    }

    public record SharedFolderView(String folderName, Long folderId, List<Folder> children) {}
}
