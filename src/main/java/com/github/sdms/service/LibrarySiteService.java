package com.github.sdms.service;

import com.github.sdms.dto.LibrarySiteDTO;
import com.github.sdms.dto.LibrarySitePageRequest;
import com.github.sdms.dto.OptionDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LibrarySiteService {

    Page<LibrarySiteDTO> pageSites(LibrarySitePageRequest request);

    List<OptionDTO> getEnabledLibrarySiteOptions();

    Page<OptionDTO> queryOptions(String keyword, int page, int size);

    LibrarySiteDTO createSite(LibrarySiteDTO dto);

    LibrarySiteDTO updateSite(LibrarySiteDTO dto);

    void deleteSite(Long id);

    LibrarySiteDTO getSiteById(Long id);
}
