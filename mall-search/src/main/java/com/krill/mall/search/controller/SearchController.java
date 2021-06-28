package com.krill.mall.search.controller;

import com.krill.mall.search.service.MallSearchService;
import com.krill.mall.search.vo.SearchParam;
import com.krill.mall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request) {

        param.set_queryString(request.getQueryString());
        // 根据参数在es中检索商品
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result", result);

        return "list";
    }

}
