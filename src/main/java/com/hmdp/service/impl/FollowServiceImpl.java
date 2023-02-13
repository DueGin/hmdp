package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();

        String key = "follow:"+userId;

        // 判断是关注还是取关
        if (isFollow) {
            // 关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if(isSuccess){
                // 把关注用户的id，放入redis的set集合 sadd userId followUserId
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }

        } else {
            // 取关，删除记录 delete from tb_follow where user_id = ? and follow_user_id = ?
            QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(Follow::getUserId, userId)
                    .eq(Follow::getFollowUserId, followUserId);
            boolean isSuccess = remove(queryWrapper);
            if(isSuccess){
                // 把取关用户的id从redis集合中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result isFollow(String id) {
        // 查询表中是否存在数据 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Long count = query().eq("user_id", UserHolder.getUser().getId())
                .eq("follow_user_id", id).count();

        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(String id) {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        String k1="follow:"+userId;
        String k2="follow:"+id;
        // 求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(k1, k2);

        if(intersect == null || intersect.isEmpty()){
            // 无交集
            return Result.ok(Collections.emptyList());
        }

        // 解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        // 查询用户
        List<UserDTO> dtoList = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(dtoList);
    }
}
