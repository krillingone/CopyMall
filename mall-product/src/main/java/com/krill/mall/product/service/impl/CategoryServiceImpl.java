package com.krill.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.krill.common.utils.PageUtils;
import com.krill.common.utils.Query;
import com.krill.mall.product.dao.CategoryDao;
import com.krill.mall.product.entity.CategoryEntity;
import com.krill.mall.product.service.CategoryBrandRelationService;
import com.krill.mall.product.service.CategoryService;
import com.krill.mall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        // 组装 父子的树形列表
        //    一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    // return like [2, 34, 225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();

        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联数据
     * @param category
     */
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }

    @Override
    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    @Cacheable(value = {"category"}, key = "#root.method.name")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

        // 优化，直接一次查出所有，然后一次次的挑，而不是多次数据库，多次数据库是很耗时的
        System.out.println("查询了数据库 ==》 getCatalogJson");
        List<CategoryEntity> categoryList = baseMapper.selectList(null);


        // 1.查所有1级分类
        List<CategoryEntity> level1Categorys = getChildrenListByParentCid(categoryList, 0L);
        // 2.封装
        Map<String, List<Catelog2Vo>> categorysMap = level1Categorys.stream()
                .collect(Collectors.toMap(
                        k -> k.getCatId().toString(),
                        v -> {
                            // 查此一级的二级分类
                            List<CategoryEntity> level2Catelog = getChildrenListByParentCid(categoryList, v.getCatId());
                            // 封装查到的二级分类
                            List<Catelog2Vo> catelog2Vos = null;
                            if (level2Catelog != null) {
                                catelog2Vos = level2Catelog.stream().map(l2 -> {
                                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                                    // 找这个二级分类的三级分类
                                    List<CategoryEntity> level3Catelog = getChildrenListByParentCid(categoryList, l2.getCatId());
                                    // 封
                                    if (level3Catelog != null) {
                                        List<Object> catelog3Vos = level3Catelog.stream().map(l3 -> new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName())).collect(Collectors.toList());
                                        catelog2Vo.setCatalog3List(catelog3Vos);
                                    }

                                    return catelog2Vo;
                                }).collect(Collectors.toList()) ;

                            }
                            return catelog2Vos;

                        }));
        return categorysMap;
    }

    /**
     * 通过父cid找到其子菜单list
     * @param categoryList 查到的所有的category
     * @param parentCid 父cid
     * @return 子菜单项的list
     */
    private List<CategoryEntity> getChildrenListByParentCid(List<CategoryEntity> categoryList, Long parentCid) {
        return categoryList.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        // 1.收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            // 2.递归找老爸
            findParentPath(byId.getParentCid(), paths);
        }

        return paths;
    }

    // 递归查找所有的菜单的子项
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        return all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {    // 递归找到子项的子项，一步到位
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {    // 排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());
    }

}