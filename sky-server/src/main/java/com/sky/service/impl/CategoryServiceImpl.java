package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);
        categoryMapper.insert(category);
    }

    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    public void deleteById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        Integer count = dishMapper.countByCategoryId(id);
        if (count > 0) {
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        count = setmealMapper.countByCategoryId(id);
        if (count > 0) {
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
    }

    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        categoryMapper.update(category);
    }

    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.update(category);
    }


    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }
}
