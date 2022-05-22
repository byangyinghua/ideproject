package bzl.common;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * @Title: WeatherCache
 * @ProjectName leadeon-third
 * @Description: 常用数据缓存工具
 * @author: 
 * @date: 2019/3/14 17:25
 * @version: V1.0
 */
public class MemoryCache {
 
    // 容器
    private  Map<String, CacheData> CACHE_DATA = new ConcurrentHashMap<>();
 
    
    /**
     * @throws
     * @Description: 获取所有存储的数据
     * @param: [key, load, expire]
     * @return: T
     * @author: 
     * @date: 
     */
    
    public Set<String> getAllDataKeys() {
    	return CACHE_DATA.keySet();
    }
    
    
    /**
     * @throws
     * @Description: 根据key获取数据（数据可再处理）
     * @param: [key, load, expire]
     * @return: T
     * @author: 
     * @date: 
     */
    public  <T> T getData(String key, Load<T> load, int expire) {
        T data = getData(key);
        if (data == null && load != null) {
            data = load.load();
            if (data != null) {
                setData(key, data, expire);
            }
        }
        return data;
    }
 
    /**
     * @throws
     * @Description: 根据key获取数据（数据不可再处理）
     * @param: [key]
     * @return: T
     * @author: 
     * @date: 2019/7/23 9:47
     */
    public  <T> T getData(String key) {
        CacheData<T> data = CACHE_DATA.get(key);
        
        // 数据未过期则返回
        if (data != null && (data.getExpire() <= 0 || data.getSaveTime() >= new Date().getTime())) {
            return data.getData();
        } else {// 数据过期则清除key
            clear(key);
        }
        return null;
    }
 
    /**
     * @throws
     * @Description: 获取集合大小
     * @author: 
     * @date: 2019/7/23 9:47
     */
    public  int getDataSize() {
        return CACHE_DATA.size();
    }
 
    /**
     * @throws
     * @Description: 新增缓存数据
     * @param: [key, data, expire 是秒数]
     * @return: void
     * @author: 
     * @date: 2019/7/23 9:47
     */
    public  <T> void setData(String key, T data, int expire) {
        CACHE_DATA.put(key, new CacheData(data, expire));
    }
 
    /**
     * @throws
     * @Description: 根据key删除数据
     * @param: [key]
     * @return: void
     * @author: 
     * @date: 2019/7/23 9:47
     */
    public  void clear(String key) {
        CACHE_DATA.remove(key);
    }
 
    /**
     * @throws
     * @Description: 清空缓存容器
     * @param: []
     * @return: void
     * @author: 
     * @date: 2019/7/23 9:47
     */
    public  void clearAll() {
        CACHE_DATA.clear();
    }
 
    /**
     * @Description: 内部接口：缓存数据再处理功能
     * @param:
     * @return:
     * @throws
     * @author: 
     * @date: 2019/3/21 11:02
     */
    public interface Load<T> {
        T load();
    }
 
    /**
     * @Description: 缓存数据实体
     * @param:
     * @return:
     * @throws
     * @author: 
     * @date: 2019/7/23 9:48
     */
    private  class CacheData<T> {
        private T data;
        private long saveTime; // 存活时间
        private long expire;   // 过期时间 小于等于0标识永久存活
 
        CacheData(T t, int expire) {
            this.data = t;
            this.expire = expire <= 0 ? 0 : expire * 1000;
            this.saveTime = new Date().getTime() + this.expire;
        }
        public T getData() {
            return data;
        }
        public long getExpire() {
            return expire;
        }
        public long getSaveTime() {
            return saveTime;
        }
    }
}