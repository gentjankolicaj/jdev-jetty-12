package io.jdev.jetty;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;

/**
 * @author gentjan kolicaj
 * @Date: 11/22/24 2:04 PM
 */
public class JettyHandlerWrapper extends ContextHandler {

  public JettyHandlerWrapper(Handler handler, String contextPath) {
    super(handler, contextPath);
  }

  /**
   * @param vHost       "foo.company.com"
   * @param handler     jetty handler
   * @param contextPath jetty handler path
   */
  public JettyHandlerWrapper(String vHost, Handler handler, String contextPath) {
    super(handler, contextPath);
    this.setVirtualHosts(List.of(vHost));
  }

  /**
   * @param vHosts      "foo.company.com","bar.company.com"
   * @param handler     jetty handler
   * @param contextPath jetty handler path
   */
  public JettyHandlerWrapper(List<String> vHosts, Handler handler, String contextPath) {
    super(handler, contextPath);
    this.setVirtualHosts(vHosts);
  }

  /**
   * @param handler       jetty handler
   * @param contextPath   jetty handler path
   * @param connectorName jetty connector name with added prefix `@` Ex: @ +
   *                      connectorName=@connectorName
   */
  public JettyHandlerWrapper(Handler handler, String contextPath, String connectorName) {
    super(handler, contextPath);
    this.setVirtualHosts(List.of(createVirtualHost(connectorName)));
  }

  /**
   * @param handler        jetty handler
   * @param contextPath    jetty handler path
   * @param connectorNames jetty connector name with added prefix `@` Ex: @ +
   *                       connectorA=@connectorA
   */
  public JettyHandlerWrapper(Handler handler, String contextPath, List<String> connectorNames) {
    super(handler, contextPath);
    this.setVirtualHosts(
        connectorNames.stream().map(JettyHandlerWrapper::createVirtualHost).toList());
  }

  protected static String createVirtualHost(String connectorName) {
    char at = '@';
    if (StringUtils.isEmpty(connectorName)) {
      throw new JettyException("Connector name not valid : " + connectorName);
    }
    return connectorName.charAt(0) == at ? connectorName : at + connectorName;
  }

}
