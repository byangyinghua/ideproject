package utils;

import org.apache.log4j.Logger;

public class Log {
    private final static Logger logger = Logger.getLogger("BoyaoCms");
    private final static StringBuilder sb = new StringBuilder();

    public synchronized static void d(String tag , String log){
        sb.setLength(0);
        sb.append("（").append(tag).append("） --- ").append(log);
        logger.debug(sb);
    }

    public synchronized static void e(String tag , String log){
        sb.setLength(0);
        sb.append("（").append(tag).append("） --- ").append(log);
        logger.error(sb);
    }

    public synchronized static void i(String tag , String log){
        sb.setLength(0);
        sb.append("（").append(tag).append("） --- ").append(log);
        logger.info(sb);
    }
}
