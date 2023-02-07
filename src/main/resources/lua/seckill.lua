-- 1. 参数列表
-- 1.1 优惠券id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]
-- 1.2 订单id
local orderId = ARGV[3]

-- 2. 数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId -- 拼接字符串用“..”拼接
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3. 脚本业务
-- 是否存在此key
if redis.call('EXISTS', stockKey) == 0 then
    return 2
end
-- 3.1 判断库存是否充足 get stockKey
if tonumber(redis.call('get', stockKey)) <= 0 then
    -- 3.2 库存不足，返回1
    return 1
end
-- 3.2 判断用户是否下单 sismember orderKey userId
if redis.call('sismember', orderKey, userId) == 1 then
    --3.3 存在，说明重复下单，返回2
    return 2
end
-- 3.4 扣减库存
redis.call('incrby', stockKey, -1)
-- 3.5 下单（保存用户）
redis.call('sadd', orderKey, userId)
-- 3.6 发送消息到队列中，xadd stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
-- 成功，返回0
return 0