package bzl.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.ExamInfo;
import bzl.entity.ExamTask;
import bzl.entity.TaskInfo;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import sun.rmi.log.LogHandler;
import utils.HttpIO;

//考试信息处理设置 controller
@Controller
@RequestMapping("/exam")
public class ExamController {
	
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private int checkActionPermission(String uid,String exam_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("exam_id", exam_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> bootsettingList = ms.selectList("ExamInfo", "selectByCondition", conMap);
			if(bootsettingList!=null && bootsettingList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}else {
			conMap.put("creator_uid", uid);
			List<Map<String,Object>> bootsettingList = ms.selectList("ExamInfo", "selectByCondition", conMap);
			if(bootsettingList!=null && bootsettingList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}
	
	
	@RequestMapping(value="/exam_list",method=RequestMethod.POST)
	@ResponseBody
	public void getExamList(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		//int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			Map conMap = new HashMap<String,Object>();
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					conMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					tmpUserSet = new HashSet<String>();
					tmpUserSet.add(adminUser.getUid());
					conMap.put("creator_uids", tmpUserSet.toArray());
				}
			}
			
			List<Map<String,Object>> examInfolist = ms.selectList("ExamInfo", "selectByConditionWithPage", conMap);
			if(examInfolist != null && examInfolist.size() > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", examInfolist);
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/getExamInfoById",method=RequestMethod.POST)
	@ResponseBody
	public void getExamInfoById(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		//int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String exam_id = jsonBody.getString("exam_id");
			Map conMap = new HashMap<String,Object>();
			conMap.put("exam_id", exam_id);
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					conMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					tmpUserSet = new HashSet<String>();
					tmpUserSet.add(adminUser.getUid());
					conMap.put("creator_uids", tmpUserSet.toArray());
				}
			}
			
			List<Map<String,Object>> examInfolist = ms.selectList("ExamInfo", "selectByCondition", conMap);
			if(examInfolist != null && examInfolist.size() > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", examInfolist);
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	//增加考试信息
	@RequestMapping(value="/addOrUpdate",method=RequestMethod.POST)
	@ResponseBody
	public void addExamInfo(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String exam_id = jsonBody.getString("exam_id");
			String examCode = jsonBody.getString("exam_code");
			String examName = jsonBody.getString("exam_name");
			String startDate = jsonBody.getString("start_date");
			String endDate = jsonBody.getString("end_date");
			String AMstartTime = jsonBody.getString("am_start_time");
			String AMendTIme = jsonBody.getString("am_end_time");
			String PMstartTime = jsonBody.getString("pm_start_time");
			String PMendTIme = jsonBody.getString("pm_end_time");
			Integer countDownTime = jsonBody.getInteger("count_down");
			ExamInfo newExamInfo = new ExamInfo();
			if(examCode!=null) {
				newExamInfo.setExam_code(examCode);
			}
			if(examName!=null) {
				newExamInfo.setExam_name(examName);
			}
			if(startDate!=null) {
				newExamInfo.setStart_date(startDate);
			}
			if(endDate!=null) {
				newExamInfo.setEnd_date(endDate);
			}
			
			if(AMstartTime!=null) {
				newExamInfo.setAm_start_time(AMstartTime);
			}
			
			if(AMendTIme!=null) {
				newExamInfo.setAm_end_time(AMendTIme);
			}
			
			if(PMstartTime!=null) {
				newExamInfo.setPm_start_time(PMstartTime);
			}
			
			if(PMendTIme!=null) {
				newExamInfo.setPm_end_time(PMendTIme);
			}
		    
			if(countDownTime!=null) {
				newExamInfo.setCount_down(countDownTime);
			}
			if(exam_id !=null && exam_id.length() >0) {//如果参数带EXAM_ID 则为修改
				newExamInfo.setExam_id(exam_id);
				if(adminUser.getIs_supper()==0) {
					int hasSettPermission=checkActionPermission(adminUser.getUid(),exam_id);
					if(hasSettPermission==1) {
						result = es.update("ExamInfo", "update", newExamInfo);
					}
				}else {
					result = es.update("ExamInfo", "update", newExamInfo);
				}
				
				if(result>0) {
					Map conMap = new HashMap<String,Object>();
					conMap.put("exam_id", exam_id);
					conMap.put("sort", "create_time asc");
					List<Map<String,Object>> examTasklist = ms.selectList("ExamTask", "selectByCondition", conMap);
					if(examTasklist!=null && examTasklist.size() >0) {
						Map<String,Object> updateTaskInfo = new HashMap<String,Object>();
						for(int i=0;i<examTasklist.size();i++) {
							boolean bUpdate = false;
							updateTaskInfo.put("task_id",(String) examTasklist.get(i).get("task_id"));
							if(startDate!=null && startDate.length() >0) {
								updateTaskInfo.put("start_date",startDate);
								bUpdate = true;
							}
							if(endDate!=null && endDate.length()>0) {
								updateTaskInfo.put("end_date",endDate);
								bUpdate = true;
							}
							if(bUpdate) {
								result = es.update("TaskInfo", "update", updateTaskInfo);
							}else {
								System.out.println("222get exam task list task_id:" + bUpdate +":startDate="+startDate + ":endDate" + endDate);
							}
						}
					}
				}
				
			}else {//如果参数不带exam_id，则添加
				exam_id = "exam-" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(8);
				newExamInfo.setExam_id(exam_id);//生成新的id
				newExamInfo.setCreator(adminUser.getUsername());
				newExamInfo.setCreator_uid(adminUser.getUid());
				result = es.insert("ExamInfo", "insert", newExamInfo); 
			}
			if(result==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("exam_id", exam_id);
				respJson.put("msg", "操作成功!");
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "操作失败!可能是编号重复。");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		 HttpIO.writeResp(response, respJson);
	}
	
	
	//删除考试信息
	@RequestMapping(value="/del_exam",method=RequestMethod.POST)
	@ResponseBody
	public void delExamInfo(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray exam_ids = jsonBody.getJSONArray("exam_ids");
			if(adminUser.getIs_supper()==0) {
				for(int i=0;i<exam_ids.size();i++) {
					int hasSettPermission=checkActionPermission(adminUser.getUid(),exam_ids.getString(i));
					if(hasSettPermission==0) {
						respJson.put("status", Constant.FAILED);
						respJson.put("msg", Constant.ParemeterErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			for(int i=0;i<exam_ids.size();i++) {
				condMap.put("exam_id", exam_ids.get(i));
				List<Map<String, Object>> examList = ms.selectList("ExamTask", "selectByCondition", condMap);
				if(examList !=null && examList.size() >0) {
					Map<String,Object> delTaskIds = new HashMap<String,Object>();
					JSONArray taskIds = new JSONArray();
					for(int j=0;j<examList.size();j++) {
						taskIds.add(examList.get(j).get("task_id"));
					}
					delTaskIds.put("task_ids", taskIds);
					ms.execute("TaskInfo", "deleteBat", delTaskIds);
				}
			}
			condMap.clear();
			condMap.put("exam_ids", exam_ids);
			result = ms.execute("ExamInfo", "deleteBat", condMap);
			if(result >= 1) {
				ms.execute("ExamTask", "deleteBat", condMap);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "删除成功!");
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除失败!");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	//below is task templete api
	@RequestMapping(value="/getExamTpllist",method=RequestMethod.POST)
	@ResponseBody
	public void getExamTplList(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		//int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			Map conMap = new HashMap<String,Object>();
			
			List<Map<String,Object>> examInfolist = ms.selectList("ExamInfoTpl", "selectAll", conMap);
			if(examInfolist != null && examInfolist.size() > 0) {
				SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd");
				myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
				for(int i=0;i<examInfolist.size();i++) {
					examInfolist.get(i).put("start_date", myFmt.format(new Date()));
					examInfolist.get(i).put("end_date", myFmt.format(new Date()));
				}
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", examInfolist);
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/getTasktplByExamTplId",method=RequestMethod.POST)
	@ResponseBody
	public void getTasktplByExamTplId(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		//int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String exam_id = jsonBody.getString("exam_id");
			Map conMap = new HashMap<String,Object>();
			conMap.put("exam_id", exam_id);
			//List<Map<String,Object>> examInfolist = ms.selectList("ExamInfoTpl", "selectAll", conMap);
			
			List<Map<String,Object>> examInfolist = ms.selectList("ExamInfoTpl", "selectByCondition", conMap);
			if(examInfolist != null && examInfolist.size() > 0) {
				String allTaskIds = (String) examInfolist.get(0).get("templete_tasks");
				String[] allTaskIdList = allTaskIds.split(";");
				conMap.clear();
				conMap.put("task_ids", allTaskIdList);
				List<Map<String,Object>> examTasklist = ms.selectList("ExamTaskTpl", "selectByCondition", conMap);
				if(examTasklist!=null) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", examTasklist);
				}else {
					respJson.put("status", Constant.notExistStatus);
					respJson.put("msg", Constant.NodataErr);
				}
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/createExamFromTplId",method=RequestMethod.POST)
	@ResponseBody
	public void createExamFromTplId(HttpServletRequest request,HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String exam_id = jsonBody.getString("exam_id");
			Map conMap = new HashMap<String,Object>();
			conMap.put("exam_id", exam_id);
			List<Map<String,Object>> examInfolist = ms.selectList("ExamInfoTpl", "selectByCondition", conMap);
			if(examInfolist != null && examInfolist.size() > 0) {
				SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd");
				myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
				Map<String,Object> newExamInfo = examInfolist.get(0);
				String newExamId = "exam-" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(8);
				newExamInfo.put("exam_id", newExamId);
				newExamInfo.put("exam_code", RandomStringUtils.randomAlphanumeric(6));
				newExamInfo.put("start_date",myFmt.format(new Date()));
				newExamInfo.put("end_date",myFmt.format(new Date()));
				newExamInfo.put("creator",adminUser.getUsername());
				newExamInfo.put("creator_uid",adminUser.getUid());
				newExamInfo.put("exam_name", newExamInfo.get("exam_name") + myFmt.format(new Date()) + "-"+RandomStringUtils.randomAlphanumeric(5));
				newExamInfo.remove("id");
				result = es.insert("ExamInfo", "insert", newExamInfo); 
				if(result>0) {
					String allTaskIdList = (String) examInfolist.get(0).get("templete_tasks");
					if(allTaskIdList!=null && allTaskIdList.length() >0) {
						String[] tmpTaskIdList = allTaskIdList.split(";");
						conMap.clear();
						conMap.put("task_ids", tmpTaskIdList);
						conMap.put("sort", "id asc");
						List<Map<String,Object>> examTasklist = ms.selectList("ExamTaskTpl", "selectByCondition", conMap);
						if(examTasklist!=null) {
							for(int i=0;i<examTasklist.size();i++){
								Map<String,Object> newExamTask = examTasklist.get(i);
								System.out.println("the task type of:" + newExamTask.get("task_type"));
								String task_id = "taskid" + new Date().getTime();
								task_id = task_id + RandomStringUtils.randomAlphanumeric(32 - task_id.length()); // id总长度为32个字符
								newExamTask.remove("id");
								newExamTask.put("task_id", task_id);
								newExamTask.put("create_time", new Date());
								newExamTask.put("create_user", adminUser.getUsername());
								newExamTask.put("creator_uid", adminUser.getUid());
								newExamTask.put("start_date", myFmt.format(new Date()));
								newExamTask.put("end_date", myFmt.format(new Date()));
								result = es.insert("TaskInfo", "insert", newExamTask);
								if(result >0) {
									Map<String,Object> newExamTaskRelate = new HashMap<String,Object>();
									newExamTaskRelate.put("exam_id", newExamId);
									newExamTaskRelate.put("task_id", task_id);
									result =es.insert("ExamTask", "insert", newExamTaskRelate);
									if(result==0) {
										System.out.println("insert exam task relate failed!!!task_id="+task_id);
									}
								}
							}
							if(result >0) {
								examInfolist.clear();
								examInfolist.add(newExamInfo);
								respJson.put("status", Constant.SUCCESS);
								respJson.put("result", examInfolist);
							}else {
								respJson.put("status", Constant.FAILED);
								respJson.put("msg", Constant.FailedMsg);
							}
						}
					}
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.FailedMsg);
				}
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.FailedMsg);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
}
