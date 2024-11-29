package io.jdev.jetty;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.conscrypt.Conscrypt;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 6:51 PM
 */
@Slf4j
public class JettyServer {

  private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
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
    Arrays.stream(webAppContexts).forEach(this.contextHandlers::addHandler);
  }

  public JettyServer(JettyServerProperties serverProperties,
      ServletContextHandler... servletContextHandlers) {
    this.serverProperties = serverProperties;
    if (ArrayUtils.isEmpty(servletContextHandlers)) {
      throw new IllegalArgumentException("ServletContextHandlers can't be empty !");
    }
    this.contextHandlers = new ContextHandlerCollection();
    Arrays.stream(servletContextHandlers).forEach(this.contextHandlers::addHandler);
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
      connectors.add(createConnector(server, connectorProperties));
    }
    //add all connectors to server
    server.setConnectors(connectors.toArray(new Connector[0]));
  }

  protected Connector createConnector(Server server, ConnectorProperties connectorProps) {
    Optional<HttpProperties> optionalHttpProperties = connectorProps.getHttp();
    Connector connector;
    if (optionalHttpProperties.isPresent()) {
      HttpProperties httpProps = optionalHttpProperties.get();
      if (httpProps.getVersion().isPresent()) {
        HttpVersion httpVersion = httpProps.getVersion().get();
        if (HttpVersion.HTTP_1_0.equals(httpVersion)) {
          connector = getConnectorHttp11(server, connectorProps, httpProps);
        } else if (HttpVersion.HTTP_1_1.equals(httpVersion)) {
          connector = getConnectorHttp11(server, connectorProps, httpProps);
        } else if (HttpVersion.HTTP_2.equals(httpVersion)) {
          connector = getConnectorHttp2(server, connectorProps, httpProps);
        } else {
          connector = getConnectorHttpUnsecure(server, connectorProps);
        }
      } else {
        connector = getConnectorHttpUnsecure(server, connectorProps);
      }
    } else {
      connector = getConnectorHttpUnsecure(server, connectorProps);
    }
    return connector;
  }

  private ServerConnector getConnectorHttp11(Server server,
      ConnectorProperties connectorProperties, HttpProperties httpProperties) {
    HttpConfiguration httpConfig = createHttpConfiguration(httpProperties);

    Optional<SSLProperties> optionalSSL = httpProperties.getSsl();
    ServerConnector connector;
    if (optionalSSL.isPresent()) {
      SSLProperties sslProperties = optionalSSL.get();

      // Add the SecureRequestCustomizer because TLS is used.
      // Note: disabled sniHostCheck
      //https://stackoverflow.com/questions/69945173/org-eclipse-jetty-http-badmessageexception-400-invalid-sni
      httpConfig.addCustomizer(new SecureRequestCustomizer(false));

      // The ConnectionFactory for HTTP/1.1.
      HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

      //KeyStore resource
      Resource keyStoreResource = findKeyStore(sslProperties.getKeyStoreFile(),
          ResourceFactory.of(server));

      // Configure the SslContextFactory with the keyStore information.
      SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
      sslContextFactory.setKeyStoreResource(keyStoreResource);
      sslContextFactory.setKeyStorePassword(sslProperties.getKeyStorePassword());
      sslContextFactory.setKeyManagerPassword(sslProperties.getKeyPassword());

      // The ConnectionFactory for TLS.
      SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, http11.getProtocol());

      // The ServerConnector instance.
      connector = new ServerConnector(server, tls, http11);
    } else {

      // The ConnectionFactory for HTTP/1.1.
      HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

      // The ServerConnector instance.
      connector = new ServerConnector(server, http11);
    }

    // ServerConnector setup
    if (StringUtils.isNotEmpty(connectorProperties.getName())) {
      connector.setName(connectorProperties.getName());
    }
    if (StringUtils.isNotEmpty(connectorProperties.getHost())) {
      connector.setHost(connectorProperties.getHost());
    }
    if (connectorProperties.getPort() != 0) {
      connector.setPort(connectorProperties.getPort());
    }
    connector.setIdleTimeout(getTimeout(connectorProperties.getIdleTimeout()));
    return connector;

  }

  private ServerConnector getConnectorHttpUnsecure(Server server,
      ConnectorProperties connectorProperties) {
    HttpConfiguration httpConfig = new HttpConfiguration();

    // The ConnectionFactory for HTTP/1.1.
    HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

    // The ConnectionFactory for clear-text HTTP/2.
    HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);

    //http connector
    ServerConnector connector = new ServerConnector(server, http11, http2);
    if (StringUtils.isNotEmpty(connectorProperties.getName())) {
      connector.setName(connectorProperties.getName());
    }
    if (StringUtils.isNotEmpty(connectorProperties.getHost())) {
      connector.setHost(connectorProperties.getHost());
    }
    if (connectorProperties.getPort() != 0) {
      connector.setPort(connectorProperties.getPort());
    }
    connector.setIdleTimeout(getTimeout(connectorProperties.getIdleTimeout()));
    return connector;
  }

  private ServerConnector getConnectorHttp2(Server server,
      ConnectorProperties connectorProperties, HttpProperties httpProperties) {
    HttpConfiguration httpConfig = createHttpConfiguration(httpProperties);

    Optional<SSLProperties> optionalSSL = httpProperties.getSsl();
    ServerConnector connector;
    if (optionalSSL.isPresent()) {
      SSLProperties sslProperties = optionalSSL.get();

      // Add the SecureRequestCustomizer because TLS is used.
      // Note: disabled sniHostCheck
      // https://stackoverflow.com/questions/69945173/org-eclipse-jetty-http-badmessageexception-400-invalid-sni
      httpConfig.addCustomizer(new SecureRequestCustomizer(false));

      //KeyStore resource
      Resource keyStoreResource = findKeyStore(sslProperties.getKeyStoreFile(),
          ResourceFactory.of(server));

      // SSL Context Factory
      SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
      sslContextFactory.setKeyStoreResource(keyStoreResource);
      sslContextFactory.setKeyStorePassword(sslProperties.getKeyStorePassword());
      sslContextFactory.setKeyManagerPassword(sslProperties.getKeyPassword());

      //Configure TLS
      //Because of java.lang.IllegalStateException: Connection rejected: No ALPN Processor
      // for sun.security.ssl.SSLEngineImpl from [org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor@ce5a68e]
      configureTLS(sslContextFactory);

      // Configure the Connector to speak HTTP/1.1 and HTTP/2.
      HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);
      HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpConfig);
      ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
      alpn.setDefaultProtocol(http11.getProtocol());
      SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

      connector = new ServerConnector(server, ssl, alpn, http2, http11);
    } else {

      // The ConnectionFactory for HTTP/1.1.
      HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

      // The ConnectionFactory for clear-text HTTP/2.
      HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);

      //ServerConnector instance
      connector = new ServerConnector(server, http11, http2);
    }
    if (StringUtils.isNotEmpty(connectorProperties.getName())) {
      connector.setName(connectorProperties.getName());
    }
    if (StringUtils.isNotEmpty(connectorProperties.getHost())) {
      connector.setHost(connectorProperties.getHost());
    }
    if (connectorProperties.getPort() != 0) {
      connector.setPort(connectorProperties.getPort());
    }
    connector.setIdleTimeout(getTimeout(connectorProperties.getIdleTimeout()));
    return connector;
  }

  protected void configureTLS(SslContextFactory.Server sslContextFactory) {
    // https://jetty.org/docs/jetty/12/programming-guide/server/http.html#connector-protocol-tls-conscrypt
    Security.insertProviderAt(Conscrypt.newProvider(), 1);
  }


  protected Resource findKeyStore(String resourceName, ResourceFactory resourceFactory) {
    Resource resource = resourceFactory.newClassLoaderResource(resourceName);
    if (!Resources.isReadableFile(resource)) {
      throw new JettyException("Unable to read " + resourceName);
    }
    return resource;
  }


  private long getTimeout(TimeoutProperties timeoutProperties) {
    if (Objects.isNull(timeoutProperties)) {
      return DEFAULT_TIMEOUT;
    }
    return timeoutProperties.timeUnit().toMillis(timeoutProperties.duration());
  }


  protected HttpConfiguration createHttpConfiguration(HttpProperties httpProperties) {
    HttpConfiguration httpConfig = new HttpConfiguration();

    //set values
    httpProperties.getSecureScheme().ifPresent(httpConfig::setSecureScheme);
    httpProperties.getSecurePort().ifPresent(httpConfig::setSecurePort);
    httpProperties.getOutputBufferSize().ifPresent(httpConfig::setOutputBufferSize);
    httpProperties.getRequestHeaderSize().ifPresent(httpConfig::setRequestHeaderSize);
    httpProperties.getResponseHeaderSize().ifPresent(httpConfig::setResponseHeaderSize);
    httpProperties.getSendServerVersion().ifPresent(httpConfig::setSendServerVersion);
    httpProperties.getSendDateHeader().ifPresent(httpConfig::setSendDateHeader);
    return httpConfig;
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
