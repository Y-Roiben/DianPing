-- 1.参数列表
-- 1.1 优惠券id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]

-- 2. 数据key
-- 2.1 库存key
local stockKey = "seckill:stock:" .. voucherId
-- 2.2 订单key
local OrderKey = "seckill:order:" .. voucherId

-- 3. 业务逻辑
-- 3.1 判断库存是否大于0
if (tonumber(redis.call("get", stockKey)) <= 0) then
    return 1;
end
-- 3.2 判断用户是否已经抢购过
if (redis.call("sismember", OrderKey, userId) == 1) then
    -- 已经抢购过
    return 2;
end
-- 3.3 减库存
redis.call("incrby", stockKey, -1)
-- 3.4 加订单
redis.call("sadd", OrderKey, userId)
return 0;