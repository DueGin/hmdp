-- 锁的key
local key = KEYS[1]
-- 当前线程标识
local threadId = ARGV[1]

-- 获取锁中的线程标识 get key
local id = redis.call('get', KEYS[1])
-- 比较线程标识与锁中的标识是否一致
if id == threadId then
    -- 释放锁 del key
    return redis.call('del', key)
end
return 0