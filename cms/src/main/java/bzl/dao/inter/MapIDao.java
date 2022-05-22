package bzl.dao.inter;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface MapIDao<T> {
	int execute(String namespace,String id,Map<String,Object> map);
	int executeMulti(String namespace,String id,List<Map<String,Object>> list,HttpServletRequest request);
	int executeBatch(String namespace,String[] tlist,Map<String,List<Map<String,Object>>> list,HttpServletRequest request);
	int executeBatch(String[] tlist,Map<String,List<Map<String,Object>>> list,HttpServletRequest request);
	Object selectOne(String mapperspace,String id,Map<String,Object> map);
	Map<String,Object> selectOneMap(String mapperspace,String id,Map<String,Object> map);
	List<Map<String,Object>> selectList(String mapperspace,String id,Map<String,Object> map);
	public int save(String namespace,String id,Map<String,Object> map);
	public int save(String tablename,String id,String updateid,String insertid,Map<String, Object> map);
}
