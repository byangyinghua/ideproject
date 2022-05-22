package bzl.dao.inter;  
  
import java.util.List;  
import java.util.Map;

import org.springframework.stereotype.Component;
import utils.ResultModel;

public interface EntityIDao<T> {
	
    List<?> select(String tablename,String id);
    List<?> select(String tablename,String id,Map<String,Object> map);
    List<?> select(String classname, String id, T object);
    Object selectOne(String mapperspace,String id);
    Object selectOne(String mapperspace,String id,Map<String,Object> map);
  
    int insert(String classname,String id,T object);
    int update(String classname,String id,T object);
    int delete(String classname,String id,T object);
    
    int doBatch(String classname,String operator,List<T> list);
    int doBatch(String[] classlist,String operlist[],List<ResultModel> list);
    int doThing(String classname);
    List<?> dosomething(String mapperspace, String id);
 
}