package com.krill.mall.product.vo;

import com.krill.mall.product.entity.SkuImagesEntity;
import com.krill.mall.product.entity.SkuInfoEntity;
import com.krill.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    // 1、sku基本信息 pms_sku_info
    SkuInfoEntity info;
    // add, 有货无货
    boolean hasStock = true;

    // 2、sku图片信息 pms_sku_images
    List<SkuImagesEntity> images;
    // 3、销售属性组合
    List<SkuItemSaleAttrsVo> saleAttr;
    // 4、spu介绍
    SpuInfoDescEntity desc;
    // 5、spu规格参数
    List<SpuItemGroupAttrVo> groupAttrs;

}
