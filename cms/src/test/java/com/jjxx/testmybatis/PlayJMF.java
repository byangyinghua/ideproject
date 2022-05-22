package com.jjxx.testmybatis;
import java.awt.BorderLayout;  
import java.awt.Component;  
import java.awt.Dimension;  
import java.awt.Frame;  
import java.awt.Panel;  
import java.awt.event.WindowAdapter;  
import java.awt.event.WindowEvent;  
import java.io.IOException;  
import java.net.MalformedURLException;  
import java.net.URL;  
  
import javax.media.CannotRealizeException;  
import javax.media.ControllerEvent;  
import javax.media.ControllerListener;  
import javax.media.EndOfMediaEvent;  
import javax.media.Manager;  
import javax.media.MediaLocator;  
import javax.media.NoPlayerException;  
import javax.media.Player;  
import javax.media.PrefetchCompleteEvent;  
import javax.media.RealizeCompleteEvent;  
import javax.media.Time;  
public class PlayJMF implements ControllerListener{
	 public static void main(String[] args) {  
		 	VideoConverter vc=new VideoConverter();
		 	vc.convert("1234.mp4");
		  
	    }  
	     
	    private Player mediaPlayer;  
	    private Frame f;  
	    private Player player;  
	    private Panel panel;  
	    private Component visual;  
	    private Component control = null;  
	     
	    public void play(){  
	        f = new Frame("JMF Sample1");  
	        f.addWindowListener(new WindowAdapter() {  
	            public void windowClosing(WindowEvent we) {  
	                if(player != null) {  
	                    player.close();  
	                }  
	                System.exit(0);  
	            }  
	        });  
	        f.setSize(500,400);  
	        f.setVisible(true);  
	        URL url = null;  
	        try {  
	            //准备一个要播放的视频文件的URL  
	            url = new URL("file:/F:/KuGou/1234.mp4");  
	        } catch (MalformedURLException e) {  
	            e.printStackTrace();  
	        }         
	        try {  
	            //通过调用Manager的createPlayer方法来创建一个Player的对象  
	            //这个对象是媒体播放的核心控制对象  
	            player = Manager.createPlayer(url);  
	        } catch (NoPlayerException e1) {  
	            e1.printStackTrace();  
	        } catch (IOException e1) {  
	            e1.printStackTrace();  
	        }   
	  
	        //对player对象注册监听器，能噶偶在相关事件发生的时候执行相关的动作  
	        player.addControllerListener(this);  
	         
	        //让player对象进行相关的资源分配  
	        player.realize();  
	    }  
	     
	    private int videoWidth = 0;  
	    private int videoHeight = 0;  
	    private int controlHeight = 30;  
	    private int insetWidth = 10;  
	    private int insetHeight = 30;  
	     
	    //监听player的相关事件  
	    public void controllerUpdate(ControllerEvent ce) {  
	        if (ce instanceof RealizeCompleteEvent) {  
	            //player实例化完成后进行player播放前预处理  
	            player.prefetch();  
	        } else if (ce instanceof PrefetchCompleteEvent) {  
	            if (visual != null)  
	                return;  
	  
	            //取得player中的播放视频的组件，并得到视频窗口的大小  
	            //然后把视频窗口的组件添加到Frame窗口中，  
	            if ((visual = player.getVisualComponent()) != null) {  
	                Dimension size = visual.getPreferredSize();  
	                videoWidth = size.width;  
	                videoHeight = size.height;  
	                f.add(visual);  
	            } else {  
	                videoWidth = 320;  
	            }  
	             
	            //取得player中的视频播放控制条组件，并把该组件添加到Frame窗口中  
	            if ((control = player.getControlPanelComponent()) != null) {  
	                controlHeight = control.getPreferredSize().height;  
	                f.add(control, BorderLayout.SOUTH);  
	            }  
	             
	            //设定Frame窗口的大小，使得满足视频文件的默认大小  
	            f.setSize(videoWidth + insetWidth, videoHeight + controlHeight + insetHeight);  
	            f.validate();  
	            
	            //启动视频播放组件开始播放  
	            player.start();  
	            mediaPlayer.start();  
	        } else if (ce instanceof EndOfMediaEvent) {  
	            //当播放视频完成后，把时间进度条恢复到开始，并再次重新开始播放  
	            player.setMediaTime(new Time(0));  
	            player.start();  
	        }  
	    }  
	  
	}  