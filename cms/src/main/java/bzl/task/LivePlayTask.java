package bzl.task;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.alibaba.fastjson.JSONObject;

import bzl.common.Callback;
import bzl.common.SonThread;
import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.common.RedisExpireListener;
import bzl.controller.LiveplayController;
import utils.RedisUtils;

public class LivePlayTask {
	RedisExpireListener newListener = new RedisExpireListener();
	 private static MemoryCache localMemCache = new MemoryCache();

	public void startListenLivePlay() {
		newListener.addCallBack(new Callback() {
			@Override
			public JSONObject callBack(String key, JSONObject valObj) {
				// TODO Auto-generated method stub
				String redisKey = valObj.getString("message");
				System.out.println("!!!!!!!!!!!redisKey==" + redisKey);
				if (redisKey.startsWith(Constant.LivePlaySwitch) && redisKey.endsWith(":on")) {
					String liveId = redisKey.split(":")[3];
					if (liveId != null && liveId.length() > 0) {
						Map<String,String> livePlayInfos = RedisUtils.hgetAll(Constant.LivePlayInfo + liveId);
						
						SonThread startThread = new SonThread();
						startThread.setCallback(new Callback() {
							@Override
							public JSONObject callBack(String key, JSONObject valObj) {
								// TODO Auto-generated method stub
								startFFmpegPushRtmp(liveId,livePlayInfos.get("media_path"),livePlayInfos.get("rtmpUrl"), livePlayInfos.get("stream_type"));
								return null;
							}
						});
						startThread.start();
					}
				}else if(redisKey.startsWith(Constant.LivePlaySwitch) && redisKey.endsWith(":off")) {
					String liveId = redisKey.split(":")[3];
					stopFFmpegPushRtmp(liveId);
				}
				return null;
			}
		});

		SonThread listenThread = new SonThread();
		listenThread.setCallback(new Callback() {
			@Override
			public JSONObject callBack(String key, JSONObject valObj) {
				// TODO Auto-generated method stub
				RedisUtils.getJedis().psubscribe(newListener, "__keyevent@0__:expired");
				return null;
			}
		});
		listenThread.start();

	}

	private void startFFmpegPushRtmp(String live_id,String mediaPath,String rtmpUrl,String streamType) {
		//String ffmpegCmd = "ffmpeg -re -i $1 -vcodec copy -acodec copy -b:v 800k -b:a 32k -f flv $2 >/dev/null 2>&1 &";
//		ffmpeg -i /home/jerrylu/bzlfile/video/aeb387c1176510d9490b75f215fb099c.mp4 -c:a copy -c:v libx264 -b:v 320k  -f flv "rtmp://192.168.101.9/live/h264Stream live=1"

//		String ffmpegCmd = String.format("ffmpeg -re -i %s -s 640×360 -vcodec copy -acodec copy -b:v 48k -b:a 32k -f flv %s", videoPath,rtmpUrl);
		String ffmpegCmd = null;
		
		if(streamType.equals(Constant.MediaVideo +"")) {
			ffmpegCmd = String.format("ffmpeg -re -i %s -c:a copy -c:v libx264 -b:v 320k -b:a 32k  -f flv %s", mediaPath,rtmpUrl);
		}else {
			ffmpegCmd = String.format("ffmpeg -re -i %s -c:a aac -b:a 32k  -f flv %s", mediaPath,rtmpUrl);
		}
		
		
		//String ffmpegCmd = String.format("ffmpeg -re -i %s -s 640×360 -b:v 400k -b:a 32k -f flv %s", videoPath,rtmpUrl);
		Process exec = null;
		try {
			exec = Runtime.getRuntime().exec(new String[] { "bash","-c", ffmpegCmd});
			try {
				Map<String,String> livePlayInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + live_id);
				if(true ==LiveplayController.startOrStopPushRtmp("start", live_id)) {
					localMemCache.setData(live_id, exec, 0);
					livePlayInfo.put("state", Constant.IsPlaying);//２表示正在推流中
					RedisUtils.hset(Constant.LivePlayInfo + live_id, livePlayInfo, 0);
					printProcessMsg(exec);
					if(0== exec.waitFor()) {
						livePlayInfo.put("state", Constant.IsStopPlay);//3表示已经结束
						RedisUtils.hset(Constant.LivePlayInfo + live_id, livePlayInfo, 0);
						LiveplayController.startOrStopPushRtmp("stop", live_id);
						System.out.println("ffmpeg finish!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}else {
						System.out.println("start ffmpeg failed,exitValue=" + exec.exitValue());
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//exec.destroyForcibly();
	}
	
	private void stopFFmpegPushRtmp(String live_id) {
		
		Process exec = localMemCache.getData(live_id);
		if(exec !=null) {
			System.out.println("force stop ffmpeg exec" + exec.toString());
			exec.destroyForcibly();
			if(exec.isAlive()) { //not allways can kill success
				String psCmd = String.format("ps -ef | grep %s | grep -v grep | awk '{print $2}'",live_id);
	            try {
					Process ffmpegProcess = Runtime.getRuntime().exec(new String[] { "bash","-c", psCmd});
					String pid = printProcessMsg(ffmpegProcess);
					System.out.println("ffmpeg pid===" + pid);
					if(pid !=null && pid.length() >0) {
						String killCmd =  String.format("kill -9 %s",pid);
						Runtime.getRuntime().exec(new String[] { "bash","-c", killCmd});
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Map<String,String> livePlayInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + live_id);
		System.out.println("force stop ffmpeg!!" + livePlayInfo.toString());
		livePlayInfo.put("state", Constant.IsStopPlay);//3表示推流结束
		RedisUtils.hset(Constant.LivePlayInfo + live_id, livePlayInfo, 0);
	}

	/**
	 * 处理process输出流和错误流，防止进程阻塞，在process.waitFor();前调用
	 * 
	 * @param exec
	 * @throws IOException
	 */
	private String printProcessMsg(Process exec) throws IOException {
		// 防止ffmpeg进程塞满缓存造成死锁
		InputStream error = exec.getErrorStream();
		InputStream is = exec.getInputStream();

		StringBuffer result = new StringBuffer();
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(error, "GBK"));
			BufferedReader br2 = new BufferedReader(new InputStreamReader(is, "GBK"));

			while (exec.isAlive() && (line = br.readLine()) != null) {
				result.append(line + "\n");
			}
		   //System.out.println("进程错误信息：" + result.toString());

			result = new StringBuffer();
			line = null;

			while (exec.isAlive() && (line = br2.readLine()) != null) {
				result.append(line + "\n");
			}
			//System.out.println("进程输出内容为：" + result.toString());
		} catch (IOException e2) {
			e2.printStackTrace();
		} finally {
			error.close();
			is.close();
		}
		
		return result.toString();
	}

	private static Timer timer = new Timer();

	public static void startTaskTimer() {

		timer.schedule(new TimerTask() {
			public void run() {
				SocketMsgHandler socketMsg = new SocketMsgHandler();

				Set<String> onlineKeys = RedisUtils.getKeys(Constant.OnlineTerminals);
				for (String tmpKey : onlineKeys) {
					String onlineInfos = RedisUtils.get(tmpKey);
					String terminal_ip = onlineInfos.split(":")[0];
				}
			}
		}, 1000, 300 * 1000); // 12小时检查一次
	}

}

