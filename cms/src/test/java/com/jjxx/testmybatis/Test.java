package com.jjxx.testmybatis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	/*	try {
			downloadNet();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		AePlayWave apw=new AePlayWave("F:/KuGou/wav/111.wav");
		apw.start();
	}

	
	 public static void downloadNet() throws MalformedURLException {
	        // 下载网络文件
	        int bytesum = 0;
	        int byteread = 0;
	        URL url = new URL("http://localhost:8080/cms/upload/attachment/f33a08e5fe704cf482bef1f4b9ec4d2520170731171759.mp4");
	        try {
	            URLConnection conn = url.openConnection();
	            InputStream inStream = conn.getInputStream();
	            FileOutputStream fs = new FileOutputStream("D:/2.mp4");
	            byte[] buffer = new byte[1024];
	            int length;
	            while ((byteread = inStream.read(buffer)) != -1) {
	                bytesum += byteread;
	                System.out.println(byteread+"==="+bytesum);
	                fs.write(buffer, 0, byteread);
	            }
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
}

class AePlayWave extends Thread {

	private String filename;
	public AePlayWave(String wavfile) {
		filename = wavfile;

	}

	public void run() {

		File soundFile = new File(filename);

		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		AudioFormat format = audioInputStream.getFormat();
		SourceDataLine auline = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

		try {
			auline = (SourceDataLine) AudioSystem.getLine(info);
			auline.open(format);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		auline.start();
		int nBytesRead = 0;
		byte[] abData = new byte[512];
		try {
			while (nBytesRead != -1) {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0)
					auline.write(abData, 0, nBytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			auline.drain();
			auline.close();
		}

	}
}
