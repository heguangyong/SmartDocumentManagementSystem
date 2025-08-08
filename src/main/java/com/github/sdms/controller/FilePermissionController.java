package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.FilePermissionAssignRequest;
import com.github.sdms.dto.FilePermissionDTO;
import com.github.sdms.dto.FilePermissionUpdateRequest;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.service.FilePermissionService;
import com.github.sdms.util.PermissionChecker;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/file-permission")
@RequiredArgsConstructor
public class FilePermissionController {

    private final FilePermissionService filePermissionService;
    private final PermissionChecker permissionChecker;

    @GetMapping("/by-file/{fileId}")
    @Operation(summary = "根据文件ID查询文件权限列表")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<List<FilePermissionDTO>> getPermissionsByFile(@PathVariable Long fileId) {
        List<FilePermissionDTO> list = filePermissionService.getPermissionsByFileId(fileId);
        return ApiResponse.success(list);
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "根据用户ID查询文件权限列表")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<List<FilePermissionDTO>> getPermissionsByUser(@PathVariable Long userId) {
        List<FilePermissionDTO> list = filePermissionService.getPermissionsByUserId(userId);
        return ApiResponse.success(list);
    }

    @PostMapping("/assign")
    @Operation(summary = "给用户分配文件权限")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<FilePermissionDTO> assignPermission(@Valid @RequestBody FilePermissionAssignRequest request) {
        // 校验调用者对文件的操作权限
        permissionChecker.checkAccess(request.getUserId(), request.getLibraryCode());
        FilePermissionDTO dto = filePermissionService.assignPermission(request);
        return ApiResponse.success("权限分配成功", dto);
    }

    @PutMapping("/update")
    @Operation(summary = "更新文件权限")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<FilePermissionDTO> updatePermission(@Valid @RequestBody FilePermissionUpdateRequest request) {
        FilePermissionDTO dto = filePermissionService.updatePermission(request);
        return ApiResponse.success("权限更新成功", dto);
    }

    @DeleteMapping("/revoke")
    @Operation(summary = "撤销用户的文件权限")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> revokePermission(@RequestParam Long fileId, @RequestParam Long userId) {
        filePermissionService.revokePermission(fileId, userId);
        return ApiResponse.success("权限撤销成功", null);
    }

    @GetMapping("/check")
    @Operation(summary = "校验用户对文件的指定权限")
    public ApiResponse<Boolean> checkPermission(@RequestParam Long userId,
                                                @RequestParam Long fileId,
                                                @RequestParam PermissionType permissionType) {
        boolean hasPerm = filePermissionService.checkUserPermission(userId, fileId, permissionType);
        return ApiResponse.success(hasPerm);
    }
}
