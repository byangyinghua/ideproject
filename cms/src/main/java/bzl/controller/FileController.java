package bzl.controller;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;

//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import bzl.common.Configure;
import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.Attachment;
import bzl.entity.User;
import bzl.entity.TerminalLog;
import bzl.entity.UserLog;


import bzl.service.EntityService;
import bzl.service.MapService;
//import utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import utils.Convert;
//import utils.CommUtil;
//import utils.Convert;
import utils.EncryptionUtil;
import utils.FileUtil;
import utils.HttpIO;
//import utils.JModelAndView;
//import utils.MyListener;
import utils.MyReflect;
import utils.NetUtil;
import utils.RedisUtils;
//import utils.URLEncoder;
import utils.UUIDUtil;
//import utils.Writer;
import utils.ZipUtil;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ibm.icu.impl.duration.impl.DataRecord.EUnitVariant;

import sun.rmi.log.LogHandler;

@Controller
@RequestMapping("/file")
public class FileController {
	static Logger log=Logger.getLogger(LogHandler.class);

	protected MyReflect<?> mf=new MyReflect<Object>();
	@Autowired
	protected EntityService es;
	@Autowired
	protected MapService ms;
	private static String uploadpath= Configure.getUploadPath();
	private static String ImagePath = "/image";
	private static String AudioPath = "/audio";
	private static String VideoPath = "/video";
	private static String ZipPath = "/zip";
	private static String TerminalFilePath = "/terminal_file";//终端上传文件
	

	private static int checkFileType(String filename) {
		int typeCode = 100;
		filename = filename.toLowerCase();
		if(filename.endsWith(".jpeg")
				||filename.endsWith(".jpg")
				||filename.endsWith(".png")) {
			typeCode =  1; //仅仅支持 jpeg 以及png服务
		}else if(filename.endsWith(".mp3")
				||filename.endsWith(".wave")
				||filename.endsWith(".wma")
				||filename.endsWith(".aac")) {
			typeCode = 2;
		}else if (filename.endsWith(".mp4")
				||filename.endsWith(".flv")
				||filename.endsWith(".mkv")
				||filename.endsWith(".rmvb")
				||filename.endsWith(".avi")
				||filename.endsWith(".mpeg")) {
			typeCode = 3; //目前支持 mp4以及flv
		}else if(filename.endsWith(".zip")) {
			typeCode =  4; //终端安卓apk文件压缩包
		}else {
			typeCode =  100;
		}
		return typeCode;
	}
	
	private static JSONArray getAllUploadFileName(MultipartHttpServletRequest request){
		 Iterator<String> itr =  request.getFileNames();
		 JSONArray fileNames = new JSONArray();
	     while(itr.hasNext()){
	         String str = itr.next();
	         MultipartFile multipartFile = (MultipartFile)request.getFile(str);
	         String fileName = multipartFile.getOriginalFilename();   //原文件名
	         fileNames.add(fileName);
	     }
	     
	     return fileNames;
	}
	
	/**
	 * 获取上传的文件信息，并保存文件数据
	 * @param filename 不同的字段之间用-隔开
	 * @param request
	 * @return
	 */
	private static List<Map<String,Object>> saveUploadFiles(MultipartHttpServletRequest request,String terminal_id){
		List<MultipartFile> list = request.getFiles("upload_files");// 得到上传的文件复数
	    //System.out.println("\n file list:" + list.size());
		//String attachment = "";
		//HttpSession session = request.getSession();
		List<Map<String,Object>> uploadFileInfos = new ArrayList<Map<String,Object>>();
		for (int i = 0; i < list.size(); i++) {
			MultipartFile	mFile = list.get(i);
//			System.out.println("-----------------------------------------------mFile.hashCode()=" + mFile.hashCode());
//			log.error(mFile);
//			System.out.println("------------------------------------------------");
			if (!mFile.isEmpty()) {
				// 得到上传服务器的路径
			    SimpleDateFormat myFmt = new SimpleDateFormat("yyyyMMddHHmmss"); 
			    myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
				//String path = request.getSession().getServletContext().getRealPath(uploadpath);
			    String path = Configure.getUserHomePath() + uploadpath;
			    if(null !=terminal_id && terminal_id.length() >0) { //终端上传的文件
			    	path += TerminalFilePath + "/" + terminal_id;
			    }

				String originalFileName = mFile.getOriginalFilename();
				String uploadType = request.getParameter("upload_type");
				
				if(null !=uploadType && uploadType.equals("help")) {
					String helpId = request.getParameter("helpid");
					String uploadStatus = RedisUtils.get("upload:" + helpId + ":" + originalFileName);
					if(null == uploadStatus) {
						RedisUtils.set("upload:" + helpId + ":" + originalFileName,"uploading");
					}else {
						continue;
					}
				}
				
				int fileType = checkFileType(originalFileName);
				if(fileType == 1) { //图片
					path += ImagePath;
				}else if(fileType == 2) {//音频
					path += AudioPath;
				}else if(fileType ==3) {//视频
					path += VideoPath;
				}else if(fileType == 4) {
					path += ZipPath;
				}else {
					continue;
				}
				
				InputStream inputStream;
				try {
					inputStream = mFile.getInputStream();
					//获取文件类型
//					byte[] b = new byte[50];
//					inputStream.read(b);   
//					filetype = FileUtil.getFileTypeByStream(b);  
					//byte[] b = new byte[(int) mFile.getSize()];
					//int length = inputStream.read(b);
					// 构造上传的文件的文件名
					String realMd5 =EncryptionUtil.md5Hex("" + mFile.hashCode() + mFile.getContentType() + mFile.getSize());
					String newFilename = realMd5 +"." + originalFileName.substring(originalFileName.lastIndexOf(".")+1);
				
					//文件流写到服务器端
					File checkUploadDir = new File(path);
			        if (!checkUploadDir.exists()) {
			        	checkUploadDir.mkdirs();
			        }
			        
			        path += "/" +   newFilename;
			        
			        File checkUploadFile = new File(path);
			        if (!checkUploadFile.exists()) {
			        	FileOutputStream outputStream = new FileOutputStream(path);
						int bytesRead = 0;
						byte[] buffer = new byte[81920];
						//开始读取网络上传文件
						while ((bytesRead = inputStream.read(buffer, 0, 81920)) != -1) {
							outputStream.write(buffer, 0, bytesRead);
						}
						
						//outputStream.write(b, 0, length);
						inputStream.close();
						outputStream.close();

						Map<String,Object> fileInfoMap=new HashMap<String,Object>();
						fileInfoMap.put("attach_id", RandomStringUtils.randomAlphanumeric(11));
						fileInfoMap.put("name", originalFileName);
						fileInfoMap.put("save_path", path);
						fileInfoMap.put("size", mFile.getSize());
						fileInfoMap.put("attach_type", checkFileType(originalFileName));
						
						uploadFileInfos.add(fileInfoMap);
						if(null !=uploadType && uploadType.equals("help")) {
							String helpId = request.getParameter("helpid");
							RedisUtils.set("upload:" + helpId + ":" + originalFileName,path);
						}
			        }
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//System.out.println("\n\nuploadFileInfos================" + uploadFileInfos.toString());
		return uploadFileInfos;
	}
	

	/**
	 * 
	 * @功能: 管理用户文件上传
	 * @标题 fileUpload
	 * @参数 @param request
	 * @参数 @param response
	 * @参数 @throws IOException   
	 * @返回 void  
	 * @throws
	 * @日期 2019年08月07日
	 */
	@RequestMapping(value = "/user_upload",method=RequestMethod.POST)
	@ResponseBody
	public void fileUpload(MultipartHttpServletRequest request,
			HttpServletResponse response) throws IOException {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			List<Map<String,Object>> savedFileInfos = saveUploadFiles(request,null);
			if(savedFileInfos !=null && savedFileInfos.size() >0) {
				JSONArray success_list = new JSONArray();
				for(int i=0;i < savedFileInfos.size();i++) {
					Map<String,Object> saveFileInfo =  savedFileInfos.get(i);
					saveFileInfo.put("upload_user", adminUser.getUsername());
					saveFileInfo.put("upload_uid", adminUser.getUid());
					result = ms.execute("Attachment", "insert", saveFileInfo);
					if(result==1) {
						success_list.add(saveFileInfo);
					}
				}
				
				if(success_list != null && success_list.size() >0) {
					//下面添加用户操作日志
					JSONObject actionLog = new JSONObject();
					actionLog.put("uid", adminUser.getUid());
					actionLog.put("username", adminUser.getUsername());
					actionLog.put("realname", adminUser.getReal_name());
					actionLog.put("action_type", Constant.ActionUpload);
					JSONObject content = new JSONObject();
					content.put("action_name", "上传文件");
					content.put("filename",  savedFileInfos.get(0).get("name"));
					actionLog.put("action_content",content.toJSONString());
					es.insert("UserLog", "insert", actionLog);
					
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", success_list);
				}else {
					System.out.println("\n\nrun into file saveFileInfo failed!!!!!!!!!!==");
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.FailedMsg);
				}
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
		 HttpIO.writeResp(response, respJson);
	
	}
	
	
	/**
	 * 
	 * @功能: 终端上传文件
	 * @标题 fileUpload
	 * @参数 @param request
	 * @参数 @param response
	 * @参数 @throws IOException   
	 * @返回 void  
	 * @throws
	 * @日期 2019年08月07日
	 */
	@RequestMapping(value = "/terminal_upload",method=RequestMethod.POST)
	@ResponseBody
	public void terminalUploadFile(MultipartHttpServletRequest request,
			HttpServletResponse response) throws IOException {
		JSONObject respJson = new JSONObject();
		
		String terminalIP = NetUtil.getIpAddr(request);
		
		String terminalId = request.getParameter("terminal_id");
		
		if(terminalId != null && terminalId.length() >0) {
			Map<String,Object> condMap = new HashMap<String,Object>();
			condMap.put("ip", terminalIP);
			condMap.put("terminal_id", terminalId);
			List<Map<String,Object>> terminal = ms.selectList("Terminal", "selectByCondition", condMap);
			if(terminal != null && terminal.size()==1) {
				List<Map<String,Object>> savedFileInfos = saveUploadFiles(request,terminalId);
				if(savedFileInfos !=null && savedFileInfos.size() >0) {
					//JSONArray success_list = new JSONArray();
					for(int i=0;i < savedFileInfos.size();i++) {
						Map<String,Object> saveFileInfo =  savedFileInfos.get(i);
						TerminalLog tmpTerminalLog = new TerminalLog();
						tmpTerminalLog.setTerminal_id(terminalId);
						tmpTerminalLog.setAction("upload"); //上传
						tmpTerminalLog.setContent(JSONObject.toJSONString(saveFileInfo));
						tmpTerminalLog.setResult("ok");; //　１为上传成功
						//es.insert("TerminalLog", "insert", tmpTerminalLog);
//						log.info("====================================");
//						log.info(tmpTerminalLog);
//						log.info("====================================");
						es.insert("TerminalLog", "insert", tmpTerminalLog);
						respJson.put("status", Constant.SUCCESS);
						respJson.put("msg", Constant.SuccessMsg);
					}
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.ParemeterErr);
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
	
	
	/**
	 * 文件下载
	 * @功能: TODO
	 * @标题 download下载函数
	 * @参数 @param request
	 * @参数 @param response
	 * @返回 void  
	 * @throws
	 * @作者 
	 * @日期 2017年3月20日
	 */
	@RequestMapping(value = "/tmpDownload/{file_name}",method=RequestMethod.GET) 
	public void downloadTmpFile(HttpServletRequest request, HttpServletResponse response,@PathVariable java.lang.String file_name) {
		JSONObject respJson = new JSONObject();
		String terminalIP = NetUtil.getIpAddr(request);
		String tmpFilePath = "/home/boyao/tmpFile/";
		String tmpFileName = tmpFilePath + file_name;
		
		if(file_name.equals("all")) {
			try {
				String zipFileName = "/tmp/" +  file_name +  new Date().getTime() + ".zip";
				File zipFile = new File(zipFileName);
				FileOutputStream fos1 = new FileOutputStream(zipFile);
				ZipUtil.compress(tmpFilePath, zipFileName);
				FileUtil.transferFile(request, response, zipFileName); // 实际传输文件
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			FileUtil.transferFile(request,response,tmpFileName); //实际传输文件
		}
		
	}
	
	/**
	 * 日志文件或者临时文件上传
	 * @功能: TODO
	 * @标题 tmpUpload　文件上传
	 * @参数 @param request
	 * @参数 @param response
	 * @返回 void  
	 * @throws
	 * @作者 
	 * @日期 2017年3月20日
	 */
	@RequestMapping(value = "/tmpUpload",method=RequestMethod.POST)
	@ResponseBody
	public void tmpUploadFile(MultipartHttpServletRequest request,
			HttpServletResponse response) throws IOException {
		JSONObject respJson = new JSONObject();
		String terminalIP = NetUtil.getIpAddr(request);
		String tmpFilePath = "/home/boyao/tmpFile/";
		
		List<MultipartFile> list = request.getFiles("upload_files");// 得到上传的文件复数
		List<Map<String,Object>> uploadFileInfos = new ArrayList<Map<String,Object>>();
		for (int i = 0; i < list.size(); i++) {
			MultipartFile	mFile = list.get(i);
			if (!mFile.isEmpty()) {
				// 得到上传服务器的路径
			    SimpleDateFormat myFmt = new SimpleDateFormat("yyyyMMddHHmmss"); 
			    myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
				String originalFileName = mFile.getOriginalFilename();
				InputStream inputStream;
				try {
					inputStream = mFile.getInputStream();
					//文件流写到服务器端
					File checkUploadDir = new File(tmpFilePath);
			        if (!checkUploadDir.exists()) {
			        	checkUploadDir.mkdirs();
			        }
			        String tmpFile = tmpFilePath +   originalFileName;
			        File checkUploadFile = new File(tmpFile);
			        if (!checkUploadFile.exists()) {
			        	FileOutputStream outputStream = new FileOutputStream(tmpFile);
						int bytesRead = 0;
						byte[] buffer = new byte[81920];
						//开始读取网络上传文件
						while ((bytesRead = inputStream.read(buffer, 0, 81920)) != -1) {
							outputStream.write(buffer, 0, bytesRead);
						}
						inputStream.close();
						outputStream.close();
			        }
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * 提供网络日志文件
	 * @功能: TODO
	 * @标题 提供给终端写网络日志文件
	 * @参数 @param request
	 * @参数 @param response
	 * @返回 void  
	 * @throws
	 * @作者 
	 * @日期 2017年3月20日
	 */
	@RequestMapping(value = "/terminalNetLog",method=RequestMethod.POST)
	@ResponseBody
	public void terminalLogUpload(MultipartHttpServletRequest request,
			HttpServletResponse response) throws IOException {
		JSONObject respJson = new JSONObject();
		String terminalIP = NetUtil.getIpAddr(request);
		Map<String,Object> condMap = new HashMap<String,Object>();
		condMap.put("ip", terminalIP);
		List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByCondition", condMap);
		if(terminalList == null || terminalList.size()==0) {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
			HttpIO.writeResp(response, respJson);
			return;
		}else {
			String logfilePath = "/home/boyao/tmpFile/" +terminalList.get(0).get("terminal_id") + ".log" ;
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String content = jsonBody.getString("content");
			BufferedWriter out = null;
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(logfilePath, true)));
					out.write(content+"\r\n");
			out.close();
		}
		respJson.put("status", Constant.SUCCESS);
		respJson.put("msg", Constant.SuccessMsg);
		HttpIO.writeResp(response, respJson);
	}
	
	

	/**
	 * 文件下载
	 * @功能: TODO
	 * @标题 download下载函数
	 * @参数 @param request
	 * @参数 @param response
	 * @返回 void  
	 * @throws
	 * @作者 
	 * @日期 2017年3月20日
	 */
	@RequestMapping(value = "/download/{attach_id}",method=RequestMethod.GET) 
	public void download(HttpServletRequest request, HttpServletResponse response,@PathVariable java.lang.String attach_id) {
		JSONObject respJson = new JSONObject();
		String terminalId = request.getParameter("terminal_id"); //终端下载需要携带 terminal_id
		String terminalIP = NetUtil.getIpAddr(request);
		if(terminalId ==null ||terminalId.length()==0) {
			User adminUser = SesCheck.getUserBySession(request,es, false);
			if(adminUser ==null) {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.PermissionErr);
				 HttpIO.writeResp(response, respJson);
				 return;
			}
		}else {
			Map<String,Object> condMap = new HashMap<String,Object>();
			condMap.put("ip", terminalIP);
			condMap.put("terminal_id", terminalId);
			List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByCondition", condMap);
			System.out.println("terminalIP:"+terminalIP);
			System.out.println("terminalId:"+terminalId);
			System.out.println("terminal:"+terminalList.toString());
			if(terminalList == null || terminalList.size()==0) {
				respJson.put("status", Constant.UserNotLogin);
				respJson.put("msg", Constant.PermissionErr);
				HttpIO.writeResp(response, respJson);
				return;
			}
		}
		
		Map<String,Object> conditionMap = new HashMap<String,Object>();
		conditionMap.put("attach_id", attach_id);
		Map<String,Object> resultMap=(Map<String, Object>) ms.selectOne("Attachment","selectByCondition", conditionMap);
		String savePath = null;
		if(resultMap!=null) {
			savePath =  resultMap.get("save_path").toString();
		}

		if (savePath!=null && !"".equals(savePath)) {
			FileUtil.transferFile(request,response,savePath); //实际传输文件
		} else {
			System.out.println("下载文件时参数错误");
			respJson.put("status", Constant.FAILED);
			respJson.put("msg", "下载失败");
		}
		 HttpIO.writeResp(response, respJson);
	}
	
	//前端页面播放求助视频
	@RequestMapping(value = "/play/help_video",method=RequestMethod.GET) 
	public void playVideo(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			Map<String,Object> paremeterJson = SesCheck.getPathParameter("a?" + request.getQueryString());
			String helpId = (String) paremeterJson.get("help_id");
			String videoName = (String) paremeterJson.get("video_name");
			//String savePath = uploadpath + helpId + "/" + videoName;
			
			String savePath = RedisUtils.get("upload:" + helpId + ":" + videoName);
			
			System.out.println("\nrun into play help video!!!!!!!!!!!!!!!!!!");
			System.out.println("upload:" + helpId + ":" + videoName);
			System.out.println(savePath);

			if (!"".equals(savePath)) {
				FileUtil.transferFile(request,response,savePath);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			} else {
				System.out.println("下载文件时参数错误");
			}
			
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		 HttpIO.writeResp(response, respJson);
		
	}
	
	
	//获取文件列表
	@RequestMapping(value="/file_list",method=RequestMethod.POST)
	@ResponseBody
	public void getFileList(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map<String,Object> conditionMap = new HashMap<String,Object>();
			conditionMap.put("attach_type", jsonBody.getIntValue("file_type"));
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("Attachment", "selectCountByCondition", conditionMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			conditionMap.put("startrom", (page-1)*pagesize);
			conditionMap.put("pagesize", pagesize);
			conditionMap.put("sort", "create_time desc");
			
		 	List<Map<String,Object>> attachmentList =  ms.selectList("Attachment", "selectByConditionWithPage", conditionMap);
		 	attachmentList=Convert.SortDataListId(attachmentList,page,pagesize);
		    if(attachmentList != null && attachmentList.size() >0) {
		    	JSONArray retList = new JSONArray();
		    	File checkFile = null;
		    	for(int i=0;i<attachmentList.size();i++) {
		    		//JSONObject retFileInfo = new JSONObject();
		    		Map<String,Object> attachInfo  = attachmentList.get(i);
		    		checkFile = new File((String) attachInfo.get("save_path"));
		    		if(checkFile.exists()) {
		    			attachInfo.put("exist", 1);
		    		}else {
		    			attachInfo.put("exist", 0);
		    		}
		    		retList.add(attachInfo);
		    	}
		    	
		    	if(retList.size() > 0) {
		    		respJson.put("status", Constant.SUCCESS);
		    		respJson.put("result",retList);
		    	}else {
		    		respJson.put("status", Constant.FAILED);
		    		respJson.put("msg",Constant.NodataErr);
		    	}
		    }else {
		    	respJson.put("status", Constant.notExistStatus);
	    		respJson.put("msg",Constant.NodataErr);
		    }
		
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	
	//修改附件文件名
	@RequestMapping(value="/modify_name",method=RequestMethod.POST)
	@ResponseBody
	public void modifyFileName(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String newName = jsonBody.getString("new_name");
			String attach_id =  jsonBody.getString("attach_id");
			if(newName==null||newName.length()==0||attach_id==null||attach_id.length()==0) {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);	
			}else {
				Map<String,Object> conditionMap = new HashMap<String,Object>();
				conditionMap.put("attach_id", attach_id);
				conditionMap.put("upload_uid", adminUser.getUid());
				conditionMap.put("name", newName);
				result = es.update("Attachment", "update", conditionMap);
				if(result ==1) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("msg", Constant.SuccessMsg);
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.FailedMsg);
				}
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
		 HttpIO.writeResp(response, respJson);
	}
	
	
	 //校验文件是否ok
		@RequestMapping(value="/checkMd5",method=RequestMethod.GET)
		@ResponseBody
		public void fileCheck(HttpServletRequest request,
					HttpServletResponse response) {
			JSONObject respJson = new JSONObject();
			String attach_id = request.getParameter("attach_id"); //文件的md5值
			String fileMd5 = request.getParameter("file_md5"); //文件的md5值
			int result = 0;
			try {
				Map<String,Object> condMap = new HashMap<String,Object>();
				condMap.put("attach_id", attach_id);
				List<Map<String, Object>> attachList = ms.selectList("Attachment", "selectByPK", condMap);
				if(attachList !=null && attachList.size()==1) {
					String realMd5 = DigestUtils.md5Hex(new FileInputStream(new File((String) attachList.get(0).get("save_path"))));
					if(realMd5.equals(fileMd5)) {
						result = 1;
					}
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "文件校验失败!");
			}
			
			 HttpIO.writeResp(response, respJson);
		}
	
	
	 //删除附件
	@RequestMapping(value="/del_files",method=RequestMethod.POST)
	@ResponseBody
	public void deleteAttachment(HttpServletRequest request,
				HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser != null) {
			boolean hasFileInUse = false;
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			JSONArray attach_ids = jsonBody.getJSONArray("attach_ids");
			
			Map<String,Object> conMap = new HashMap<String,Object>();
			conMap.put("attach_ids", attach_ids);
			if(attach_ids != null && attach_ids.size() >0) {
				for(int i =0; i< attach_ids.size();i++) {
					boolean inUse = false;
					conMap.put("attach_id", attach_ids.get(i));
					conMap.put("upload_uid", adminUser.getUid());
					List<Map<String,Object>> attachList = ms.selectList("Attachment", "selectByCondition", conMap);
					if(attachList != null && attachList.size() >0) {
						//selectByCondition 删除该文件在使用的任务的列表
						conMap.clear();
						conMap.put("content", attach_ids.get(i));
						List<Map<String,Object>> tmpTaskList = ms.selectList("TaskInfo", "selectByCondition", conMap);
						if(tmpTaskList!=null && tmpTaskList.size() >0) {
							File checkFile = new File((String) attachList.get(i).get("save_path"));
							if(!checkFile.exists()) {
								for(int j=0;j<tmpTaskList.size();j++) {
									String theContent = (String) tmpTaskList.get(j).get("content");
									JSONArray contentArr = JSONObject.parseArray(theContent);
									contentArr.remove(attach_ids.get(i));
									tmpTaskList.get(j).put("content", contentArr.toJSONString());
									result = es.update("TaskInfo", "update", tmpTaskList);
							    }
							}else {
								inUse = true;
								hasFileInUse = true;
								continue;
							}
						}
						if(!inUse) {
							String filePath = (String) attachList.get(0).get("save_path");
							File file = new File(filePath);
							file.delete();
							conMap.put("attach_id", attach_ids.get(i));
							conMap.put("upload_uid", adminUser.getUid());
							result = ms.execute("Attachment","delete",conMap);
							if(result==1) {
								//下面添加用户操作日志
								JSONObject actionLog = new JSONObject();
								actionLog.put("uid", adminUser.getUid());
								actionLog.put("username", adminUser.getUsername());
								actionLog.put("realname", adminUser.getReal_name());
								actionLog.put("action_type", Constant.ActionDel);
								JSONObject content = new JSONObject();
								content.put("action_name", "删除文件");
								content.put("filename",  attachList.get(0).get("name"));
								actionLog.put("action_content",content.toJSONString());
								es.insert("UserLog", "insert", actionLog);
							}
						}
					}
				}
			}
			
			if(hasFileInUse) {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除失败,文件被任务使用中!");
			}else if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "操作失败,可能是别人上传的或者文件不存在");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
			
		HttpIO.writeResp(response, respJson);
		}
	
}