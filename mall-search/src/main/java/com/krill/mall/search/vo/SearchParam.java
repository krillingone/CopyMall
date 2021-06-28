package com.krill.mall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装所有可能传过来的查询条件
 */
@Data
public class SearchParam {
    // 搜索框 全文匹配关键字
    private String keyword;
    // 三级分类id
    private Long catalog3Id;
    /*排序条件
     *sort=saleCount_asc/desc
     *sort=skuPrice_asc/desc
     *sort=hotScore_asc/desc
     */
    private String sort;

    /* 过滤条件
     * hasStock=0、1 是否有货 0无1有
     * skuPrice=0_1000、_500、500_ 价格区间
     * brandId=1 品牌ID
     * attrs 1_其他:安卓&attrs=2_5.56英寸:6英寸 属性
     */
    private Integer hasStock;
    private String skuPrice;
    private List<Long> brandId;
    private List<String> attrs;

    // 页码
    private Integer pageNum = 1;

    private String _queryString;

}
