
-- 比较线程中标识与锁标识是否一致
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 一致则删除锁
    return redis.call('del', KEYS[1])
end
    -- 不一致则返回0
return 0
