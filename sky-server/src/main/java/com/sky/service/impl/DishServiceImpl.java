package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.StatusSetException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DIshFlavorService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.sky.constant.StatusConstant.DISABLE;

@Service
public class DishServiceImpl implements DishService {

    private static final String CACHE_NAME = "dish-cache";

    @Autowired
    DishMapper dishMapper;
    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    DishFlavorMapper dishFlavorMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    DIshFlavorService dishFlavorService;

    @Transactional
    @Override
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setStatus(DISABLE);
        dishMapper.insert(dish);
        Long dishId = dish.getId();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            dishFlavorMapper.insert(flavors);
        }

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.put(dishId, dishVO);
        } else {
            ValueOperations valueOps = redisTemplate.opsForValue();
            String nameAndKey = CACHE_NAME + "::" + dishId;
            valueOps.set(nameAndKey, dishVO);
        }

    }

    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        int page = dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();
        Dish dish = new Dish();
        PageHelper.startPage(page, pageSize);
        BeanUtils.copyProperties(dishPageQueryDTO, dish);
        Page<DishVO> dishes = (Page<DishVO>) dishMapper.get(dish);
        return new PageResult(dishes.getTotal(), dishes.getResult());
    }

    @Transactional
    @Override
    public void batchDelete(List<Long> ids) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        List<Integer> statuses = dishMapper.getStatusByIds(ids);
        for (Integer status : statuses) {
            if (status == null) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_NOT_EXIST);
            }
            if (Objects.equals(status, StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        List<Long> setmealDishes = setmealDishMapper.getByDishId(ids);
        if (!setmealDishes.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        if (cache != null) {
            for (Long id : ids) cache.evict(id);
        }
        dishMapper.batchDelete(ids);
        dishFlavorMapper.batchDelete(ids);
    }

    @Override
    @Cacheable(cacheNames = CACHE_NAME, key = "#id")
    public DishVO getById(Long id) {
        DishVO dishVO = dishMapper.get(Dish.builder().id(id).build()).get(0);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getById(id);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, key = "#dishDTO.id")
    public void update(DishDTO dishDTO) {
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long id = dishDTO.getId();
        if (flavors != null && !flavors.isEmpty()) {
            //删除后再添加//
            //删除//
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            dishFlavorMapper.batchDelete(ids);
            //添加//
            flavors.forEach(
                    flavor -> flavor.setDishId(id)
            );
            dishFlavorMapper.batchInsert(flavors);
        }
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> dishes = dishMapper.getByCategory(categoryId);
        return dishes;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, key = "#id")
    public void status(Long id, Integer status) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        Integer preStatus = dishMapper.getStatusByIds(ids).get(0);
        if (preStatus == status) {
            throw new StatusSetException("状态设置错误");
        }
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.update(dish);
    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {

        List<DishVO> dishVOs = dishMapper.get(dish);
        for (DishVO d : dishVOs) {
            d.setFlavors(dishFlavorService.getDishFlavors(d.getId()));
        }
        return dishVOs;
    }

}


