package com.jjxx.testmybatis;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
public class VideoConverter {
	  private final static String BASE_PATH = "F:\\KuGou\\";
      private final static String FLV_PATH = "F:\\KuGou\\flv\\";
      private static String PATH = "";
      private static String FILE_NAME = "";
       
      private static final Log log = LogFactory.getLog(com.jjxx.testmybatis.VideoConverter.class);

      public static boolean convert(String fileName) {
              PATH = BASE_PATH + fileName;
              FILE_NAME = fileName;
              if(log.isDebugEnabled()){
                      log.debug("start to convert video to flv format...");
                      log.debug("the file name is : " + FILE_NAME);
                      log.debug("the file path is : " + PATH);
              }

              if (!checkfile(PATH)) {
                      return false;
              } else {
                      if (process()) {
                              if(log.isDebugEnabled()){
                                      log.debug("process() ok");
                              }
                              return true;
                      } else {
                              return false;
                      }
              }
      }

      private static boolean process() {
              int type = checkContentType();
              boolean status = false;
              if (type == 0) {
                      if(log.isDebugEnabled()){
                              log.debug("Start to convert to flv file");
                      }
                      status = processFLV(PATH);// 直接将文件转为flv文件
              } else if (type == 1) {
                      String avifilepath = processAVI(type);
                      if (avifilepath == null){
                              return false;// avi文件没有得到
                      }
                      status = processFLV(avifilepath);// 将avi转为flv
              } else if(type == 9){
                      if(log.isDebugEnabled()){
                              log.debug("this file is no need to convert.");
                      }
                      return false;
              }
              return status;
      }

      private static int checkContentType() {
              String type = PATH.substring(PATH.lastIndexOf(".") + 1, PATH.length())
                              .toLowerCase();
              // ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
              if (type.equals("avi")) {
                      return 0;
              } else if (type.equals("mpg")) {
                      return 0;
              } else if (type.equals("wmv")) {
                      return 0;
              } else if (type.equals("3gp")) {
                      return 0;
              } else if (type.equals("mov")) {
                      return 0;
              } else if (type.equals("mp4")) {
                      return 0;
              } else if (type.equals("asf")) {
                      return 0;
              } else if (type.equals("asx")) {
                      return 0;
              } else if (type.equals("flv")) {
                      return 0;
              }
              // 对ffmpeg无法解析的文件格式(wmv9，rm，rmvb等),
              // 可以先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式.
              else if (type.equals("wmv9")) {
                      return 1;
              } else if (type.equals("rm")) {
                      return 1;
              } else if (type.equals("rmvb")) {
                      return 1;
              }
              return 9;
      }

      // check file
      private static boolean checkfile(String path) {
              File file = new File(path);
              if (!file.isFile()) {
                      return false;
              }
              return true;
      }

      // 对ffmpeg无法解析的文件格式(wmv9，rm，rmvb等), 可以先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式.
      private static String processAVI(int type) {
              List<String> commend = new ArrayList<String>();
              commend.add(FLV_PATH + "/mencoder");
              commend.add(PATH);
              commend.add("-oac");
              commend.add("lavc");
              commend.add("-lavcopts");
              commend.add("acodec=mp3:abitrate=64");
              commend.add("-ovc");
              commend.add("xvid");
              commend.add("-xvidencopts");
              commend.add("bitrate=600");
              commend.add("-of");
              commend.add("avi");
              commend.add("-o");
              commend.add(FLV_PATH + FILE_NAME.substring(0,FILE_NAME.lastIndexOf(".")) + ".avi");
              try {
                      ProcessBuilder builder = new ProcessBuilder();
                      builder.command(commend);
                      builder.start();
                      return FLV_PATH + FILE_NAME.substring(0,FILE_NAME.lastIndexOf(".")) + ".avi";
              } catch (Exception e) {
                      e.printStackTrace();
                      return null;
              }
      }

      // ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
      private static boolean processFLV(String oldfilepath) {
              if (!checkfile(PATH)) {
                      return false;
              }
              List<String> commend = new ArrayList<String>();
              commend.add(FLV_PATH + "ffmpeg");
              commend.add("-i");
              commend.add(oldfilepath);
              commend.add("-ab");
              commend.add("56");
              commend.add("-ar");
              commend.add("22050");
              commend.add("-qscale");
              commend.add("8");
              commend.add("-r");
              commend.add("15");
              commend.add("-s");
              commend.add("600x500");
              commend.add(FLV_PATH + FILE_NAME.substring(0,FILE_NAME.lastIndexOf(".")) + ".flv");
              try {
                      Runtime runtime = Runtime.getRuntime();
                      String cmd = "";
                      String cut = FLV_PATH + "ffmpeg.exe -i "
                                      + oldfilepath
                                      + " -y -f image2 -ss 8 -t 0.001 -s 600x500 "
                                      + FLV_PATH
                                      + FILE_NAME.substring(0,FILE_NAME.lastIndexOf("."))
                                      + ".jpg";
                      String cutCmd = cmd + cut;
                      runtime.exec(cutCmd);
                      ProcessBuilder builder = new ProcessBuilder(commend);
                      builder.command(commend);
                      Process process = builder.start();
                      int i = doWaitFor(process);
                      if(i == 0){
                              if(log.isDebugEnabled()){
                                      log.debug("ffmpeg has finished.");
                              }
                      }
                      return true;
              } catch (Exception e) {
                      e.printStackTrace();
                      return false;
              }
      }
       
      public static int doWaitFor(Process p) {
       int exitValue = -1; // returned to caller when p is finished
       try {

       InputStream in = p.getInputStream();
       InputStream err = p.getErrorStream();
       boolean finished = false; // Set to true when p is finished

       while(!finished){
       try {
       while( in.available() > 0) {
       // Print the output of our system call
       Character c = new Character( (char) in.read());
       if(log.isDebugEnabled()){
               log.debug( c);
       }
       }
       while( err.available() > 0) {
       // Print the output of our system call
       Character c = new Character( (char) err.read());
       if(log.isDebugEnabled()){
               log.debug( c);
       }
       }

       // Ask the process for its exitValue. If the process
       // is not finished, an IllegalThreadStateException
       // is thrown. If it is finished, we fall through and
       // the variable finished is set to true.
       exitValue = p.exitValue();
       finished = true;
       }
       catch (IllegalThreadStateException e) {
       Thread.currentThread();
                                      // Process is not finished yet;
       // Sleep a little to save on CPU cycles
       Thread.sleep(500);
       }
       }
       }
       catch (Exception e) {
       // unexpected exception! print it out for debugging...
               if(log.isErrorEnabled()){
                       log.error("doWaitFor(): unexpected exception - " +
       e.getMessage());
               }
       if(log.isErrorEnabled()){
               log.error(e.getMessage());
       }
       }
       // return completion status to caller
       return exitValue;
      }
}