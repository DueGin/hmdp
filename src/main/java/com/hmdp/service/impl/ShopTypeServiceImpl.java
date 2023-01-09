package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> getList() {
        // 由于商铺类型都是固定的，所以通用的key就行
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        // 1.查询redis
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(key, 0, -1);

        // 2. 判断是否存在
        if (shopTypeJsonList != null && !shopTypeJsonList.isEmpty()) {
            // 3. 存在则返回
            return shopTypeJsonList.stream()
                    .map(shopType -> JSONUtil.toBean(shopType, ShopType.class))
                    .collect(Collectors.toList());
        }

        // 4. 不存在则查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        // 5. 存入redis
        List<String> list = shopTypeList.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(key, list);
        stringRedisTemplate.expire(key, 1, TimeUnit.DAYS);

        return shopTypeList;
    }
}
