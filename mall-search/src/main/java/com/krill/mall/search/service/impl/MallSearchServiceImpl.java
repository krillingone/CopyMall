package com.krill.mall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.krill.common.to.es.SkuESModel;
import com.krill.common.utils.R;
import com.krill.mall.search.config.MallElasticSearchConfig;
import com.krill.mall.search.constant.ESConstant;
import com.krill.mall.search.feign.ProductFeignService;
import com.krill.mall.search.service.MallSearchService;
import com.krill.mall.search.vo.AttrResponseVo;
import com.krill.mall.search.vo.SearchParam;
import com.krill.mall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("SearchService")
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) {
        SearchResult searchResult = null;

        // 检索请求
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            // 执行
            SearchResponse response = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);

            // 分析响应数据并封装
            searchResult = buildSearchResult(response, param);
//            System.out.println(searchResult);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return searchResult;
    }

    /**
     * 准备检索请求
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        /**模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存）*/
        // #1 bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // #1.1 bool - must
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // #1.2 bool - filter
        // #1.2.1 bool - filter - 三级分类
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // #1.2.2 bool - filter - 品牌id
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // #1.2.3 bool - filter - 所有指定属性    attrs=1_5寸:8寸&attrs=2_16G:8G
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();

                String[] s = attr.split("_");
                String attrId = s[0];
                String[] attrValue = s[1].split(":");

                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));

                // 每一个都得生成一个nestedQuery，然后拼到boolQuery了里
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // #1.2.4 bool - filter - 是否有库存
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        // #1.2.5 bool - filter - 价格区间    _500;1_500;500_
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] split = param.getSkuPrice().split("_");
            if (split.length == 2) {    // _500会被split为[null, 500]
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(split[1]);

                } else {
                    rangeQuery.gte(split[0]).lte(split[1]);
                }
            } else if (split.length == 1) {    // split后长度为1，则必定是100_
                rangeQuery.gte(split[0]);
            }
            boolQuery.filter(rangeQuery);
        }

        // 封装条件
        sourceBuilder.query(boolQuery);

        /**排序，分页，高亮*/
        // 排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] split = sort.split("_");
            sourceBuilder.sort(split[0], split[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页   (pageNum - 1) * size
        sourceBuilder.from((param.getPageNum() - 1) * ESConstant.PRODUCT_PAGE_SIZE);
        sourceBuilder.size(ESConstant.PRODUCT_PAGE_SIZE);

        // 高亮   传了keyword才高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();

            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            sourceBuilder.highlighter(highlightBuilder);
        }

        /**聚合分析*/
        // 1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 品牌聚合子聚合：name, img
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);    //=== 聚合品牌 brand ===//

        // 2、分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        // 分类子聚合： name
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);    //=== 聚合分类 catalog ===//

        // 3、属性聚合
        /*---- nested_attr_agg
                  |
                  attr_id_agg
                     |
                     attr_name_agg
                     attr_value_agg

         */
        NestedAggregationBuilder nested_attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 属性子聚合
        // 有多少种属性
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(10);
        // attr的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // attr的所有可能属性值
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        nested_attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(nested_attr_agg);    //=== 聚合属性 attr ===//


        String dsl = sourceBuilder.toString();
        System.out.println("构建的DSL" + dsl);

        SearchRequest searchRequest = new SearchRequest(new String[]{ESConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }

    /**
     * 构建结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1、检索到的商品
        SearchHits hits = response.getHits();
        SearchHit[] hitsHits = hits.getHits();

        List<SkuESModel> skus = new ArrayList<>();
        if (hitsHits != null && hitsHits.length > 0) {
            for (SearchHit hit : hitsHits) {
                String sourceAsString = hit.getSourceAsString();
                SkuESModel sku = JSON.parseObject(sourceAsString, SkuESModel.class);
                //设置高亮属性
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLight = skuTitle.getFragments()[0].string();
                    sku.setSkuTitle(highLight);
                }
                skus.add(sku);
            }
        }
        result.setProducts(skus);

        //=========上界：从aggregation中得到===========//
        Aggregations aggregations = response.getAggregations();
        // 2、涉及的所有属性
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = aggregations.get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket_attr_id : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // id
            long attrId = bucket_attr_id.getKeyAsNumber().longValue();
            // name
            String attrName = ((ParsedStringTerms) bucket_attr_id.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // List<String> 可能的值
            List<String> attrValues = ((ParsedStringTerms) bucket_attr_id.getAggregations().get("attr_value_agg")).getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);

        // 3、涉及的所有品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = aggregations.get("brand_agg");
        List<? extends Terms.Bucket> buckets_brand = brand_agg.getBuckets();
        for (Terms.Bucket bucket : buckets_brand) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // id
            long brandId = bucket.getKeyAsNumber().longValue();
            // name    => size:1
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            // img    => size:1
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        // 4、涉及的所有分类
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> buckets_catalog = catalog_agg.getBuckets();

        for (Terms.Bucket bucket : buckets_catalog) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // 分类id
            catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));
            // 分类名
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
        //==========下界：从aggregation中得到==========//


        // 5、分页信息 - 页码
        result.setPageNum(param.getPageNum());
        // 6、分页信息 - 总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 7、分页信息 - 总页码(need calc
        int totalPages = (int)total % ESConstant.PRODUCT_PAGE_SIZE == 0 ? (int)total / ESConstant.PRODUCT_PAGE_SIZE : ((int)total / ESConstant.PRODUCT_PAGE_SIZE) + 1;
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        // 6、面包屑导航
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");

                navVo.setNavValue(s[1]);
                try {
                    R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                    result.getAttrIds().add(Long.parseLong(s[0]));
                    if (r.getCode() == 0) {
                        AttrResponseVo attrResponseVo = JSON.parseObject(JSON.toJSONString(r.get("attr")), new TypeReference<AttrResponseVo>() {
                        });
                        navVo.setNavName(attrResponseVo.getAttrName());
                    }
                } catch (Exception e) {
                    log.error("远程调用商品服务查询属性失败", e);
                }

                // 取消面包屑
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.copymall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }

        // 品牌
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();

            navVo.setNavName("品牌");
            R r = productFeignService.brandsInfo(param.getBrandId());
            if (r.getCode() == 0) {
                List<SearchResult.BrandVo> brand = r.getData("brand", new TypeReference<List<SearchResult.BrandVo>>() {});

                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (SearchResult.BrandVo brandVo : brand) {
                    buffer.append(brandVo.getBrandName()).append(";");
                    replace = replaceQueryString(param, brandVo.getBrandId()+"", "brandId");

                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.copymall.com/list.html?" + replace);

            }
            navs.add(navVo);
        }

        // 分类


        return result;
    }

    private String replaceQueryString(SearchParam param, String value, String key) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
            encoded = encoded.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return param.get_queryString().replace("&"+key+"=" + encoded, "");
    }
}
