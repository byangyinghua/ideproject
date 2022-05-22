package bzl.websocket;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

 /**
* 继承websocket配置类，将httpsession放入ServerEndpointConfig的map中
* 从而达到使websocket对象可以访问到httpsession中的对象
*/
 public class WebSocketHttpSession extends Configurator{ 
 /**
     *      * 重写修改握手方法
     * @param sec
    * @param request
    * @param response
    */
@Override
 public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
//	   System.out.println("WebSocketHttpSession1111:" + HttpSession.class.getName());
//       HttpSession httpSession = (HttpSession)request.getHttpSession();
//        sec.getUserProperties().put(HttpSession.class.getName(),httpSession);

 	  /*如果没有监听器,那么这里获取到的HttpSession是null*/
	HttpSession session = (HttpSession) request.getHttpSession();
	sec.getUserProperties().put(HttpSession.class.getName(), session);
	
    System.out.println("HttpSession.class.getName()==:" +HttpSession.class.getName());
    System.out.println("WebSocketHttpSession2222:" +session.getAttribute("usession"));
//		sec.getUserProperties().put("name", "小强");
		super.modifyHandshake(sec, request, response);
    }

}
