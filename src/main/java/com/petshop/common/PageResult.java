package com.petshop.common;

import java.util.List;

/** 分页结果 */
public class PageResult<T> {

    private List<T> rows;
    private long total;
    private int page;
    private int size;
    private int pages;

    public PageResult(List<T> rows, long total, int page, int size) {
        this.rows = rows;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
    }

    public List<T> getRows() { return rows; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getPages() { return pages; }
}
