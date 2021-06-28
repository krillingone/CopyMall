package com.krill.mall.search.service;

import com.krill.mall.search.vo.SearchParam;
import com.krill.mall.search.vo.SearchResult;

public interface MallSearchService {
    /**
     * @param param 检索参数
     * @return 检索结果,包含页面所需所有信息
     */
    SearchResult search(SearchParam param);
}
