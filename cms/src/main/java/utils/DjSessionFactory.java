package utils;

import java.io.File;
import java.io.IOException;  
import java.io.Reader;  

import org.apache.ibatis.io.Resources;  
import org.apache.ibatis.session.SqlSessionFactory;  
import org.apache.ibatis.session.SqlSessionFactoryBuilder;  
import org.springframework.core.io.ClassPathResource;
public final class DjSessionFactory {  
  private String resource="config/SqlMapper.xml";  
  private SqlSessionFactory sqlSessionFactory=null;  
  private static DjSessionFactory sessionFactory=new DjSessionFactory();  
  private DjSessionFactory(){
      try {
    	  ClassPathResource resource1 = new ClassPathResource(resource);
    	  String path=resource1.getPath();
    	  System.out.println("path==="+path);
          Reader reader=Resources.getResourceAsReader(path);  
          sqlSessionFactory=new SqlSessionFactoryBuilder().build(reader);  
         // sqlSessionFactory=new SqlSessionFactoryBuilder().build(reader, properties);
          //SqlSessionFactory build(Reader reader, Properties properties)
      } catch (IOException e) {
          System.out.println("#IOException happened in initialising the SessionFactory:"+e.getMessage());  
          throw new ExceptionInInitializerError(e);  
      }  
  }
  
  public static DjSessionFactory getInstance() {  
      return sessionFactory;  
  }
  
  public SqlSessionFactory getSqlSessionFactory() {  
      return sqlSessionFactory;  
  }  
}  