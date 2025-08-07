package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.FolderPageRequest;
import com.github.sdms.dto.FolderSummaryDTO;
import com.github.sdms.model.Folder;
import com.github.sdms.service.FolderService;
import com.github.sdms.util.CustomerUserDetails;
import com.github.sdms.util.PermissionChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folder")
@RequiredArgsConstructor
@Tag(name = "目录管理", description = "文件夹/目录的增删查改及层级管理接口")
public class FolderController {

    private final FolderService folderService;
    private final PermissionChecker permissionChecker;

    @PostMapping("/page")
    public ApiResponse<Page<FolderSummaryDTO>> pageFolders(@RequestBody FolderPageRequest request,
                                                           @AuthenticationPrincipal CustomerUserDetails userDetails) {
        Page<FolderSummaryDTO> result = folderService.pageFolders(request, userDetails);
        return ApiResponse.success(result);
    }


    @PostMapping("/create")
    @Operation(summary = "创建文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Folder> createFolder(
            @RequestParam Long userId,
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) Long parentId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(userId, libraryCode);
        Folder folder = folderService.createFolder(userId, name, parentId, libraryCode);
        return ApiResponse.success("创建成功", folder);
    }

    @PutMapping("/rename")
    @Operation(summary = "重命名文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Folder> renameFolder(
            @RequestParam Long userId,
            @RequestParam Long folderId,
            @RequestParam @NotBlank String newName,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(userId, libraryCode);
        Folder folder = folderService.renameFolder(userId, folderId, newName, libraryCode);
        return ApiResponse.success("重命名成功", folder);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> deleteFolder(
            @RequestParam Long userId,
            @RequestParam Long folderId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(userId, libraryCode);
        folderService.deleteFolder(userId, folderId, libraryCode);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/list")
    @Operation(summary = "列出指定目录下子文件夹")
    public ApiResponse<List<Folder>> listFolders(
            @RequestParam Long userId,
            @RequestParam(required = false) Long parentId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(userId, libraryCode);
        List<Folder> list = folderService.listFolders(userId, parentId, libraryCode);
        return ApiResponse.success("查询成功", list);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取完整目录树")
    public ApiResponse<List<FolderNode>> folderTree(
            @RequestParam Long userId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(userId, libraryCode);
        List<Folder> all = folderService.listAllFolders(userId, libraryCode);
        List<FolderNode> tree = buildTree(all);
        return ApiResponse.success("查询成功", tree);
    }

    @PutMapping("/move")
    @Operation(summary = "移动文件夹到新的父目录")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> moveFolder(
            @RequestParam Long userId,
            @RequestParam Long folderId,
            @RequestParam Long newParentId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(userId, libraryCode);
        folderService.moveFolder(userId, folderId, newParentId, libraryCode);
        return ApiResponse.success("移动成功", null);
    }

    // 用于前端展示树结构的内部节点类
    public static class FolderNode {
        public Long id;
        public String name;
        public Long parentId;
        public List<FolderNode> children = new ArrayList<>();

        public FolderNode(Folder folder) {
            this.id = folder.getId();
            this.name = folder.getName();
            this.parentId = folder.getParentId();
        }
    }

    private List<FolderNode> buildTree(List<Folder> folders) {
        Map<Long, FolderNode> idMap = folders.stream()
                .collect(Collectors.toMap(Folder::getId, FolderNode::new));
        List<FolderNode> rootNodes = new ArrayList<>();

        for (Folder folder : folders) {
            FolderNode node = idMap.get(folder.getId());
            if (folder.getParentId() == null) {
                rootNodes.add(node);
            } else {
                FolderNode parent = idMap.get(folder.getParentId());
                if (parent != null) {
                    parent.children.add(node);
                }
            }
        }

        return rootNodes;
    }
}
