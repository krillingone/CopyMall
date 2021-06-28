package com.krill.mall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.krill.common.to.es.SkuESModel;
import com.krill.mall.search.config.MallElasticSearchConfig;
import com.krill.mall.search.constant.ESConstant;
import com.krill.mall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient client;

    @Override
    public Boolean productStatusUp(List<SkuESModel> skuESModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuESModel skuESModel : skuESModels) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest(ESConstant.PRODUCT_INDEX);
            indexRequest.id(skuESModel.getSkuId().toString());
            String s = JSON.toJSONString(skuESModel);
//            System.out.println("JSON model:" + s);
            indexRequest.source(s, XContentType.JSON);

            // 放进去
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = client.bulk(bulkRequest, MallElasticSearchConfig.COMMON_OPTIONS);

        //TODO 批量上传错误处理：re上传、提示、。。。
        boolean bulkHasFailures = bulk.hasFailures();
        if (bulkHasFailures) {
            List<String> failIdCollect = Arrays.stream(bulk.getItems()).map(item -> {
                return item.getId();
            }).collect(Collectors.toList());
            log.error("商品上架错误: {}", failIdCollect);
        }

        return bulkHasFailures;
    }
}
