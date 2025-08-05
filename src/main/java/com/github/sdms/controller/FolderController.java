package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Folder;
import com.github.sdms.service.FolderService;
import com.github.sdms.util.PermissionChecker;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final PermissionChecker permissionChecker;


    @PostMapping("/create")
    @Operation(summary = "创建文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Folder> createFolder(
            @RequestParam String uid,
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) Long parentId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(uid, libraryCode);
        Folder folder = folderService.createFolder(uid, name, parentId, libraryCode);
        return ApiResponse.success("创建成功", folder);
    }

    @PutMapping("/rename")
    @Operation(summary = "重命名文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Folder> renameFolder(
            @RequestParam String uid,
            @RequestParam Long folderId,
            @RequestParam @NotBlank String newName,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(uid, libraryCode);
        Folder folder = folderService.renameFolder(uid, folderId, newName, libraryCode);
        return ApiResponse.success("重命名成功", folder);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> deleteFolder(
            @RequestParam String uid,
            @RequestParam Long folderId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(uid, libraryCode);
        folderService.deleteFolder(uid, folderId, libraryCode);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/list")
    @Operation(summary = "列出指定目录下子文件夹")
    public ApiResponse<List<Folder>> listFolders(
            @RequestParam String uid,
            @RequestParam(required = false) Long parentId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(uid, libraryCode);
        List<Folder> list = folderService.listFolders(uid, parentId, libraryCode);
        return ApiResponse.success("查询成功", list);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取完整目录树")
    public ApiResponse<List<FolderNode>> folderTree(
            @RequestParam String uid,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(uid, libraryCode);
        List<Folder> all = folderService.listAllFolders(uid, libraryCode);
        List<FolderNode> tree = buildTree(all);
        return ApiResponse.success("查询成功", tree);
    }

    @PutMapping("/move")
    @Operation(summary = "移动文件夹到新的父目录")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> moveFolder(
            @RequestParam String uid,
            @RequestParam Long folderId,
            @RequestParam Long newParentId,
            @RequestParam String libraryCode
    ) {
        permissionChecker.checkAccess(uid, libraryCode);
        folderService.moveFolder(uid, folderId, newParentId, libraryCode);
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
