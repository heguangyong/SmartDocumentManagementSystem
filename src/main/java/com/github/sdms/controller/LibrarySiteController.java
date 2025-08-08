package com.github.sdms.controller;

import com.github.sdms.dto.LibrarySiteDTO;
import com.github.sdms.dto.LibrarySitePageRequest;
import com.github.sdms.dto.OptionDTO;
import com.github.sdms.service.LibrarySiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class LibrarySiteController {

    private final LibrarySiteService librarySiteService;

    /**
     * 馆点分页查询（后台使用）
     */
    @GetMapping("/library-sites")
    @Operation(summary = "分页查询馆点", description = "支持分页查询图书馆点，可用于后台管理列表展示")
    @PreAuthorize("hasAnyRole('ADMIN')") // 限管理员
    public Page<LibrarySiteDTO> pageLibrarySites(LibrarySitePageRequest request) {
        return librarySiteService.pageSites(request);
    }

    /**
     * 馆点下拉选项（支持前端搜索 + 分页）
     */
    @GetMapping("/library-sites/options")
    @Operation(summary = "查询馆点选项（分页+搜索）", description = "提供给前端下拉搜索使用，支持模糊搜索和分页")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // 館員和管理員都可以查
    public Page<OptionDTO> queryLibrarySites(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return librarySiteService.queryOptions(keyword, page, size);
    }
}

