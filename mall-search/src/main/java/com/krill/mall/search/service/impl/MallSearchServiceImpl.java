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

        // ????????????
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            // ??????
            SearchResponse response = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);

            // ???????????????????????????
            searchResult = buildSearchResult(response, param);
//            System.out.println(searchResult);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return searchResult;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        /**?????????????????????????????????????????????????????????????????????????????????*/
        // #1 bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // #1.1 bool - must
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // #1.2 bool - filter
        // #1.2.1 bool - filter - ????????????
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // #1.2.2 bool - filter - ??????id
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // #1.2.3 bool - filter - ??????????????????    attrs=1_5???:8???&attrs=2_16G:8G
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();

                String[] s = attr.split("_");
                String attrId = s[0];
                String[] attrValue = s[1].split(":");

                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));

                // ???????????????????????????nestedQuery???????????????boolQuery??????
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // #1.2.4 bool - filter - ???????????????
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        // #1.2.5 bool - filter - ????????????    _500;1_500;500_
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] split = param.getSkuPrice().split("_");
            if (split.length == 2) {    // _500??????split???[null, 500]
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(split[1]);

                } else {
                    rangeQuery.gte(split[0]).lte(split[1]);
                }
            } else if (split.length == 1) {    // split????????????1???????????????100_
                rangeQuery.gte(split[0]);
            }
            boolQuery.filter(rangeQuery);
        }

        // ????????????
        sourceBuilder.query(boolQuery);

        /**????????????????????????*/
        // ??????
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] split = sort.split("_");
            sourceBuilder.sort(split[0], split[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }
        // ??????   (pageNum - 1) * size
        sourceBuilder.from((param.getPageNum() - 1) * ESConstant.PRODUCT_PAGE_SIZE);
        sourceBuilder.size(ESConstant.PRODUCT_PAGE_SIZE);

        // ??????   ??????keyword?????????
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();

            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            sourceBuilder.highlighter(highlightBuilder);
        }

        /**????????????*/
        // 1???????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // ????????????????????????name, img
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);    //=== ???????????? brand ===//

        // 2???????????????
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        // ?????????????????? name
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);    //=== ???????????? catalog ===//

        // 3???????????????
        /*---- nested_attr_agg
                  |
                  attr_id_agg
                     |
                     attr_name_agg
                     attr_value_agg

         */
        NestedAggregationBuilder nested_attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // ???????????????
        // ??????????????????
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(10);
        // attr?????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // attr????????????????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        nested_attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(nested_attr_agg);    //=== ???????????? attr ===//


        String dsl = sourceBuilder.toString();
        System.out.println("?????????DSL" + dsl);

        SearchRequest searchRequest = new SearchRequest(new String[]{ESConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }

    /**
     * ??????????????????
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1?????????????????????
        SearchHits hits = response.getHits();
        SearchHit[] hitsHits = hits.getHits();

        List<SkuESModel> skus = new ArrayList<>();
        if (hitsHits != null && hitsHits.length > 0) {
            for (SearchHit hit : hitsHits) {
                String sourceAsString = hit.getSourceAsString();
                SkuESModel sku = JSON.parseObject(sourceAsString, SkuESModel.class);
                //??????????????????
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLight = skuTitle.getFragments()[0].string();
                    sku.setSkuTitle(highLight);
                }
                skus.add(sku);
            }
        }
        result.setProducts(skus);

        //=========????????????aggregation?????????===========//
        Aggregations aggregations = response.getAggregations();
        // 2????????????????????????
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = aggregations.get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket_attr_id : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // id
            long attrId = bucket_attr_id.getKeyAsNumber().longValue();
            // name
            String attrName = ((ParsedStringTerms) bucket_attr_id.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // List<String> ????????????
            List<String> attrValues = ((ParsedStringTerms) bucket_attr_id.getAggregations().get("attr_value_agg")).getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);

        // 3????????????????????????
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

        // 4????????????????????????
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> buckets_catalog = catalog_agg.getBuckets();

        for (Terms.Bucket bucket : buckets_catalog) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // ??????id
            catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));
            // ?????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
        //==========????????????aggregation?????????==========//


        // 5??????????????? - ??????
        result.setPageNum(param.getPageNum());
        // 6??????????????? - ????????????
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 7??????????????? - ?????????(need calc
        int totalPages = (int)total % ESConstant.PRODUCT_PAGE_SIZE == 0 ? (int)total / ESConstant.PRODUCT_PAGE_SIZE : ((int)total / ESConstant.PRODUCT_PAGE_SIZE) + 1;
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        // 6??????????????????
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
                    log.error("??????????????????????????????????????????", e);
                }

                // ???????????????
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.copymall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }

        // ??????
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();

            navVo.setNavName("??????");
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

        // ??????


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
