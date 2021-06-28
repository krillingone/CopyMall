package com.krill.mall.product;

import com.krill.mall.product.dao.AttrGroupDao;
import com.krill.mall.product.entity.BrandEntity;
import com.krill.mall.product.service.BrandService;
import com.krill.mall.product.service.CategoryService;
import com.krill.mall.product.service.SkuSaleAttrValueService;
import com.krill.mall.product.vo.SpuItemGroupAttrVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Test
    public void test() {
        List<SpuItemGroupAttrVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(100L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }

    @Test
    public void testFindPath() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径：{}", Arrays.asList(catelogPath));
    }

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("KRILL");

        brandService.save(brandEntity);
        System.out.println("Succeed!");
    }

}
