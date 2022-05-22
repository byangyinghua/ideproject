package bzl.entity;

/**
 * 一些项目配置信息
 */
public class LocalProjectConfig {
    private static LocalProjectConfig localProjectConfig;

    public static LocalProjectConfig getLocalProjectConfig(){
        return localProjectConfig;
    }

    public static void initLocalProjectConfig(LocalProjectConfig config){
        localProjectConfig = config;
    }

    private String project_code;

    public String getProject_code(){
        return project_code;
    }

    public void setProject_code(String project_code){
        this.project_code = project_code;
    }
}
