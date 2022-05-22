package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jfree.util.Log;

import bzl.common.Configure;
import bzl.entity.User;
//import bzl.viewoffice.FileUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import sun.rmi.log.LogHandler;

public class FileUtil {
	Logger log = Logger.getLogger(LogHandler.class);

	public static Map<String, List<String>> umap = new HashMap<String, List<String>>();
	public final static Map<String, String> FILE_TYPE_MAP = new HashMap<String, String>();    
	private static String uploadpath;
	static {
		getAllFileType();  //初始化文件类型信息    
		Properties prop = new Properties();
		try {
			prop.load(
					Thread.currentThread().getContextClassLoader().getResourceAsStream("properties/config.properties"));
			uploadpath = prop.getProperty("uploadpath").trim();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	 /**  
     * Created on 2010-7-1   
     * <p>Discription:[getAllFileType,常见文件头信息]</p>  
     * @author:[shixing_11@sina.com]  
     */    
    private static void getAllFileType()    
    {    
        FILE_TYPE_MAP.put("jpg", "FFD8FF"); //JPEG (jpg)    
        FILE_TYPE_MAP.put("png", "89504E47");  //PNG (png)    
       // FILE_TYPE_MAP.put("gif", "47494638");  //GIF (gif)    
       // FILE_TYPE_MAP.put("tif", "49492A00");  //TIFF (tif)    
        FILE_TYPE_MAP.put("bmp", "424D"); //Windows Bitmap (bmp)    
       // FILE_TYPE_MAP.put("dwg", "41433130"); //CAD (dwg)    
       // FILE_TYPE_MAP.put("html", "68746D6C3E");  //HTML (html)    
       // FILE_TYPE_MAP.put("rtf", "7B5C727466");  //Rich Text Format (rtf)    
       // FILE_TYPE_MAP.put("xml", "3C3F786D6C");    
        FILE_TYPE_MAP.put("zip", "504B0304");    
       // FILE_TYPE_MAP.put("rar", "52617221");    
       // FILE_TYPE_MAP.put("psd", "38425053");  //Photoshop (psd)    
       // FILE_TYPE_MAP.put("eml", "44656C69766572792D646174653A");  //Email [thorough only] (eml)    
       // FILE_TYPE_MAP.put("dbx", "CFAD12FEC5FD746F");  //Outlook Express (dbx)    
       // FILE_TYPE_MAP.put("pst", "2142444E");  //Outlook (pst)    
       // FILE_TYPE_MAP.put("xls", "D0CF11E0");  //MS Word    
       // FILE_TYPE_MAP.put("doc", "D0CF11E0");  //MS Excel 注意：word 和 excel的文件头一样    
        FILE_TYPE_MAP.put("mdb", "5374616E64617264204A");  //MS Access (mdb)    
        FILE_TYPE_MAP.put("wpd", "FF575043"); //WordPerfect (wpd)     
        FILE_TYPE_MAP.put("eps", "252150532D41646F6265");    
        FILE_TYPE_MAP.put("ps", "252150532D41646F6265");    
        FILE_TYPE_MAP.put("pdf", "255044462D312E");  //Adobe Acrobat (pdf)    
        FILE_TYPE_MAP.put("qdf", "AC9EBD8F");  //Quicken (qdf)    
        FILE_TYPE_MAP.put("pwl", "E3828596");  //Windows Password (pwl)    
        FILE_TYPE_MAP.put("wav", "57415645");  //Wave (wav)    
        FILE_TYPE_MAP.put("avi", "41564920");    
        FILE_TYPE_MAP.put("ram", "2E7261FD");  //Real Audio (ram)    
        FILE_TYPE_MAP.put("rm", "2E524D46");  //Real Media (rm)    
        FILE_TYPE_MAP.put("mpg", "000001BA");  //    
        FILE_TYPE_MAP.put("mov", "6D6F6F76");  //Quicktime (mov)    
        FILE_TYPE_MAP.put("asf", "3026B2758E66CF11"); //Windows Media (asf)    
        FILE_TYPE_MAP.put("mid", "4D546864");  //MIDI (mid)    
    }    
    
  
	public static String getUploadpath() {
		return uploadpath;
	}

	public static void setUploadpath(String uploadpath) {
		FileUtil.uploadpath = uploadpath;
	}

	/**
	 * 上传同一个字段名下的多个文件
	 * 
	 * @param filename
	 * @param request
	 * @return
	 */
	@Deprecated
	public static List<Map<String, Object>> getUploadFMap(String filename, MultipartHttpServletRequest request) {
		List<MultipartFile> list = request.getFiles(filename);// 得到上传的文件复数
		String attachment = "";
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		List<Map<String, Object>> flist = new ArrayList<Map<String, Object>>();
		List<String> ulist = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) {

			MultipartFile mFile = list.get(i);
			if (!mFile.isEmpty()) {
				// 得到上传服务器的路径
				String path = request.getSession().getServletContext().getRealPath(FileUtil.getUploadpath());
				// 得到上传的文件的文件名
				String filename1 = System.currentTimeMillis() + mFile.getOriginalFilename();
				InputStream inputStream;
				try {
					inputStream = mFile.getInputStream();
					byte[] b = new byte[1048576];
					int length = inputStream.read(b);
					path += "\\" + filename1;
					// 文件流写到服务器端
					FileOutputStream outputStream = new FileOutputStream(path);
					outputStream.write(b, 0, length);
					inputStream.close();
					outputStream.close();
					attachment = "/upload/attachment/" + filename1;
					Map<String, Object> map = new HashMap<String, Object>();
					// map.put(filename, attachment);
					map.put("userid", user.getId());
					map.put("attachment", attachment);
					String uuid = UUIDUtil.getUUID();
					map.put("id", uuid);
					ulist.add(uuid);
					flist.add(map);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flist;
	}

	/**
	 * 上传多个字段下的多个文件，返回UUID的map
	 * 
	 * @param filename 不同的字段之间用-隔开
	 * @param request
	 * @return
	 */
	public static Map<String, List<Map<String, Object>>> getUploadMap(String filename,
			MultipartHttpServletRequest request) {
		String[] file = filename.split("-");
		Map<String, List<Map<String, Object>>> resultmap = new HashMap<String, List<Map<String, Object>>>();
		for (int n = 0; n < file.length; n++) {
			List<MultipartFile> list = request.getFiles(file[n]);// 得到上传的文件复数
			String attachment = "";
			HttpSession session = request.getSession();
			User user = (User) session.getAttribute("user");
			List<Map<String, Object>> flist = new ArrayList<Map<String, Object>>();
			List<String> ulist = new ArrayList<String>();
			List<String> filelist = new ArrayList<String>();
			for (int i = 0; i < list.size(); i++) {
				MultipartFile mFile = list.get(i);
				if (!mFile.isEmpty()) {
					// 得到上传服务器的路径
					SimpleDateFormat myFmt1 = new SimpleDateFormat("yyyyMMddHHmmss");
					myFmt1.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
//				    String name = myFmt1.format(new Date());
					String path = request.getSession().getServletContext().getRealPath(FileUtil.getUploadpath());
					// 得到上传的文件的文件名
//					String filename1 = UUID.randomUUID().toString().replaceAll("-", "")+"."+FileUtils.getExtend(mFile.getOriginalFilename());
					String filename1 = UUID.randomUUID().toString().replaceAll("-", "") + "." + "exe";
					System.out.println("filename1:" + filename1);
					InputStream inputStream;
					try {
						inputStream = mFile.getInputStream();
						System.out.println(mFile.getSize());
						byte[] b = new byte[(int) mFile.getSize()];
						int length = inputStream.read(b);

						System.out.println("path:" + path);
						// 文件流写到服务器端
						File testfile = new File(path);
						if (!testfile.exists()) {
							testfile.mkdirs();
							System.out.println("create file:" + testfile.getPath());
						}

						path += filename1;

						FileOutputStream outputStream = new FileOutputStream(path);
						outputStream.write(b, 0, length);
						inputStream.close();
						outputStream.close();
						attachment = FileUtil.getUploadpath() + filename1;
						Map<String, Object> map = new HashMap<String, Object>();
						// map.put(filename, attachment);
						map.put("userid", user.getId());
						map.put("filename", mFile.getOriginalFilename());
						map.put("attachment", attachment);
						map.put("filelength", mFile.getSize());
						String uuid = UUIDUtil.getUUID();
						map.put("id", uuid);
						ulist.add(uuid);
						filelist.add(attachment);
						flist.add(map);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			umap.put(file[n], ulist);
			System.out.println(file[n] + "111111=============" + ulist);
			resultmap.put(file[n], flist);
			// System.out.println("resultmap===="+resultmap);
		}
		return resultmap;
	}

	public static String checkUpgradeVersion(String zipPackage, String fileToBeExtracted) throws FileNotFoundException {
		String newVersion = null;
		OutputStream out = new FileOutputStream(fileToBeExtracted);
		FileInputStream fileInputStream = new FileInputStream(zipPackage);
		BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
		ZipInputStream zin = new ZipInputStream(bufferedInputStream);
		ZipEntry ze = null;
		byte[] buffer = null;
		try {
			while ((ze = zin.getNextEntry()) != null) {
				if (ze.getName().equals(fileToBeExtracted)) {
					buffer = new byte[9000];
					int len;
					while ((len = zin.read(buffer)) != -1) {
						out.write(buffer, 0, len);
					}
					out.close();
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			zin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (buffer != null) {
			newVersion = new String(buffer).split("\n|\r")[0];
			System.out.println("\nread version from zip:" + new String(buffer));
		} else {
			System.out.println("\nerror upgrade zip file!!!!!!!");
		}
		return newVersion;
	}

	// return copy size
	public static int copyFile(String from, String to) {
		int bytesRead = 0;
		try {
			InputStream ins = new FileInputStream(from);
			BufferedInputStream bins = new BufferedInputStream(ins);// 放到缓冲流里面
			OutputStream outs = new FileOutputStream(to);
			BufferedOutputStream bouts = new BufferedOutputStream(outs);

			byte[] buffer = new byte[81920];
			// 开始向网络传输文件流
			try {
				while ((bytesRead = bins.read(buffer, 0, 81920)) != -1) {
					bouts.write(buffer, 0, bytesRead);
				}
				bouts.flush();// 这里一定要调用flush()方法
				ins.close();
				bins.close();
				outs.close();
				bouts.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // 构造一个读取文件的IO流对象

		return bytesRead;
	}

	public static void transferFile(HttpServletRequest request, HttpServletResponse response, String savePath) {

		try {
			// String fullPath =request.getSession().getServletContext().getRealPath("");
			File file = new File(savePath);// 构造要下载的文件

			if (file.exists()) {
				InputStream ins = new FileInputStream(savePath);// 构造一个读取文件的IO流对象
				BufferedInputStream bins = new BufferedInputStream(ins);// 放到缓冲流里面
				OutputStream outs = response.getOutputStream();// 获取文件输出IO流
				BufferedOutputStream bouts = new BufferedOutputStream(outs);

				String[] splitFilePath = savePath.split("/");

				response.setContentType("application/x-download");// 设置response内容的类型
				response.setContentLength((int) file.length());// 设置response内容的类型
				response.setHeader("Content-disposition",
						"attachment;filename=" + URLEncoder.encode(splitFilePath[splitFilePath.length - 1], "UTF-8"));// 设置头部信息
				int bytesRead = 0;
				byte[] buffer = new byte[81920];
				// 开始向网络传输文件流
				while ((bytesRead = bins.read(buffer, 0, 81920)) != -1) {
					bouts.write(buffer, 0, bytesRead);
				}
				bouts.flush();// 这里一定要调用flush()方法
				ins.close();
				bins.close();
				outs.close();
				bouts.close();
			} else {
				response.setContentType("application/x-download");// 设置response内容的类型
				response.setContentLength(0);// 设置response内容的类型
				response.setStatus(500);
				System.out.println("下载的文件不存在!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return;
	}

	public static void delete(File file) {
		if (!file.exists())
			return;

		if (file.isFile() || file.list() == null) {
			file.delete();
			System.out.println("删除了" + file.getName());
		} else {
			File[] files = file.listFiles();
			for (File a : files) {
				delete(a);
			}
			file.delete();
			System.out.println("删除了" + file.getName());
		}

	}
	  
	//判断文件类型
	  public final static String getFileHexString(byte[] b)    
	    {    
	        StringBuilder stringBuilder = new StringBuilder();    
	        if (b == null || b.length <= 0)    
	        {    
	            return null;    
	        }    
	        for (int i = 0; i < b.length; i++)    
	        {    
	            int v = b[i] & 0xFF;    
	            String hv = Integer.toHexString(v);    
	            if (hv.length() < 2)    
	            {    
	                stringBuilder.append(0);    
	            }    
	            stringBuilder.append(hv);    
	        }    
	        return stringBuilder.toString();    
	    }    
	  
    public final static String getFileTypeByStream(byte[] b)    
    {    
        String filetypeHex = String.valueOf(getFileHexString(b));    
        Iterator<Entry<String, String>> entryiterator = FILE_TYPE_MAP.entrySet().iterator();    
        while (entryiterator.hasNext()) {    
            Entry<String,String> entry =  entryiterator.next();    
            String fileTypeHexValue = entry.getValue();    
            if (filetypeHex.toUpperCase().startsWith(fileTypeHexValue)) {    
            	
            	
                return entry.getKey();    
            }    
        }    
        return null;    
    }  
}
