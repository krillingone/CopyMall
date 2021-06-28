package com.krill.mall.search.vo;

import com.krill.common.to.es.SkuESModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {

    // 商品信息
    private List<SkuESModel> products;

    private Integer pageNum;    //当前页码
    private Long total;    // 总记录数
    private Integer totalPages;    // 总页码
    private List<Integer> pageNavs;    // 导航页码

    private List<BrandVo> brands;    // 涉及到的品牌

    private List<AttrVo> attrs;    // 涉及到的属性

    private List<CatalogVo> catalogs;    // 涉及到的所有分类

    //面包屑导航
    private List<NavVo> navs = new ArrayList<>();
    private List<Long> attrIds = new ArrayList<>();

/*=============================以上是返给页面的信息===========================================*/

    @Data
    public static class NavVo {
        private String NavName;
        private String NavValue;
        private String link;
    }

    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

}
