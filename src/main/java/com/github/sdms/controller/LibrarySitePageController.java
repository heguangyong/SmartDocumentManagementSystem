package com.github.sdms.controller;

import com.github.sdms.dto.LibrarySiteDTO;
import com.github.sdms.dto.LibrarySitePageRequest;
import com.github.sdms.service.LibrarySiteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/library-sites")
@Controller
public class LibrarySitePageController {

    private final LibrarySiteService librarySiteService;

    public LibrarySitePageController(LibrarySiteService librarySiteService) {
        this.librarySiteService = librarySiteService;
    }

    /**
     * 馆点列表页面，默认加载第一页数据
     */
    @GetMapping
    public String librarySitePage(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "100") int size) {
        LibrarySitePageRequest request = new LibrarySitePageRequest();
        request.setPage(page);
        request.setSize(size);

        List<LibrarySiteDTO> sites = librarySiteService.pageSites(request).getContent();
        model.addAttribute("librarySites", sites);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "library-sites/list";
    }

    /**
     * 新建馆点页面
     */
    @GetMapping("/create")
    public String librarySiteCreatePage() {
        return "library-sites/form";
    }

    /**
     * 编辑馆点页面，传入id用于前端加载详情
     */
    @GetMapping("/edit/{id}")
    public String librarySiteEditPage(@PathVariable Long id, Model model) {
        model.addAttribute("librarySiteId", id);
        return "library-sites/form";
    }
}
