package bzl.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import utils.Column;


public interface MapService {
	 int execute(String classname,String id,Map<String,Object> map);
	 int executeMulti(String classname,String id,List<Map<String,Object>> list,HttpServletRequest request);
	 int executeBatch(String classname,String[] tlist,Map<String,List<Map<String,Object>>> map,HttpServletRequest request);
	 int executeBatch(String[] tlist,Map<String,List<Map<String,Object>>> List,HttpServletRequest request);
	 int executeBatch(String classname,String id,List<Map<String,Object>> list,HttpServletRequest request);
	 Object selectOne(String classname,String id,Map<String,Object> map);
	 List<Map<String,Object>> selectList(String classname,String id,Map<String,Object> map);
	 List<Map<String,Object>> selectList(String classname,String id,String[] item,Map<String,Object> map);
	 
//	 List<Map<String,Object>> selectLmtList(String classname,String id,Map<String,Object> map,List<String> item);
//	 Map<String,Object> selectOneMap(String classname,String id,Map<String,Object> map);
//	 Map<String,Object> getMapByPro(String procname,Map<String,Object> map);
//	 Map<String,Object> getMapPageByPro(String procname,Map<String,Object> map,String pagekey);
//	 Map<String,Object> getMapDoPage(String classname,String id,String countid,Map<String,Object> map);
//	 Map<String,Object> getMapWithPage(String classname,String id,String countid,Map<String,Object> map);
//	 List<String> selectListforTree(String classname,String id,Map<String,Object> map);
//	 Map<String,Object> getMapByProDoPage(String procname,Map<String,Object> map,String pagekey);
//	 Map<String,Object> getMySqlMapPageByPro(String procname,Map<String,Object> map);
//	 Map<String,Object> getMySqlMapByPro(String procname,Map<String,Object> map);
	 
	 /**
	  * ���ͳ����Ϣ
	  * @param tablename
	  * @param map
	  * @param sumlist
	  * @return
	  */
//	 public Map<String,Object> TableselectTj(String tablename,Map<String, Object> map,String sumlist,String sumhz1);
//	 /**
//	  * 	 ��ñ��ѯ�б�
//	  */
//	 List<Map<String,Object>> selectTableList(String tablename,Map<String,Object> map);
//	 /**
//	  * 	 ��ñ��ѯ�б�
//	  */
//	 List<Map<String,Object>> selectOrTableList(String tablename,Map<String,Object> map);
//	 /**
//	  * 	 ��ñ��ѯ�б�
//	  */
//	 List<Column> getTableItem(String tablename);
	 /**
	  * ������
	  * @param tablename
	  * @param map
	  * @return
	  */
//	 public int TableCreate(String tablename,Map<String, Object> map);
//	 public int Tabledelete(String tablename,Map<String, Object> map);
//	 public int Tableupdate(String tablename,Map<String, Object> map);
//	 public int TableInsert(String tablename,Map<String, Object> map);
//	 public int TableselectCount(String tablename,Map<String, Object> map);
//	 /**
//	  * ��Excel����������ݵ���ݿ�
//	  * @param tablename
//	  * @param list
//	  * @return
//	  */
//	public int InsertBat(String tablename,List<Map<String, Object>> list);
//	
//	/**
//	 * sql分页查询
//	 * <p>create or modify by 龙光磊 [2016年10月26日 上午9:31:41]</p>
//	 * @param sql
//	 * @param page
//	 * @param rows
//	 * @param params
//	 * @return
//	 */
//	public List<Map<String, Object>> getListBySql(String sql, Integer page, Integer rows, Object...params);
//	/**
//	 * sql查询结果集
//	 * <p>create or modify by 龙光磊 [2016年10月26日 上午9:31:56]</p>
//	 * @param sql
//	 * @param params
//	 * @return
//	 */
//	public List<Map<String, Object>> getListBySql(String sql, Object...params) ;
//	/**
//	 * sql查询单一结果
//	 * <p>create or modify by 龙光磊 [2016年10月26日 上午9:32:06]</p>
//	 * @param sql
//	 * @param params
//	 * @return
//	 */
//	public Map<String, Object> getResultBySql(String sql, Object...params);
//	/**
//	 * 执行sql更新
//	 * <p>create or modify by 龙光磊 [2016年10月26日 上午9:50:13]</p>
//	 * @param sql
//	 * @param params
//	 * @return
//	 */
//	public Integer updateBySql(String sql, Object ...params);
//	
//	public int TableupdateByU(String tablename,Map<String, Object> map);
//	/**
//	 * 如果有就更新，没有就新增
//	 * @param tablename
//	 * @param map
//	 * @return
//	 */
//	public int save(String tablename,String id,Map<String, Object> map);
//	public int save(String tablename,String id,String updateid,String insertid,Map<String, Object> map);
}
