package com.krill.mall.product.vo;

import lombok.Data;

@Data
public class BrandVo {
    //      {
    //        "msg": "success",
    //            "code": 0,
    //            "data": [{
    //                  "brandId": 0,
    //                  "brandName": "string",
    //              }]
    //      }
    private Long brandId;
    private String brandName;
}
