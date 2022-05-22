package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis工具类
 * 
 * @author Logan
 * @version 1.0.0
 */
public class RedisUtils {

	private static JedisPool jedisPool = null;
	private static JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
	
	public static  void initRedisClient() {
		
		// 设置最大对象数
		jedisPoolConfig.setMaxTotal(20);

		// 最大能够保持空闲状态的对象数
		jedisPoolConfig.setMaxIdle(10);

		// 超时时间
		jedisPoolConfig.setMaxWaitMillis(10000);

		// 在获取连接的时候检查有效性, 默认false
		jedisPoolConfig.setTestOnBorrow(true);

		// 在返回Object时, 对返回的connection进行validateObject校验
		jedisPoolConfig.setTestOnReturn(false);

		jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379, 2000, "hkjdi40"); //在 /etc/redis/redis.conf配置文件里面改
//		jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379, 2000, "boyao");
	}
	
	private static Map<String,String> converJSONOBj2Map(JSONObject obj) {
		Map<String,String> resultMap = new HashMap<String,String>();
		if(obj != null) {
			for(String key: obj.keySet()) {
				resultMap.put(key, obj.get(key).toString());
			}
			return resultMap;
		}
		return null;
	}
	
	
	//模糊匹配key
	
	public static Set<String> getKeys(String prefixStr){
      try (Jedis jedis = jedisPool.getResource();) {
			return jedis.keys(prefixStr + "*");
		}
	}
	
	public static Long refreshExpired(String key,int expireSec) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.expire(key, expireSec);
		}
		
	}
	
	public static Long setExpire(String key,String value,int expireSec) {
		try (Jedis jedis = jedisPool.getResource();) {
			jedis.set(key, value);
			return jedis.expire(key, expireSec);
		}
	}

	/**
	 * 从连接池中获取一个Jedis对象
	 */
	public static Jedis getJedis() {
		if(jedisPool ==null) {
			initRedisClient();
		}
		return jedisPool.getResource();
	}

	/**
	 * 关闭Jedis对象，放回池中
	 */
	public static void closeJedis(Jedis jedis) {
		jedis.close();
	}

	/**
	 * 通过key获取String类型Value
	 * 
	 * @param key 键
	 * @return 值
	 */
	public static String get(String key) {
		if(key==null||key.length()==0) {
			return null;
		}
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.get(key);
		}
	}

	/**
	 * 通过key获取字节数组类型Value
	 * 
	 * @param key 键
	 * @return 值
	 */
	public static byte[] get(byte[] key) {
		try (Jedis jedis = jedisPool.getResource();) {

			return jedis.get(key);
		}
	}

	/**
	 * 设置String类型key和value
	 * 
	 * @param key   键
	 * @param value 值
	 * @return
	 */
	public static String set(String key, String value) {
		try (Jedis jedis = jedisPool.getResource();) {

			return jedis.set(key, value);
		}

	}

	/**
	 * 设置字节数组类型key和value
	 * 
	 * @param key   键
	 * @param value 值
	 * @return
	 */
	public static String set(byte[] key, byte[] value) {
		try (Jedis jedis = jedisPool.getResource();) {

			return jedis.set(key, value);
		}

	}

	/**
	 * 删除指定key
	 */
	public static Long del(String key) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.del(key);
		}
	}

	/**
	 * 左侧放入集合
	 * 
	 * @param key    键
	 * @param values 值集合
	 * @return
	 */
	public static Long lpush(String key, String... values) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.lpush(key, values);
		}
	}

	/**
	 * 左侧弹出一个元素
	 * 
	 * @param key 指定键
	 * @return 左侧第一个元素
	 */
	public static String lpop(String key) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.lpop(key);
		}
	}

	/**
	 * 右侧放入集合
	 * 
	 * @param key    键
	 * @param values 值集合
	 * @return
	 */
	public static Long rpush(String key, String... values) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.rpush(key, values);
		}
	}

	/**
	 * 右侧弹出一个元素
	 * 
	 * @param key 指定键
	 * @return 右侧第一个元素
	 */
	public static String rpop(String key) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.rpop(key);
		}
	}

	/**
	 * 集合操作
	 * 
	 * @param key
	 * @return 获取集合所有数据
	 */
	public static Set<String> allSetData(String key) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.smembers(key);
		}
	}

	

	/**
	 * 集合操作
	 * 
	 * @param key
	 * @return member是否存在集合中
	 */
	public static Boolean isExistMember(String key, String member) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.sismember(key, member);
		}
	}

	/**
	 * 集合操作
	 * 
	 * @param key
	 * @return 往集合中添加数据
	 */
	public static Long addMember(String key, String member,int expireSec) {
		if(key==null || member==null) {
			return null;
		}
		try (Jedis jedis = jedisPool.getResource();) {
			if(member !=null && member.length() >0) {
				jedis.sadd(key, member);
				if(expireSec > 0) {
					jedis.expire(key, expireSec);
				}
			}else {
				return null;
			}
		}
		return null;
	}

	/**
	 * 集合操作
	 * 
	 * @param key
	 * @return 从集合中删除成员
	 */
	public static Long removeMember(String key, String member) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.srem(key, member);
		}
	}

	/**
	 * 哈希操作
	 * 
	 * @param key
	 * @return 设置hash　k v
	 */
	public static Long hset(String key, Map<String, String> vmap, int expiredSec) {
		try (Jedis jedis = jedisPool.getResource();) {
			long result=jedis.hset(key, vmap);
			if(expiredSec > 0) {
				result = jedis.expire(key, expiredSec);
			}
			
			return result;
		}
	}
	
	
	/**
	 * 哈希操作
	 * 
	 * @param key
	 * @return 设置hash　k v
	 */
	public static Long hset(String key, JSONObject obj, int expiredSec) {
		try (Jedis jedis = jedisPool.getResource();) {
			Map<String,String> tmpMap = converJSONOBj2Map(obj);
			long result = jedis.hset(key, tmpMap);
			if(expiredSec > 0) {
				jedis.expire(key, expiredSec);
			}
			return result;
		}
	}
	
	/**
	 * 哈希操作
	 * 
	 * @param key
	 * @return 获取hash　某个字段
	 */
	public static String hget(String key,String field) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.hget(key, field);
		}
	}
	
	
	/**
	 * 哈希操作
	 * 
	 * @param key
	 * @return 获取hash　所有字段
	 */
	public static Map<String, String> hgetAll(String key) {
		try (Jedis jedis = jedisPool.getResource();) {
			return jedis.hgetAll(key);
		}
	}

}
