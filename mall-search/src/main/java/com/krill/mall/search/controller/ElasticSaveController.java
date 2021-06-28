package com.krill.mall.search.controller;

import com.krill.common.exception.BizCodeEnum;
import com.krill.common.to.es.SkuESModel;
import com.krill.common.utils.R;
import com.krill.mall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;

    /**
     * 商品上架
     * @param skuESModels
     * @return
     */
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuESModel> skuESModels) {

        Boolean statusUpFailure = false;
        try {
            statusUpFailure = productSaveService.productStatusUp(skuESModels);
        } catch (IOException e) {
            log.error("ElasticSaveController商品上架错误： {}", e);
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }

        if (statusUpFailure) {
            return R.error();
        } else {
            return R.ok();
        }
    }

}
