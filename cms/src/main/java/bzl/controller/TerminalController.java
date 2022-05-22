package bzl.controller;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utils.*;
//import net.sf.json.JSONObject;
import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.Terminal;
import bzl.entity.TerminalGroup;
import bzl.entity.TerminalUpdate;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.service.impl.JDBCTransaction;
import bzl.task.SocketMsgHandler;
import bzl.task.cmds;
import sun.rmi.log.LogHandler;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
//import utils.StringUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
//import org.system.socket.TaskSocket;
//import org.system.socket.ver2.SocketConnection;
//import org.system.socket.ver2.SocketServer;
//import utils.NowTime;
//import utils.Writer;
import org.xnx.sql.util.SQLTools;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ibm.icu.text.DateFormat;

@Controller
@RequestMapping("/terminal")
public class TerminalController {
	
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	
	private String checkTerminalType(String terminalId) {

        if(terminalId.substring(3,5).indexOf("02")!=-1){
        	return "广播终端";
        }else if(terminalId.substring(3,5).indexOf("03")!=-1){
            return "音箱终端";
        }else if(terminalId.substring(3,5).indexOf("04")!=-1){
            return "LED大屏";
        }else{
            return "普通终端";
        }
	}
	
	//true　表示类型一样，false表示不一样
	private boolean compareGroupType(String from_gid,String to_gid) {
		Map<String,Object> condMap = new HashMap<String,Object>();
		boolean result = true;
		
		if(from_gid==null||from_gid.length()==0||to_gid==null||to_gid.length()==0) {
			return result;
		}

		if(from_gid !=null && from_gid.length() >0) {
			condMap.put("gid", from_gid);
			List<Map<String,Object>> groulist = ms.selectList("TerminalGroup", "selectByPK", condMap);
			if(groulist!=null && groulist.size()==1) {
				condMap.clear();
				condMap.put("gid", to_gid);	
				String fromGroupType = (String) groulist.get(0).get("group_type");
				if(fromGroupType!=null && fromGroupType.length() >0) {
					List<Map<String,Object>> groulist1 = ms.selectList("TerminalGroup", "selectByPK", condMap);
					if(groulist1!=null && groulist1.size()==1) {
						String toGroupType = (String) groulist1.get(0).get("group_type");
						if(toGroupType != null && toGroupType.length() >0 && !fromGroupType.equals(toGroupType)) {
							System.out.println("to group groulist222222:" + groulist1.toString());
							result =  false;
						}
					}
				}
			}
		}
		
		return result;
	}
	
	@RequestMapping(value ="/group/list",method=RequestMethod.POST)
	public void getGroupList(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map conMap = new HashMap<String,Object>();
			List<Map<String,Object>> groulist = null;
			if(adminUser.getIs_supper()==1) {
				if(getTotal !=null) {
					List<Map<String,Object>> totalList= ms.selectList("TerminalGroup", "selectCountByCondition", conMap);
					if(totalList !=null && totalList.size()==1) {
						respJson.put("total", totalList.get(0).get("count"));
					}
				}
				conMap.put("startrom", (page-1)*pagesize);
				conMap.put("pagesize",pagesize);
				conMap.put("sort", "update_time desc");
				groulist = ms.selectList("TerminalGroup", "selectByConditionWithPage", conMap);
			}else {
				Set<String> terminalGroups = UserGroupController.getGroupListByUser(adminUser.getUid());
				for(String gid:terminalGroups) {
					conMap.put("gid", gid);
					List<Map<String,Object>> tmpgroulist = ms.selectList("TerminalGroup", "selectByPK", conMap);
					if(groulist==null) {
						groulist = tmpgroulist;
					}else {
						groulist.addAll(tmpgroulist);
					}
				}
				if(terminalGroups!=null) {
					respJson.put("total", terminalGroups.size());
				}
			}
			if(groulist != null && groulist.size() > 0) {
				groulist = Convert.SortDataListId(groulist,page,pagesize);
				for(int i=0;i<groulist.size();i++) {
					conMap.clear();
					conMap.put("gid", groulist.get(i).get("gid"));
					int totalCnt = 0;
					List<Map<String,Object>> totalList = ms.selectList("Terminal", "countTerminalByGid", conMap);
					if(totalList !=null && totalList.size()==1) {
						totalCnt = Integer.parseInt((String) totalList.get(0).get("count"));
						conMap.put("terminal_cnt", totalCnt);
						result = ms.execute("TerminalGroup", "update", conMap);
						conMap.remove("terminal_cnt");
					}
					
					int onlineCnt = 0;
					//int totalCnt = Integer.parseInt((String) groulist.get(i).get("terminal_cnt"));
					if(totalCnt >0) {
						List<Map<String,Object>> devlist = (List<Map<String,Object>>)ms.selectList("Terminal", "selectByCondition", conMap);
						for(int j=0;j<devlist.size();j++) {
							String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) devlist.get(j).get("terminal_id"));
							if(null != onlineInfo) {
								String state = onlineInfo.split(":")[1];
								if(Integer.parseInt(state) >0) {
									onlineCnt++;
								}
							}
						}
					}
					groulist.get(i).put("onlineCnt", onlineCnt);
				}
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", groulist);
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
	
	//增加终端分组
	
	@RequestMapping(value="/group/addOrUpdate",method=RequestMethod.POST)
	public void addGroup(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, true); 
		if(adminUser != null && adminUser.getIs_supper()==1) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			TerminalGroup terminalGrp = new TerminalGroup();
			String group_id = jsonBody.getString("gid");
			//这里去掉编号，为了不影响数据库，直接用ID填充.
//			terminalGrp.setGroup_code(jsonBody.getString("group_code"));
			terminalGrp.setGroup_name(jsonBody.getString("group_name"));
			
			if(group_id != null && group_id.length() >0) {
				terminalGrp.setGid(jsonBody.getString("gid"));
				terminalGrp.setGroup_code(terminalGrp.getGid());
				result = es.update("TerminalGroup", "update", terminalGrp);
			}else {
				terminalGrp.setGid("gid" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(6));
				terminalGrp.setGroup_code(terminalGrp.getGid());
				terminalGrp.setAdd_uid(adminUser.getUid());
				result = es.insert("TerminalGroup", "insert", terminalGrp);
			}
			
			if(result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
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
	
	
	
	//删除终端分组信息
	@RequestMapping(value="/group/del",method=RequestMethod.POST)
	public void delGroupsInfo(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, true); 
		if(adminUser !=null && adminUser.getIs_supper()==1) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			JSONArray gids = jsonBody.getJSONArray("gids");
			Map<String,Object> condMap = new HashMap<String,Object>();
			
			TerminalGroup terminalGrp = new TerminalGroup();
		    if(gids != null && gids.size() >0) {
		    	//JSONArray successList = new JSONArray();
		    	for(int i=0;i<gids.size();i++) {
		    		String tmpGid = gids.getString(i);
		    		terminalGrp.setGid(tmpGid);
		    		result = es.delete("TerminalGroup", "delete", terminalGrp);
		    		if(result == 1 ) {
		    			condMap.clear();
		    			condMap.put("gid", tmpGid);
		    			List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByGroupId", condMap);
		    			if(terminalList != null && terminalList.size() >0) {
		    				for(int n =0;n < terminalList.size();n++) {
		    					Map<String,Object> tmpTerminal = terminalList.get(n);
		    					String terminalGids = (String) tmpTerminal.get("gids");
		    					terminalGids = terminalGids.replace("," + tmpGid, "").replace(tmpGid + ",", "").replace(tmpGid, "");
		    					tmpTerminal.put("gids", terminalGids);
		    					result = ms.execute("Terminal", "update", tmpTerminal);
		    				}
		    			}
		    		}
		    	}
		    }
	    	if(result == 1) {
	    		respJson.put("status", Constant.SUCCESS);
	    		respJson.put("msg", Constant.SuccessMsg);
	    		
	    		//下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionDel);
				JSONObject content = new JSONObject();
				content.put("action_name", "删除终端分组");
				content.put("gids",  gids);
				
				actionLog.put("action_content",content.toJSONString());
				es.insert("UserLog", "insert", actionLog);
				
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
	
	@RequestMapping(value="/change_group",method=RequestMethod.POST)
	public void changeGroup(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, true); 
		if(adminUser != null && adminUser.getIs_supper()==1) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String changeType = jsonBody.getString("change_type"); // copy 是复制到分组，move是移动到分组
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			String from_gid = jsonBody.getString("from_gid");
			String to_gid = jsonBody.getString("to_gid");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			String terminalType = "";
			
			if(from_gid!=null && from_gid.equals("all-group")) {
				from_gid = "";
			}
			
			if(compareGroupType(from_gid,to_gid)) {//只有设备类型一样的组，或者未分组的设备才可以在组间转移或者复制
				Terminal tmTerminal = new Terminal();
				tmTerminal.setTerminal_id(jsonBody.getString("terminal_id"));
				
				List<Map<String,Object>> allTerminalList = null;
				for(int i =0; i< terminal_ids.size(); i++) {
					condMap.put("terminal_id", terminal_ids.get(i));
					List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByPK", condMap);
					if(allTerminalList ==null) {
						allTerminalList = terminalList;
					}else {
						allTerminalList.addAll(terminalList);
					}
				}
				
				if(allTerminalList != null && allTerminalList.size() > 0){
					Map<String,Object> tmpTerminal = null;
					for(int j =0;j<allTerminalList.size();j++) {
						tmpTerminal = allTerminalList.get(j);
						String terminalGids = (String) tmpTerminal.get("gids");
						if(terminalType.length() >0 && !terminalType.equals(checkTerminalType((String) tmpTerminal.get("terminal_id")))) {
							result =0;
							break;
						}else {
							terminalType = checkTerminalType((String) tmpTerminal.get("terminal_id"));
						}
						
						 if(changeType.equals("copy")) {
							 if(terminalGids ==null ||terminalGids.length()==0) {
					    		 terminalGids = to_gid;
					    	 }else if(!terminalGids.contains(to_gid) ) {
					    		 terminalGids = terminalGids + "," + to_gid;
					    	 }
					     }else if(changeType.equals("move")) {
					    	 if(terminalGids ==null ||terminalGids.length()==0) {
					    		 terminalGids = to_gid;
					    	 }else if(!terminalGids.contains(to_gid)) {
					    		 terminalGids = terminalGids.replace(from_gid, to_gid);
					    	 }else {
					    		 terminalGids = terminalGids.replace("," + from_gid, "").replace(from_gid + ",", "").replace(from_gid, "");
					    	 }
					     }else if(changeType.equals("delete")) {
					    	  if(terminalGids!=null && terminalGids.length() > 0)  {
					    		  terminalGids = terminalGids.replace("," + from_gid, "").replace(from_gid + ",", "").replace(from_gid, "");
					    	 }
					     }
						 
						 tmpTerminal.put("gids", terminalGids);
						 result = ms.execute("Terminal", "update", tmpTerminal);
					}
				}
				
				if(from_gid!=null && from_gid.length() >0) {
					condMap.clear();
					condMap.put("gid", from_gid);
					List<Map<String,Object>> totalList = ms.selectList("Terminal", "countTerminalByGid", condMap);
					if(totalList !=null && totalList.size()==1) {
						int cnt = Integer.parseInt((String) totalList.get(0).get("count"));
						condMap.put("terminal_cnt", cnt);
						result = ms.execute("TerminalGroup", "update", condMap);
					}
				}
				if(to_gid!=null && to_gid.length() >0) {
					condMap.clear();
					condMap.put("gid", to_gid);
					List<Map<String,Object>> totalList = ms.selectList("Terminal", "countTerminalByGid", condMap);
					if(totalList !=null && totalList.size()==1) {
						int cnt = Integer.parseInt((String) totalList.get(0).get("count"));
						condMap.put("terminal_cnt", cnt);
						if(cnt >0) {
							condMap.put("group_type", terminalType);
						}else {
							condMap.put("group_type", "");
						}
						
						result = ms.execute("TerminalGroup", "update", condMap);
					}
				}
			}
			if(result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "修改成功！");
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "修改失败！");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
	
		HttpIO.writeResp(response, respJson);
	}
	
	
	@RequestMapping(value="/newList",method=RequestMethod.POST)
	public void getOnlineList(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String list_type = jsonBody.getString("list_type");
			String gid = jsonBody.getString("gid");
			Map conMap = new HashMap<String,Object>();
			Set<String> terminalGroups = null;
			if(adminUser.getIs_supper()==0) {
				terminalGroups = UserGroupController.getGroupListByUser(adminUser.getUid());
			}else {
				if(gid != null && gid.length() > 0 && !gid.equals("all-group")) {
					conMap.put("gid", gid);
				}
			}
			List<Map<String,Object>> devlist=null;
			List<Map<String,Object>> totalList= null;
			if(adminUser.getIs_supper()==1) {
				int total = 0;
				int pagesize =100;
				int totalPages = 0;
				totalList = ms.selectList("Terminal", "selectCountByCondition", conMap);
				if(totalList !=null && totalList.size()==1) {
					total = Integer.parseInt((String) totalList.get(0).get("count"));
				}
				totalPages = total/pagesize;
				if(total%pagesize >0) {
					totalPages +=1;
				}
				for(int page=0;page< totalPages;page++) {
					conMap.put("startrom", page*pagesize);
					conMap.put("pagesize",pagesize);
					List<Map<String,Object>> tmpdevlist = (List<Map<String,Object>>)ms.selectList("Terminal", "selectByConditionWithPage", conMap);
					if(devlist==null) {
						devlist = tmpdevlist;
					}else {
						devlist.addAll(tmpdevlist);
					}
				}
			}else if(terminalGroups.size() >0) {
				for (String terminal_gid : terminalGroups) {
					conMap.put("gid", terminal_gid);
					List<Map<String,Object>> tmpdevlist = (List<Map<String,Object>>)ms.selectList("Terminal", "selectByGroupId", conMap);
					if(devlist==null) {
						devlist = tmpdevlist;
					}else {
						devlist.addAll(tmpdevlist);
					}
				}
			}
			
			JSONArray onlineArr = new JSONArray();
			JSONArray offineArr = new JSONArray();
			if(devlist!=null && devlist.size() >0) {
				for(int i=0;i<devlist.size();i++) {
					JSONObject newTerminalInfo = new JSONObject();
					newTerminalInfo.put("terminal_id",(String) devlist.get(i).get("terminal_id"));
					newTerminalInfo.put("name",(String) devlist.get(i).get("name"));
					newTerminalInfo.put("install_addr",(String) devlist.get(i).get("install_addr"));
					newTerminalInfo.put("ip",(String) devlist.get(i).get("ip"));
					String gids = (String) devlist.get(i).get("gids");
					if(gids !=null && gids.length() >0) {
						newTerminalInfo.put("gids",gids.split(","));
					}else {
						newTerminalInfo.put("gids",new JSONArray());
					}
				
					String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) devlist.get(i).get("terminal_id"));
					if(null != onlineInfo) {
						String state = onlineInfo.split(":")[1];
						if(Integer.parseInt(state) > 0) {
							newTerminalInfo.put("state",Integer.parseInt(state));
							onlineArr.add(newTerminalInfo);
						}else {
							newTerminalInfo.put("state",0);
							offineArr.add(newTerminalInfo);
						}
					}else {
						newTerminalInfo.put("state",0);
						offineArr.add(newTerminalInfo);
					}
				}
			}
			
			if(list_type==null||list_type.length() ==0) {
				 if(onlineArr.size() >0||offineArr.size() >0) {
					    onlineArr.addAll(offineArr);
						respJson.put("status", Constant.SUCCESS);
						respJson.put("result", onlineArr);
				 }else {
					 respJson.put("status", Constant.notExistStatus);
					 respJson.put("msg", Constant.NodataErr);
				 }
			}else if(list_type.equals("online") && onlineArr.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", onlineArr);
			}else if(list_type.equals("offline") && offineArr.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", offineArr);
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
	
	@RequestMapping(value="/list",method=RequestMethod.POST)
	public void getTerminalByGid(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			String searchKey = jsonBody.getString("search_key");
			String lamp_status = jsonBody.getString("lamp_status");//获取开启教室灯的终端
			Map conMap = new HashMap<String,Object>();
			String gid = jsonBody.getString("gid");
			Set<String> terminalGroups =null;
			if(adminUser.getIs_supper()==0) {
				terminalGroups = UserGroupController.getGroupListByUser(adminUser.getUid());
				if(gid!=null && terminalGroups!=null && !terminalGroups.contains(gid)) {
					respJson.put("status", Constant.notExistStatus);
					respJson.put("msg", Constant.NodataErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			if(gid != null && gid.length() > 0 && !gid.equals("all-group")) {
				conMap.put("gid", gid);
			}
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			if(searchKey!=null && searchKey.length() >0) {
				conMap.put("key", searchKey);
			}
			if(getTotal !=null) {
				 if(lamp_status!=null && lamp_status.length() >0) {
						conMap.put("lamp_status", "yes");
				 }
				List<Map<String,Object>> totalList= ms.selectList("Terminal", "selectCountByCondition", conMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			JSONObject terminalResp = null;
			List<Map<String,Object>> devlist =null;
			if(searchKey!=null && searchKey.length() >0) {
				devlist = (List<Map<String,Object>>)ms.selectList("Terminal", "selectBySearch", conMap);
			}else if(lamp_status!=null && lamp_status.length() >0) {
				conMap.put("lamp_status", "yes");
				devlist = (List<Map<String,Object>>)ms.selectList("Terminal", "selectLampOn", conMap);
			}else {
				 devlist = (List<Map<String,Object>>)ms.selectList("Terminal", "selectByConditionWithPage", conMap);
			}

			if(devlist != null && devlist.size() > 0) {
				JSONArray retDevList = new JSONArray();
				devlist = Convert.SortDataListId(devlist, page, pagesize);
//				Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
//				for(int j=0;j<devlist.size();j++) {
//					String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) devlist.get(j).get("terminal_id"));
//					if(null != onlineInfo) {
//						int sequece = newSocketMsgHandler.sendMsgTo((String) devlist.get(j).get("ip"), cmds.GET_VOLUME, null);
//						 sequeceMap.put(sequece, (String) devlist.get(j).get("terminal_id"));
//					}
//				}
//				Log.d("test" , "testB");
//				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
//				Log.d("test" , "testC");
				for(int i=0;i<devlist.size();i++) {
					JSONObject newTerminal = new JSONObject();
					conMap.clear();
					conMap.put("terminal_id", (String) devlist.get(i).get("terminal_id"));
//					List<Map<String,Object>> cameralist = ms.selectList("Camera", "selectByCondition", conMap);
//					newTerminal.put("bind_cameras", cameralist);
					newTerminal.put("id", devlist.get(i).get("id"));
					newTerminal.put("terminal_id",(String) devlist.get(i).get("terminal_id"));
					newTerminal.put("name",(String) devlist.get(i).get("name"));
					newTerminal.put("ip",(String) devlist.get(i).get("ip"));
					newTerminal.put("install_addr",(String) devlist.get(i).get("install_addr"));
					String gids = (String) devlist.get(i).get("gids");
					if(gids !=null && gids.length() >0) {
						newTerminal.put("gids",gids.split(","));
					}else {
						newTerminal.put("gids",new JSONArray());
					}

					String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) devlist.get(i).get("terminal_id"));
					if(null != onlineInfo) {
						String state = onlineInfo.split(":")[1];
						newTerminal.put("state",Integer.parseInt(state));
						Log.d("test", "onLineInfo state " + state);
					}else {
						newTerminal.put("state",0);
					}
					if(devlist.get(i).containsKey("volume")) {
						newTerminal.put("volume", devlist.get(i).get("volume"));
					}else{
						newTerminal.put("volume", 101);
					}
//					if (RespMap != null && RespMap.size() > 0) {
//						for (Integer key : RespMap.keySet()) {
//							String tmpTerminalIP = RespMap.get(key).getString("terminal_ip");
//							if(tmpTerminalIP.equals((String) devlist.get(i).get("ip"))) {
//								JSONObject terminalRespJson = JSONObject.parseObject(RespMap.get(key).getString("resp"));
//								result = (int) terminalRespJson.get("status");
//								if (result == 1) {
//									JSONObject Volume =  terminalRespJson.getJSONObject("result");
//									newTerminal.put("volume",Volume.get("currentVolumn"));
//								}else {
//									newTerminal.put("volume",0);
//								}
//								RespMap.remove(key);
//								break;
//							}
//						}
//					}
					
					newTerminal.put("boot_time",(String)devlist.get(i).get("boot_time"));
					newTerminal.put("shutdown_time",(String)devlist.get(i).get("shutdown_time"));
					newTerminal.put("err_msg",(String)devlist.get(i).get("err_msg"));
					newTerminal.put("app_ver",(String)devlist.get(i).get("app_ver"));
					String lampStatus = (String) devlist.get(i).get("lamp_status");
					if(lampStatus!=null && lampStatus.length() >0) {
						String finalStatus = "";
						byte[] statusBytes = lampStatus.getBytes();
						for(int n=0;n<statusBytes.length;n++) {
							finalStatus+=(char)statusBytes[n];
						}
						newTerminal.put("lamp_status",finalStatus);
					}
					retDevList.add(newTerminal);
				}
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", retDevList);
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
	
	//修改终端信息
	@RequestMapping(value="/updateInfo",method=RequestMethod.POST)
	public void updateTerminalInfo(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String terminal_id = jsonBody.getString("terminal_id");
			String name = jsonBody.getString("name");
			String install_addr = jsonBody.getString("install_addr");
			Map<String,Object> condMap = new HashMap<String,Object>();
			condMap.put("terminal_id", terminal_id);
			condMap.put("name", name);
			condMap.put("install_addr", install_addr);
			result = ms.execute("Terminal","update",condMap);
			if(result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "修改成功！");
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "修改失败！");
			}
		
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", "无权操作！");
		}

		HttpIO.writeResp(response, respJson);
	}
	
	
	//删除终端，支持通过分组删除，通过终端id删除
	@RequestMapping(value="/del_dev",method=RequestMethod.POST)
	public void deleteTerminal(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String gid = jsonBody.getString("gid");
			JSONArray terminalIds = jsonBody.getJSONArray("terminal_ids");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			List<Map<String,Object>> theDelTerminals = null;
			if(gid !=null && gid.length() >0) {
				condMap.put("gid", gid);
				theDelTerminals = ms.selectList("Terminal", "selectByGroupId", condMap);
				result = ms.execute("Terminal", "deleteByGroupId", condMap);
			}else if(terminalIds != null && terminalIds.size() >0) {
				condMap.clear();
				condMap.put("terminal_ids", terminalIds);
				theDelTerminals = ms.selectList("Terminal", "selectByIds", condMap);
				condMap.clear();
				for(int i =0;i<terminalIds.size();i++) {
					String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) terminalIds.get(i));
					if(onlineInfo==null) {
						condMap.put("terminal_id", terminalIds.get(i));
						result = es.delete("Terminal", "delete", condMap);
						if(result==0) {
							break;
						}
					}
				}
				
			}
			if(result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "删除成功！");
				if(theDelTerminals!=null && theDelTerminals.size() >0) {
					Set<String> tmpTaskKeys = new HashSet<String>();
					tmpTaskKeys.addAll(RedisUtils.getKeys(Constant.TaskTerminalReady));
					tmpTaskKeys.addAll(RedisUtils.getKeys(Constant.TaskSentTerminalsOK));
					tmpTaskKeys.addAll(RedisUtils.getKeys(Constant.TaskSentTerminalsFailed));
					for(String redisKey:tmpTaskKeys) {
						for(int j=0;j<theDelTerminals.size();j++) {
							RedisUtils.removeMember(redisKey, (String) theDelTerminals.get(j).get("terminal_id"));
						}
					}
				}
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除失败！");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
		
	}
	
	
	@RequestMapping(value="/change_volume",method=RequestMethod.POST)
	public void changeVolume(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			JSONArray terminalIds = jsonBody.getJSONArray("terminal_ids");
			String action = jsonBody.getString("action");
			Map<String,Object> condMap = new HashMap<String,Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			JSONArray resultList = new JSONArray();
			if(terminalIds != null) {
				Map<Integer, Object> sequeceTaskMap = new HashMap<Integer, Object>();
				Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
				for(int i=0;i< terminalIds.size();i++) {
					condMap.clear();
					condMap.put("terminal_id", terminalIds.get(i));
					List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByPK", condMap);
					if(terminalList != null && terminalList.size()==1) {
						//Terminal theTerminal =  (Terminal) terminalList.get(0);
						JSONObject udpBody = new JSONObject();
						if(action.equals("incr")) {
							udpBody.put("cmd", 1); //增加音量
						}else if(action.equals("dec")) {
							udpBody.put("cmd", 2);//减少音量
						}else if(action.equals("max")) {
							udpBody.put("cmd", 3); //设置音量到最大值
						}else {
							udpBody.put("cmd", 4);//设置 音量到最小值
						}
//						“result”:{“currentVolumn”:12}
						int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(0).get("ip"), cmds.SET_VOLUME, udpBody.toJSONString()); //设置终端音量
						 sequeceMap.put(sequece, (String) terminalIds.get(i));
//						sequeceTaskMap.put(sequece, (String) terminalList.get(0).get("ip"));
					}
				}
				
				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {
						String terminal_ip = RespMap.get(key).getString("terminal_ip");
						String terminalRespStr = RespMap.get(key).getString("resp");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
						result = (int) terminalRespJson.get("status");
						if(result==1) {
							int volume = terminalRespJson.getJSONObject("result").getIntValue("currentVolumn");
							Map<String,Object> resultVolMap = new HashMap<String,Object>();
							resultVolMap.put(terminal_ip, volume);
							resultList.add(resultVolMap);
						}
					}
				}
			}
			
			if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", resultList);
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
	
	@RequestMapping(value="/findTerminals",method=RequestMethod.POST)
	public void findNewTerminals(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			newSocketMsgHandler.findAllLocalTerminals();
			respJson.put("status", Constant.SUCCESS);
			respJson.put("msg", Constant.SuccessMsg);
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/reboot_now",method=RequestMethod.POST)
	public void rebootTerminalNow(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			Map<String,Object> condMap = new HashMap<String,Object>();
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			JSONObject udpBody = new JSONObject();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			if(terminal_ids!=null && terminal_ids.size()>0) {
				for(int i=0;i< terminal_ids.size();i++) {
					condMap.clear();
					condMap.put("terminal_id", terminal_ids.get(i));
					List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByPK", condMap);
					if(terminalList != null && terminalList.size()==1) {
						String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + terminal_ids.get(i));
						if(null != onlineInfo) {
							newSocketMsgHandler.sendMsgTo((String) terminalList.get(0).get("ip"),cmds.REBOOT_TERMINAL_NOW, udpBody.toJSONString()); //设置终端音量
							result = 1;
						}else {
							break;
						}
					}
				}
			}
			
			if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
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
	
	//查看版本更新记录
	@RequestMapping(value="/update_log",method=RequestMethod.POST)
	public void appUpdateLog(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			Map<String,Object> conMap = new HashMap<String,Object>();
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			if (getTotal != null) {
				List<Map<String, Object>> totalList = ms.selectList("TerminalUpdate", "selectCountByCondition", conMap);
				if (totalList != null && totalList.size() == 1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			conMap.put("startrom", (page - 1) * pagesize);
			conMap.put("pagesize", pagesize);
			conMap.put("sort", "end_time desc");
			List<Map<String,Object>> logList= ms.selectList("TerminalUpdate", "selectByConditionWithPage", conMap);
			if(logList != null && logList.size() > 0) {
				respJson.put("status",Constant.SUCCESS);
				respJson.put("result", logList);
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
	
	
	//获取app升级状态，打开app升级,关闭app升级
	@RequestMapping(value="/appUpdateStatus",method=RequestMethod.POST)
	public void appUpdate(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			Map<String,Object> condMap = new HashMap<String,Object>();
			String attach_id = jsonBody.getString("attach_id"); //apk文件的附件id
			String action   = jsonBody.getString("action"); //check 为检查升级状态，on为启动升级,off为停止升级
			JSONObject updateStatus = new JSONObject();
			JSONArray resultList = new JSONArray();
			Integer totalTerminal = 0;
			Map<String, String> updateInfos = new HashMap<String,String>();
			List<Map<String,Object>> totalList= ms.selectList("Terminal", "selectCountByCondition", condMap);
			if(totalList !=null && totalList.size()==1) {
				totalTerminal = Integer.valueOf((String) totalList.get(0).get("count"));
			}else {
				totalTerminal =0;
			}
			condMap.put("attach_id", attach_id);
			condMap.put("attach_type", 4); //4是apk类型
			List<Map<String, Object>> attachList =ms.selectList("Attachment", "selectByPK", condMap);
			if(attachList!=null && attachList.size()==1) {
				updateStatus.put("apk_name", attachList.get(0).get("name"));
			}else {
				updateStatus.put("apk_name", "");
			}
			SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
			if(action.equals("on")){
				String newVersion = null;
				try {
					newVersion = FileUtil.checkUpgradeVersion((String) attachList.get(0).get("save_path"),"update-script");
					if(newVersion!=null) {
						updateInfos.put("new_version", newVersion);
						updateInfos.put("updating", "on");//启动升级，终端会不断来检查
						updateInfos.put("attach_id",attach_id);
						updateInfos.put("op_user",adminUser.getUsername());
						updateInfos.put("apk_name",(String) attachList.get(0).get("name"));
					
						updateInfos.put("create_time",f.format(new Date()));
						updateInfos.put("total_terminal",""+totalTerminal);
						RedisUtils.del(Constant.AppUpdateInfo);
						 RedisUtils.del(Constant.UpdateOkTerminals);
						 RedisUtils.del(Constant.UpdateFailedTerminals);
						RedisUtils.hset(Constant.AppUpdateInfo, updateInfos, 0);
					}else {
						respJson.put("status", Constant.FAILED);
						respJson.put("msg", "错误的升级文件!");
						HttpIO.writeResp(response, respJson);
						return;
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if(action.equals("off")) {
				updateInfos.put("updating", "off");//停止升级
				RedisUtils.hset(Constant.AppUpdateInfo, updateInfos, 0);
			}
	
			 updateInfos = RedisUtils.hgetAll(Constant.AppUpdateInfo);
			if(updateInfos !=null && updateInfos.size() > 0) {
				updateStatus.put("status", updateInfos.get("updating"));
				attach_id = updateInfos.get("attach_id");

				if(attachList!=null && attachList.size()==1) {
					updateStatus.put("apk_name", attachList.get(0).get("name"));
				}else {
					updateStatus.put("apk_name", updateInfos.get("apk_name"));
				}
				updateStatus.put("attach_id", attach_id);
				updateStatus.put("total_terminals", totalTerminal);
				Set<String> successList = RedisUtils.allSetData(Constant.UpdateOkTerminals);
				if(successList != null) {
					updateStatus.put("success_cnt", successList.size());
				}else {
					updateStatus.put("success_cnt", 0);
				}
				Set<String> failedList = RedisUtils.allSetData(Constant.UpdateFailedTerminals);
				if(failedList != null) {
					updateStatus.put("failed_cnt", failedList.size());
				}else {
					updateStatus.put("failed_cnt", 0);
				}
				updateStatus.put("notstart_cnt", 0); //此项由前端页面进行计算,总数减去成功和失败数量
				if(updateStatus.getIntValue("total_terminals") >0) {
					updateStatus.put("rate", updateStatus.getIntValue("success_cnt")*100/updateStatus.getIntValue("total_terminals")); //成功的数量除以总数是进度
				}else {
					updateStatus.put("rate", 0); //成功的数量除以总数是进度
				}
				
				updateStatus.put("create_time", updateInfos.get("create_time"));
			}else {
				updateStatus.put("status", "off");
				updateStatus.put("apk_name", "");
				updateStatus.put("attach_id", attach_id);
				updateStatus.put("total_terminals", 0);
				updateStatus.put("success_cnt", 0);
				updateStatus.put("failed_cnt", 0);
				updateStatus.put("notstart_cnt", 0);
				updateStatus.put("rate", 0); 
				updateStatus.put("create_time", "");
			}
			
			if(action.equals("off")) {
				 TerminalUpdate updateHistory = new TerminalUpdate();
				 updateHistory.setApk_name(updateStatus.getString("apk_name"));
				 updateHistory.setOp_user(updateInfos.get("op_user"));
				 updateHistory.setTotal_terminal_cnt(updateStatus.getIntValue("total_terminals"));
				 updateHistory.setOk_terminal_cnt(updateStatus.getIntValue("success_cnt"));
				 updateHistory.setFail_terminal_cnt(updateStatus.getIntValue("failed_cnt"));
				 updateHistory.setNew_version(updateInfos.get("new_version"));
				 updateHistory.setCreate_time(updateInfos.get("create_time"));

				 updateHistory.setEnd_time(f.format(new Date()));
				 
				 es.insert("TerminalUpdate", "insert", updateHistory);
				 RedisUtils.del(Constant.AppUpdateInfo);
			}
			
			resultList.add(updateStatus);
			respJson.put("status", Constant.SUCCESS);
			respJson.put("result", resultList);
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	
	//获取app升级状态，打开app升级,关闭app升级
		@RequestMapping(value="/appUpdateNow",method=RequestMethod.POST)
		public void appUpdateNow(HttpServletRequest request, HttpServletResponse response) {
			JSONObject respJson = new JSONObject();
			int result = 0;
			User adminUser = SesCheck.getUserBySession(request,es, true); 
			if(adminUser != null && adminUser.getIs_supper()==1) {
				String jsonBodyStr = HttpIO.getBody(request);
				JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
				JSONArray terminal_ips = jsonBody.getJSONArray("terminal_ips");
				Map<String, String> updateInfos = RedisUtils.hgetAll(Constant.AppUpdateInfo);
				if("on".equals(updateInfos.get("updating")) && terminal_ips!=null){
					if(updateInfos.get("attach_id").length() >0) {
						JSONObject udpBody = new JSONObject();
						udpBody.put("cmd", 3);//3是立即触发升级
						SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
						for(int i=0;i<terminal_ips.size();i++) {
							newSocketMsgHandler.sendMsgTo((String) terminal_ips.get(i),cmds.UPLOAD_TERMINAL_APP, udpBody.toJSONString());
							result = 1;
						}
					
					}
				}
				if(result==1) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("msg", "已经触发终端升级!");
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", "触发终端升级失败!");
				}
			}else {
				respJson.put("status", Constant.UserNotLogin);
				respJson.put("msg", Constant.PermissionErr);
			}
		  HttpIO.writeResp(response, respJson);
	}	
}
