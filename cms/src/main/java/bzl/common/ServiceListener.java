package bzl.common;

import bzl.task.SocketMsgHandler;
import utils.Log;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServiceListener implements ServletContextListener {
    private static final String TAG = ServiceListener.class.getSimpleName();

    private Thread serviceBackThread = null;
    private boolean isInterrupt = false;

    private final Object mLock = new Object();

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Log.i(TAG , "contextInitialized()");
        isInterrupt = false;
        serviceBackThread = new Thread(autoBroadcastTimeInfo);
        serviceBackThread.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        Log.i(TAG , "contextDestroyed()");
        serviceBackThread.interrupt();
        serviceBackThread = null;
        isInterrupt = true;
    }

    /**
     * 定期广播时间信息，保证终端时间一致性
     */
    private Runnable autoBroadcastTimeInfo = new Runnable(){
        //记录时间变化用
        private long lastMinute = -1;

        @Override
        public void run() {
            while (!isInterrupt){
                long currentTime = System.currentTimeMillis();
                //精确到分钟
                long currentMinute = currentTime / (60 * 1000);
                //整分已经跳转
                if(currentMinute != lastMinute){
                    SocketMsgHandler.getInstance().broastMsg(String.valueOf(currentTime));
                }
                lastMinute = currentMinute;
                synchronized (mLock){
                    try {
                        //为了保证时间高度一致性，设置50毫秒轮询
                        mLock.wait(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
