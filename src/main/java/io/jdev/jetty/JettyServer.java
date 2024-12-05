package io.jdev.jetty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 6:51 PM
 */
@Slf4j
public class JettyServer {

  protected final JettyServerProperties serverProperties;
  protected final ContextHandlerCollection contextHandlers;
  protected Server server;

  public JettyServer(JettyServerProperties serverProperties,
      ContextHandlerCollection contextHandlers) {
    this.serverProperties = serverProperties;
    this.contextHandlers = contextHandlers;
  }

  public JettyServer(JettyServerProperties serverProperties, Handler... handlers) {
    this.serverProperties = serverProperties;
    if (ArrayUtils.isEmpty(handlers)) {
      throw new IllegalArgumentException("Handler's can't be empty !");
    }
    this.contextHandlers = new ContextHandlerCollection();
    Arrays.stream(handlers).forEach(this.contextHandlers::addHandler);
  }

  public JettyServer(JettyServerProperties serverProperties, ContextHandler... contextHandlers) {
    this.serverProperties = serverProperties;
    if (ArrayUtils.isEmpty(contextHandlers)) {
      throw new IllegalArgumentException("ContextHandler's can't be empty !");
    }
    this.contextHandlers = new ContextHandlerCollection(contextHandlers);
  }

  public JettyServer(JettyServerProperties serverProperties, WebAppContext... webAppContexts) {
    this.serverProperties = serverProperties;
    if (ArrayUtils.isEmpty(webAppContexts)) {
      throw new IllegalArgumentException("WebAppContexts can't be empty !");
    }
    this.contextHandlers = new ContextHandlerCollection();
    this.contextHandlers.setHandlers(webAppContexts);
  }

  public JettyServer(JettyServerProperties serverProperties,
      ServletContextHandler... servletContextHandlers) {
    this.serverProperties = serverProperties;
    if (ArrayUtils.isEmpty(servletContextHandlers)) {
      throw new IllegalArgumentException("ServletContextHandlers can't be empty !");
    }
    this.contextHandlers = new ContextHandlerCollection();
    this.contextHandlers.setHandlers(servletContextHandlers);
  }


  private void bootstrap() {
    //Thread pool setup
    final QueuedThreadPool threadPool = createThreadPool(this.serverProperties.getThreadPool());

    //server setup
    this.server = createServer(threadPool, this.serverProperties);

    //connector setup
    createConnectors(this.server, this.serverProperties.getConnectors());

    //context handlers setup
    setupHandlers(this.server, this.contextHandlers, this.serverProperties);

  }

  protected void setupHandlers(Server server, ContextHandlerCollection contextHandlers,
      JettyServerProperties jettyServerProperties) {
    if (contextHandlers == null) {
      log.warn("Context handlers not set.");
    } else {
      server.setHandler(contextHandlers);

      //set handler to secured redirect
      if (jettyServerProperties.getSecuredRedirect().isPresent()) {
        Boolean securedRedirect = jettyServerProperties.getSecuredRedirect().get();
        if (securedRedirect) {
          server.setHandler(new SecuredRedirectHandler());
        }
      }
    }
  }

  protected void createConnectors(Server server,
      List<ConnectorProperties> connectorPropertiesList) {
    if (server == null) {
      throw new JettyException("Server instance can't be null.");
    }

    if (connectorPropertiesList == null || connectorPropertiesList.isEmpty()) {
      throw new JettyException("Connector properties can't be null.");
    }
    List<Connector> connectors = new ArrayList<>();

    for (int i = 0, len = connectorPropertiesList.size(); i < len; i++) {
      ConnectorProperties connectorProperties = connectorPropertiesList.get(i);
      connectors.add(JettyServerUtils.createServerConnector(server, connectorProperties));
    }
    //add all connectors to server
    server.setConnectors(connectors.toArray(new Connector[0]));
  }


  protected Server createServer(QueuedThreadPool threadPool,
      JettyServerProperties jettyServerProperties) {
    Server server = new Server(threadPool);
    server.setDumpAfterStart(jettyServerProperties.isDumpAfterStart());
    server.setDumpBeforeStop(jettyServerProperties.isDumpBeforeStop());
    server.setStopAtShutdown(jettyServerProperties.isStopAtShutdown());
    server.setStopTimeout(jettyServerProperties.getStopTimeout());
    return server;
  }

  protected QueuedThreadPool createThreadPool(ThreadPoolProperties threadPoolProperties) {
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setName(threadPoolProperties.poolName());
    threadPool.setDaemon(threadPoolProperties.daemonThreads());
    threadPool.setMinThreads(threadPoolProperties.minThreads());
    threadPool.setMaxThreads(threadPoolProperties.maxThreads());
    threadPool.setReservedThreads(threadPoolProperties.reservedThreads());
    threadPool.setIdleTimeout(threadPoolProperties.idleTimeout());
    threadPool.setStopTimeout(threadPoolProperties.stopTimeout());
    return threadPool;
  }


  public void start() throws Exception {
    bootstrap();
    if (server != null) {
      server.start();
    }
  }

  public void join() throws Exception {
    if (server != null) {
      server.join();
    }

  }

  public void stop() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

}
