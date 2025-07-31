package com.github.sdms.service;

import java.util.List;

public class PagedResult<T> {
    public List<T> items;
    public long total;

    public PagedResult(List<T> items, long total) {
        this.items = items;
        this.total = total;
    }
}
