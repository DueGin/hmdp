package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

public interface IFollowService extends IService<Follow> {

    /**
     * 关注/取关
     * @param followUserId 被操作者id
     * @param isFollow 关注/取关
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 是否关注此id
     * @param id 判断的id
     * @return 是否关注
     */
    Result isFollow(String id);

    Result followCommons(String id);
}
