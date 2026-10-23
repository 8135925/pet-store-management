package com.petshop.module.system;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 门店参数读取（带缓存，参数页保存后刷新缓存）。 */
@Service
public class ConfigService {

    private final StoreConfigMapper mapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ConfigService(StoreConfigMapper mapper) {
        this.mapper = mapper;
    }

    public String get(String key, String defVal) {
        String v = cache.get(key);
        if (v == null) {
            v = mapper.findValue(key);
            if (v == null) {
                v = defVal;
            }
            cache.put(key, v);
        }
        return v;
    }

    public boolean getBool(String key, boolean defVal) {
        return Boolean.parseBoolean(get(key, String.valueOf(defVal)));
    }

    public int getInt(String key, int defVal) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defVal)).trim());
        } catch (Exception e) {
            return defVal;
        }
    }

    public String storeName() {
        return get("store_name", "宠物店管理系统");
    }

    public boolean giftFirst() {
        return getBool("gift_first", true);
    }

    public int expireWarnDays() {
        return getInt("expire_warn_days", 30);
    }

    public int lowStockWarn() {
        return getInt("low_stock_warn", 3);
    }

    public void save(Map<String, String> kv) {
        for (Map.Entry<String, String> e : kv.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            mapper.upsert(e.getKey().trim(), e.getValue().trim());
        }
        cache.clear();
    }
}
