package utils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
/**
* @author leon
* @createDate 2019年2月12日 下午1:57:06
* @version v1.0
* @classRemarks 压缩zip方法
*/
public class ZipUtil {
	static final int BUFFER = 8192;
	
	  public static void unZipDirToFiles(File zipFile,String descDir)throws IOException{  
	        File pathFile = new File(descDir);  
	        if(!pathFile.exists()){  
	            pathFile.mkdirs();  
	        }  
	        ZipFile zip = new ZipFile(zipFile);  
	        for(Enumeration entries = zip.entries();entries.hasMoreElements();){  
	            ZipEntry entry = (ZipEntry)entries.nextElement();  
	            //获取压缩目录名称
	            String zipEntryName = entry.getName();  
	            InputStream in = zip.getInputStream(entry);  
//	            String outPath = (descDir+"//"+zipEntryName).replaceAll("\\*", "/");
	            String outPath = descDir +zipEntryName;
	            System.out.println(outPath);  
	            //判断路径是否存在,不存在则创建文件路径  
	            File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));  
	            if(!file.exists()){  
	                file.mkdirs();  
	            }  
	            //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压  
	            if(new File(outPath).isDirectory()){  
	                continue;  
	            }  
	            //输出文件路径信息  
	           // System.out.println(outPath);  
	              
	            OutputStream out = new FileOutputStream(outPath);  
	            byte[] buf1 = new byte[1024];  
	            int len;  
	            while((len=in.read(buf1))>0){  
	                out.write(buf1,0,len);  
	            }  
	            in.close();  
	            out.close();  
	            }  
	        System.out.println("******************解压完毕********************");  
	    }  
	  

	public static void compress(String srcPath , String dstPath) throws IOException{
	    File srcFile = new File(srcPath);
	    File dstFile = new File(dstPath);
	    if (!srcFile.exists()) {
	        throw new FileNotFoundException(srcPath + "不存在！");
	    }

	    FileOutputStream out = null;
	    ZipOutputStream zipOut = null;
	    try {
	        out = new FileOutputStream(dstFile);
	        CheckedOutputStream cos = new CheckedOutputStream(out,new CRC32());
	        zipOut = new ZipOutputStream(cos);
	        String baseDir = "";
	        compress(srcFile, zipOut, baseDir);
	    }
	    finally {
	        if(null != zipOut){
	            zipOut.close();
	            out = null;
	        }

	        if(null != out){
	            out.close();
	        }
	    }
	}

	private static void compress(File file, ZipOutputStream zipOut, String baseDir) throws IOException{
	    if (file.isDirectory()) {
	        compressDirectory(file, zipOut, baseDir);
	    } else {
	        compressFile(file, zipOut, baseDir);
	    }
	}

	/** 压缩一个目录 */
	private static void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir) throws IOException{
	    File[] files = dir.listFiles();
	    for (int i = 0; i < files.length; i++) {
	        compress(files[i], zipOut, baseDir + dir.getName() + "/");
	    }
	}

	/** 压缩一个文件 */
	private static void compressFile(File file, ZipOutputStream zipOut, String baseDir)  throws IOException{
	    if (!file.exists()){
	        return;
	    }

	    BufferedInputStream bis = null;
	    try {
	        bis = new BufferedInputStream(new FileInputStream(file));
	        ZipEntry entry = new ZipEntry(baseDir + file.getName());
	        zipOut.putNextEntry(entry);
	        int count;
	        byte data[] = new byte[BUFFER];
	        while ((count = bis.read(data, 0, BUFFER)) != -1) {
	            zipOut.write(data, 0, count);
	        }

	    }finally {
	        if(null != bis){
	            bis.close();
	        }
	    }
	}
}