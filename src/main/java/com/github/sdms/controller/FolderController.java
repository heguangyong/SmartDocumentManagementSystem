package com.github.sdms.controller;

import com.github.sdms.dto.*;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.service.FolderService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CustomerUserDetails;
import com.github.sdms.util.PermissionChecker;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folder")
@RequiredArgsConstructor
public class FolderController {

    @Autowired
    private FolderService folderService;

    @Autowired
    private UserFileService userFileService;

    @Autowired
    private PermissionChecker permissionChecker;

    // 2. 修改 Controller 接口
    @PostMapping("/create")
    @Operation(summary = "创建文件夹", description = "创建文件夹并绑定到指定存储桶")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Folder> createFolder(@RequestBody @Valid CreateFolderRequest request) {
        // 从 SecurityContext 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 权限校验
        permissionChecker.checkAccess(userId, libraryCode);

        // 创建文件夹
        Folder folder = folderService.createFolder(userId, request.getName(), request.getParentId(), request.getBucketId(), libraryCode);
        return ApiResponse.success("创建成功", folder);
    }



    @PostMapping("/rename")
    @Operation(summary = "重命名文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<FolderDTO> renameFolder(@RequestBody @Valid RenameFolderRequest request) {
        Long folderId = request.getFolderId();
        String newName = request.getNewName();

        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 权限校验
        permissionChecker.checkAccess(userId, libraryCode);

        // 执行重命名
        Folder folder = folderService.renameFolder(userId, folderId, newName, libraryCode);

        // 转换为 DTO
        FolderDTO fto = new FolderDTO(folder);

        return ApiResponse.success("重命名成功", fto);
    }




    // 2. 修改 Controller 接口
    @PostMapping("/delete")
    @Operation(summary = "删除文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> deleteFolder(@RequestBody @Valid DeleteFolderRequest request) {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 权限校验
        permissionChecker.checkAccess(userId, libraryCode);

        Long folderId = request.getFolderId();

        // 查询文件夹下的文件
        List<UserFile> filesInFolder = userFileService.listFilesByFolder(userId, folderId, libraryCode);
        if (!filesInFolder.isEmpty()) {
            // 返回提示，需要前端确认删除
            return ApiResponse.failure("文件夹下包含 " + filesInFolder.size() + " 个文件，请确认是否删除",409);
        }

        // 删除文件夹（同步删除文件）
        folderService.deleteFolderWithFiles(userId, folderId, libraryCode);

        return ApiResponse.success("删除成功", null);
    }



    @GetMapping("/list")
    @Operation(summary = "列出指定目录下子文件夹")
    public ApiResponse<List<Folder>> listFolders(@RequestParam Long userId, @RequestParam(required = false) Long parentId, @RequestParam String libraryCode) {
        permissionChecker.checkAccess(userId, libraryCode);
        List<Folder> list = folderService.listFolders(userId, parentId, libraryCode);
        return ApiResponse.success("查询成功", list);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取完整目录树")
    public ApiResponse<List<FolderNode>> folderTree(@RequestParam Long userId, @RequestParam String libraryCode) {
        permissionChecker.checkAccess(userId, libraryCode);
        List<Folder> all = folderService.listAllFolders(userId, libraryCode);
        List<FolderNode> tree = buildTree(all);
        return ApiResponse.success("查询成功", tree);
    }

    @PostMapping("/move")
    @Operation(summary = "批量移动文件和文件夹")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> moveBatch(
            @RequestBody MoveRequest moveRequest
    ) {
        // 从 SecurityContext 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();
        permissionChecker.checkAccess(userId, libraryCode);
        folderService.moveBatch(userId, moveRequest, libraryCode);
        return ApiResponse.success("移动成功", null);
    }

    /**
     * 分层的文件和文件夹列表接口
     * 根据桶ID或文件夹ID获取当前层级的内容（文件夹 + 文件）
     * 文件按docId进行版本折叠展示
     */
    @GetMapping("/content")
    @Operation(summary = "获取指定层级的文件夹和文件列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN') or hasRole('READER')")
    public ApiResponse<List<FolderContentDTO>> getFolderContent(@RequestParam(required = false) Long bucketId, @RequestParam(required = false) Long folderId) {
        // 从JWT token中获取用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 参数验证：bucketId 和 folderId 不能同时为空
        if (bucketId == null && folderId == null) {
            throw new ApiException(400, "bucketId 和 folderId 不能同时为空");
        }

        List<FolderContentDTO> result = new ArrayList<>();

        if (folderId != null) {
            // 获取指定文件夹下的内容
            // 权限检查
            permissionChecker.checkFolderAccess(userId, folderId, libraryCode);

            // 获取子文件夹
            List<Folder> subFolders = folderService.listFoldersByParentId(userId, folderId, libraryCode);
            for (Folder folder : subFolders) {
                result.add(new FolderContentDTO(folder));
            }

            // 获取该文件夹下的文件并按docId分组
            List<UserFile> files = userFileService.listFilesByFolderId(userId, folderId, libraryCode);
            Map<Long, List<UserFile>> fileGroups = groupFilesByDocId(files);

            // 为每个文档组创建FolderContentDTO
            for (Map.Entry<Long, List<UserFile>> entry : fileGroups.entrySet()) {
                List<UserFile> versionFiles = entry.getValue();
                // 按版本号排序，最新版本在前
                versionFiles.sort((a, b) -> Integer.compare(b.getVersionNumber(), a.getVersionNumber()));

                UserFile latestFile = versionFiles.get(0); // 最新版本文件
                result.add(new FolderContentDTO(latestFile, versionFiles));
            }

        } else {
            // 获取桶根目录下的内容（parentId 为 null 的文件夹和文件）
            // 权限检查 - 检查读取权限
            permissionChecker.checkBucketReadPermission(userId, bucketId, libraryCode);

            List<Folder> rootFolders;
            List<UserFile> rootFiles;

            // 如果是桶的拥有者或者有桶的读取权限 → 查看整个桶
            if (permissionChecker.isBucketOwner(userId, bucketId)
                    || permissionChecker.hasReadAccess(userId, bucketId)) {
                rootFolders = folderService.listRootFoldersByBucket(bucketId, libraryCode);
                rootFiles = userFileService.listRootFilesByBucket(bucketId, libraryCode);
            } else if (userDetails.getRoleType() == RoleType.LIBRARIAN) {
                // 馆员用户拥有桶只读权限 → 查看整个桶
                rootFolders = folderService.listRootFoldersByBucket(bucketId, libraryCode);
                rootFiles = userFileService.listRootFilesByBucket(bucketId, libraryCode);
            } else {
                // fallback: 只看自己上传的（理论上不会走到这，保险起见保留）
                rootFolders = folderService.listRootFoldersByBucket(userId, bucketId, libraryCode);
                rootFiles = userFileService.listRootFilesByBucket(userId, bucketId, libraryCode);
            }

// 转 DTO
            for (Folder folder : rootFolders) {
                result.add(new FolderContentDTO(folder));
            }

            Map<Long, List<UserFile>> fileGroups = groupFilesByDocId(rootFiles);

            for (Map.Entry<Long, List<UserFile>> entry : fileGroups.entrySet()) {
                List<UserFile> versionFiles = entry.getValue();
                versionFiles.sort((a, b) -> Integer.compare(b.getVersionNumber(), a.getVersionNumber()));
                UserFile latestFile = versionFiles.get(0);
                result.add(new FolderContentDTO(latestFile, versionFiles));
            }

        }

        // 按名称排序，文件夹在前
        result.sort((a, b) -> {
            if (!a.getType().equals(b.getType())) {
                return "folder".equals(a.getType()) ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });

        return ApiResponse.success("查询成功", result);
    }

    /**
     * 将文件列表按docId分组
     * @param files 文件列表
     * @return 按docId分组的文件Map
     */
    private Map<Long, List<UserFile>> groupFilesByDocId(List<UserFile> files) {
        return files.stream()
                .filter(file -> file.getDocId() != null) // 过滤掉docId为空的文件
                .collect(Collectors.groupingBy(UserFile::getDocId));
    }

    /**
     * 备选文件夹列表接口
     * 用于移动文件夹时选择目标位置
     */
    @GetMapping("/alternatives")
    @Operation(summary = "获取备选文件夹列表（用于移动操作）")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ApiResponse<List<AlternativeFolderDTO>> getAlternativeFolders(@RequestParam Long bucketId, @RequestParam(required = false) Long excludeFolderId // 排除的文件夹ID（移动时不能选择自己或子文件夹）
    ) {
        // 从JWT token中获取用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 权限检查 - 检查读取权限
        permissionChecker.checkBucketReadPermission(userId, bucketId, libraryCode);

        // 获取桶下所有文件夹
        List<Folder> allFolders = folderService.listAllFoldersByBucket(userId, bucketId, libraryCode);

        // 如果有排除的文件夹，需要排除它及其所有子文件夹
        Set<Long> excludeIds = new HashSet<>();
        if (excludeFolderId != null) {
            excludeIds.add(excludeFolderId);
            excludeIds.addAll(folderService.getAllDescendantIds(excludeFolderId, allFolders));
        }

        // 构建备选文件夹列表
        List<AlternativeFolderDTO> result = new ArrayList<>();

        // 添加根目录选项（桶根目录）
        AlternativeFolderDTO rootOption = new AlternativeFolderDTO();
        rootOption.setId(null); // 根目录 ID 为 null
        rootOption.setName("根目录");
        rootOption.setParentId(null);
        rootOption.setPath("根目录");
        rootOption.setLevel(0);
        result.add(rootOption);

        // 构建文件夹路径映射
        Map<Long, String> pathMap = buildPathMap(allFolders);

        // 添加可选文件夹
        for (Folder folder : allFolders) {
            if (!excludeIds.contains(folder.getId())) {
                String path = pathMap.getOrDefault(folder.getId(), folder.getName());
                Integer level = calculateLevel(folder, allFolders);
                result.add(new AlternativeFolderDTO(folder, path, level));
            }
        }

        // 按路径排序
        result.sort((a, b) -> {
            if (a.getLevel() != null && b.getLevel() != null) {
                int levelCompare = a.getLevel().compareTo(b.getLevel());
                if (levelCompare != 0) return levelCompare;
            }
            return a.getPath().compareToIgnoreCase(b.getPath());
        });

        return ApiResponse.success("查询成功", result);
    }

    /**
     * 构建文件夹路径映射
     */
    private Map<Long, String> buildPathMap(List<Folder> folders) {
        Map<Long, String> pathMap = new HashMap<>();
        Map<Long, Folder> idMap = folders.stream().collect(Collectors.toMap(Folder::getId, folder -> folder));

        for (Folder folder : folders) {
            StringBuilder path = new StringBuilder();
            buildPath(folder, idMap, path);
            pathMap.put(folder.getId(), path.toString());
        }

        return pathMap;
    }

    /**
     * 递归构建文件夹路径
     */
    private void buildPath(Folder folder, Map<Long, Folder> idMap, StringBuilder path) {
        if (folder.getParentId() != null) {
            Folder parent = idMap.get(folder.getParentId());
            if (parent != null) {
                buildPath(parent, idMap, path);
                path.append("/");
            }
        }
        path.append(folder.getName());
    }

    /**
     * 计算文件夹层级深度
     */
    private Integer calculateLevel(Folder folder, List<Folder> allFolders) {
        Map<Long, Folder> idMap = allFolders.stream().collect(Collectors.toMap(Folder::getId, folder1 -> folder1));

        int level = 0;
        Long currentParentId = folder.getParentId();
        while (currentParentId != null) {
            level++;
            Folder parent = idMap.get(currentParentId);
            if (parent == null) break;
            currentParentId = parent.getParentId();
        }

        return level;
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
        Map<Long, FolderNode> idMap = folders.stream().collect(Collectors.toMap(Folder::getId, FolderNode::new));
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
