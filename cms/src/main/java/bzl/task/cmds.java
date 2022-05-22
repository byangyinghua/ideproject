package bzl.task;


public class cmds {

    /** 服务器广播命令，局域网内的终端通过此命令获取到服务起的IP，然后在注册到服务器来*/
    public static final int BROADCAST = 0x01;

    /** 终端主动注册到服务器命令，收到此命令后，将等级终端信息到数据库 */
    public static final int TERMINAL_REGISTER = 0x02;

    /**终端心跳命令  */
    public static final int HEARTBEAT = 0x03;

    /** 下终端下发文本播放任务 */
    public static final int TEXT_JOB = 0x04;

    /** 向终端下发图片播放任务 */
    public static final int IMAGE_JOB = 0x05;

    /** 向终端下发音频播放任务 */
    public static final int AUDIO_JOB = 0x06;

    /**向终端下发视频播放任务  */
    public static final int VIDEO_JOB = 0x07;

    /** 向终端下发删除任务 */
    public static final int DELETE_JOBS = 0x08;

    /** 终端报告任务文件下载结果 */
    public static final int REPORT_FILEDOWNLOAD_STATUS = 0x09;
    
    /** 向终端下发任务删除文件命令*/
    public static final int DELETE_FILES = 0x0a;
    
    /** 校准终端时间*/
    public static final int AJUST_TERMINAL_TIME = 0x0b;
    
    /** 获取终端自动开关机时间 */
    public static final int GET_BOOTSHUT_TIME = 0x0c;

    /** 设置终端自动开机和关机时间 */
    public static final int SET_BOOTSHUT_TIME = 0x0d;
    
    /** 设置终端音量 */
    public static final int SET_VOLUME = 0x0e;
    
    /** 获取终端音量 */
    public static final int GET_VOLUME = 0x0f;
    
    /** 获取终端任务列表*/
    public static final int GET_TASK = 0x10;
    
    /** 设置或者取消终端屏蔽任务*/
    public static final int SET_SHIELD = 0x11;
    
    /** 设备上报求助信息*/
    public static final int REPORT_HELP = 0x12;
    
    /** 获取终端求助视频列表*/
    public static final int GET_HELP_RECORD = 0x13;
    
    /** 请求终端上传求助视频*/
    public static final int UPLOAD_HELP_VIDEO= 0x14;
    
    /** 设置终端直播非代理模式*/
    public static final int TERMINAL_DIRECT_VIDEO= 0x15;
    
    /** 设置终端直播服务器代理模式*/
    public static final int SERVER_AGENCY_VIDEO= 0x16;
    
    /** 服务器下发终端升级指令*/
    public static final int UPLOAD_TERMINAL_APP= 0x17;
    
    /** 终端上报升级进度*/
    public static final int TERMINAL_REPORT_UPLOAD_PROCESS= 0x18;
    
    /** 终端上报异常信息*/
    public static final int TERMINAL_REPORT_ERR_LOG= 0x19;
    
    /** 终端检查是否有app新版本*/
    public static final int TERMINAL_CHECK_UPDATE= 0x20;
    
    /** 立即重启终端*/
    public static final int REBOOT_TERMINAL_NOW= 0x21;
    /** 设置灯自动开关*/
    public static final int LAMP_SETTING= 0x22;
    
    //新的定时任务设置接口
    public static final int SCHEDULE_TASK=0X23;
    //终端向后台检测任务状态
    public static final int CHECK_TASK_STATUS=0X24;
    
    //一键开关终端外接所有的灯
    public static final int LAMP_ON_OFF=0x25;

}