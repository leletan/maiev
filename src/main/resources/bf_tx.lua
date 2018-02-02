--[[
Redis script that updates a bloom filter with
the value specified. If the bloom filter does
not exist it is created.
sha: 257824595b292d3a6d16be5833c9882dd10bd5dc
--]]

do
    local filter = KEYS[1]
    local value = KEYS[2]
    local ttl_from_client = tonumber(KEYS[3])
    local err_rate = KEYS[4]
    local reserve_quota = KEYS[5]

    local reserve = "NOT_CALLED"
    local add = 0
    local expire = 0

    if redis.pcall("EXISTS", filter) == 0 then
        reserve = redis.pcall("BF.RESERVE", filter, err_rate, reserve_quota)
    end

    add = redis.pcall("BF.ADD", filter, value)

    local existing_ttl = redis.pcall("TTL", filter)
    if existing_ttl < ttl_from_client then
        expire = redis.pcall("EXPIRE", filter, ttl_from_client)
    end

    return {reserve, add, expire}
end