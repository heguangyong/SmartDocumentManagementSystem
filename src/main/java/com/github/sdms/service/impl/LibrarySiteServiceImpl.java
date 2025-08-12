package com.github.sdms.service.impl;

import com.github.sdms.dto.LibrarySiteDTO;
import com.github.sdms.dto.LibrarySitePageRequest;
import com.github.sdms.dto.OptionDTO;
import com.github.sdms.model.LibrarySite;
import com.github.sdms.repository.LibrarySiteRepository;
import com.github.sdms.service.LibrarySiteService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibrarySiteServiceImpl implements LibrarySiteService {

    @Autowired
    private LibrarySiteRepository librarySiteRepository;

    @Override
    public Page<LibrarySiteDTO> pageSites(LibrarySitePageRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("createTime")));

        Page<LibrarySite> page = librarySiteRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(request.getKeyword())) {
                String like = "%" + request.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("code"), like)
                ));
            }
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<LibrarySiteDTO> content = page.getContent().stream().map(site -> {
            LibrarySiteDTO dto = new LibrarySiteDTO();
            dto.setId(site.getId());
            dto.setCode(site.getCode());
            dto.setName(site.getName());
            dto.setAddress(site.getAddress());
            dto.setType(site.getType());
            dto.setStatus(site.getStatus());
            return dto;
        }).toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public List<OptionDTO> getEnabledLibrarySiteOptions() {
        List<LibrarySite> enabledSites = librarySiteRepository.findAllByStatusTrue(Sort.by(Sort.Order.asc("sortOrder")));
        return enabledSites.stream()
                .map(site -> new OptionDTO(site.getName(), site.getCode()))
                .collect(Collectors.toList());
    }

    public Page<OptionDTO> queryOptions(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder"));
        Page<LibrarySite> result;

        if (StringUtils.hasText(keyword)) {
            result = librarySiteRepository.findByStatusTrueAndNameContainingIgnoreCase(keyword, pageable);
        } else {
            result = librarySiteRepository.findByStatusTrue(pageable);
        }

        return result.map(site -> new OptionDTO(site.getName(), site.getCode()));
    }

    @Override
    public LibrarySiteDTO createSite(LibrarySiteDTO dto) {
        LibrarySite entity = new LibrarySite();
        BeanUtils.copyProperties(dto, entity);
        librarySiteRepository.save(entity);
        dto.setId(entity.getId());
        return dto;
    }

    @Override
    public LibrarySiteDTO updateSite(LibrarySiteDTO dto) {
        LibrarySite entity = librarySiteRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("馆点不存在"));
        BeanUtils.copyProperties(dto, entity);
        librarySiteRepository.save(entity);
        return dto;
    }

    @Override
    public void deleteSite(Long id) {
        if (!librarySiteRepository.existsById(id)) {
            throw new RuntimeException("馆点不存在");
        }
        librarySiteRepository.deleteById(id);
    }

    @Override
    public LibrarySiteDTO getSiteById(Long id) {
        LibrarySite entity = librarySiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("馆点不存在"));
        LibrarySiteDTO dto = new LibrarySiteDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

}
