package com.krill.mall.search.service;

import com.krill.common.to.es.SkuESModel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public interface ProductSaveService {
    Boolean productStatusUp(List<SkuESModel> skuESModels) throws IOException;
}
