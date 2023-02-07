package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.ShopType;

import java.util.List;

public interface IShopTypeService extends IService<ShopType> {
    List<ShopType> getList();
}
