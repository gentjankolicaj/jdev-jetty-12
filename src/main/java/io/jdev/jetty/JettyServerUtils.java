package io.jdev.jetty;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * @author gentjan kolicaj
 * @Date: 12/4/24 3:06 PM
 */
public final class JettyServerUtils {

  private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);


  private JettyServerUtils() {
  }



  static Resource findKeyStore(String resourceName, ResourceFactory resourceFactory) {
    Resource resource = resourceFactory.newClassLoaderResource(resourceName);
    if (!Resources.isReadableFile(resource)) {
      throw new JettyException("Unable to read " + resourceName);
    }
    return resource;
  }


  public static long getTimeout(TimeoutProperties timeoutProperties) {
    if (Objects.isNull(timeoutProperties)) {
      return DEFAULT_TIMEOUT;
    }
    return timeoutProperties.timeUnit().toMillis(timeoutProperties.duration());
  }

  public static ServerConnector createServerConnector(Server server,
      ConnectorProperties connectorProperties) {
    Optional<HttpConfigProperties> optionalHttpConfig = connectorProperties.getHttpConfig();
    if (optionalHttpConfig.isPresent()) {
      HttpConfigProperties abstractProperties = optionalHttpConfig.get();
      if (abstractProperties instanceof HttpsProperties) {
        return createHttpsConnector(server, connectorProperties,
            (HttpsProperties) abstractProperties);
      } else if (abstractProperties instanceof HttpProperties) {
        return createHttpConnector(server, connectorProperties,
            (HttpProperties) abstractProperties);
      } else {
        throw new IllegalArgumentException(
            "Protocol 'type' unknown.It must be 'http' or 'https' !!!");
      }
    } else {
      throw new IllegalArgumentException(
          "Protocol unknown for connector !!!.Please define protocol.");
    }
  }


  public static ServerConnector createHttpConnector(Server server,
      ConnectorProperties connectorProps,
      HttpProperties httpProperties) {
    HttpConfiguration httpConfig = createHttpConfiguration(httpProperties);
    ServerConnector connector;
    //When http version is not specified or unknown , http/1.1 & http/2 is default
    if (httpProperties.getVersion().isPresent()) {
      HttpVersion httpVersion = httpProperties.getVersion().get();
      if (HttpVersion.HTTP_1_0.equals(httpVersion)
          || HttpVersion.HTTP_0_9.equals(httpVersion)
          || HttpVersion.HTTP_1_1.equals(httpVersion)) {
        // The ConnectionFactory for HTTP/1.1.
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

        // The ServerConnector instance.
        connector = new ServerConnector(server, http11);
      } else if (HttpVersion.HTTP_2.equals(httpVersion)) {
        //Because of :
        //  java.io.IOException: protocol_error/invalid_preface
        //	at org.eclipse.jetty.http2.HTTP2Session.toFailure(HTTP2Session.java:651)
        // The ConnectionFactory for HTTP/1.1.
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

        // The ConnectionFactory for clear-text HTTP/2.
        HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);

        //http connector
        connector = new ServerConnector(server, http11, http2);
      } else {
        // The ConnectionFactory for HTTP/1.1.
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

        // The ConnectionFactory for clear-text HTTP/2.
        HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);

        //http connector
        connector = new ServerConnector(server, http11, http2);
      }

    } else {
      // The ConnectionFactory for HTTP/1.1.
      HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

      // The ConnectionFactory for clear-text HTTP/2.
      HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);

      //http connector
      connector = new ServerConnector(server, http11, http2);
    }

    if (StringUtils.isNotEmpty(connectorProps.getName())) {
      connector.setName(connectorProps.getName());
    }
    if (StringUtils.isNotEmpty(connectorProps.getHost())) {
      connector.setHost(connectorProps.getHost());
    }
    if (connectorProps.getPort() != 0) {
      connector.setPort(connectorProps.getPort());
    }
    connector.setIdleTimeout(JettyServerUtils.getTimeout(connectorProps.getIdleTimeout()));
    return connector;
  }

  private static HttpConfiguration createHttpConfiguration(HttpProperties httpProperties) {
    HttpConfiguration httpConfig = new HttpConfiguration();

    //set values
    httpProperties.getOutputBufferSize().ifPresent(httpConfig::setOutputBufferSize);
    httpProperties.getRequestHeaderSize().ifPresent(httpConfig::setRequestHeaderSize);
    httpProperties.getResponseHeaderSize().ifPresent(httpConfig::setResponseHeaderSize);
    httpProperties.getSendServerVersion().ifPresent(httpConfig::setSendServerVersion);
    httpProperties.getSendDateHeader().ifPresent(httpConfig::setSendDateHeader);
    return httpConfig;
  }

  //===============================================================================================
  //HTTPS connectors

  public static ServerConnector createHttpsConnector(Server server,
      ConnectorProperties connectorProperties, HttpsProperties httpsProperties) {
    HttpConfiguration httpsConfig = createHttpsConfiguration(httpsProperties);
    // Add the SecureRequestCustomizer because TLS is used.
    // Note: disabled sniHostCheck
    //https://stackoverflow.com/questions/69945173/org-eclipse-jetty-http-badmessageexception-400-invalid-sni
    httpsConfig.addCustomizer(new SecureRequestCustomizer(false));

    Optional<SSLProperties> optionalSSL = httpsProperties.getSsl();
    ServerConnector connector;
    if (optionalSSL.isPresent()) {
      SSLProperties sslProperties = optionalSSL.get();

      // SSL Context Factory
      SslContextFactory.Server sslContextFactory = createSSLContextFactory(server, sslProperties);

      //==========================================================================
      //when https version is not specified/unknown, http/1.1 & http/2 is default
      if (httpsProperties.getVersion().isPresent()) {
        HttpVersion httpVersion = httpsProperties.getVersion().get();
        if (HttpVersion.HTTP_1_0.equals(httpVersion)
            || HttpVersion.HTTP_0_9.equals(httpVersion)
            || HttpVersion.HTTP_1_1.equals(httpVersion)) {
          // The ConnectionFactory for HTTP/1.1.
          HttpConnectionFactory http11 = new HttpConnectionFactory(httpsConfig);

          // The ConnectionFactory for TLS.
          SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory,
              http11.getProtocol());

          // The ServerConnector instance.
          connector = new ServerConnector(server, tls, http11);
        } else if (HttpVersion.HTTP_2.equals(httpVersion)) {

          // The ConnectionFactory for HTTP/2.
          HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);

          // The ALPN ConnectionFactory.
          ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
          // The default protocol to use in case there is no negotiation.
          alpn.setDefaultProtocol(h2.getProtocol());

          // The ConnectionFactory for TLS.
          SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory,
              alpn.getProtocol());

          // The ServerConnector instance.
          connector = new ServerConnector(server, tls, alpn, h2);
        } else {
          // Configure the Connector to speak HTTP/1.1 and HTTP/2.
          HttpConnectionFactory http11 = new HttpConnectionFactory(httpsConfig);
          HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpsConfig);
          ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
          alpn.setDefaultProtocol(http11.getProtocol());
          SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory,
              alpn.getProtocol());

          //https connector
          connector = new ServerConnector(server, ssl, alpn, http2, http11);
        }
      } else {
        // Configure the Connector to speak HTTP/1.1 and HTTP/2.
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpsConfig);
        HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpsConfig);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(http11.getProtocol());
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        //https connector
        connector = new ServerConnector(server, ssl, alpn, http2, http11);
      }
    } else {
      throw new IllegalArgumentException("SSL properties not found.Please configure SSL.");
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
    connector.setIdleTimeout(JettyServerUtils.getTimeout(connectorProperties.getIdleTimeout()));
    return connector;
  }


  private static HttpConfiguration createHttpsConfiguration(HttpsProperties httpProperties) {
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

  private static SslContextFactory.Server createSSLContextFactory(Server server,
      SSLProperties sslProperties) {
    //Configure TLS
    //Because of java.lang.IllegalStateException: Connection rejected: No ALPN Processor
    // for sun.security.ssl.SSLEngineImpl from [org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor@ce5a68e]

    //KeyStore resource
    Resource keyStoreResource = JettyServerUtils.findKeyStore(sslProperties.getKeyStorePath(),
        ResourceFactory.of(server));

    // SSL Context Factory
    SslContextFactory.Server factory = new SslContextFactory.Server();
    factory.setKeyStoreResource(keyStoreResource);
    factory.setKeyStorePassword(sslProperties.getKeyStorePassword());
    factory.setKeyManagerPassword(sslProperties.getKeyManagerPassword());

    if (sslProperties.getNeedClientAuth().isPresent()) {
      factory.setNeedClientAuth(sslProperties.getNeedClientAuth().get());
    }

    if (sslProperties.getWantClientAuth().isPresent()) {
      factory.setWantClientAuth(sslProperties.getWantClientAuth().get());
    }

    if (sslProperties.getCertAlias().isPresent()) {
      factory.setCertAlias(sslProperties.getCertAlias().get());
    }

    if (sslProperties.getJceProvider().isPresent()) {
      factory.setProvider(sslProperties.getJceProvider().get());
    }

    if (sslProperties.getValidateCerts().isPresent()) {
      factory.setValidateCerts(sslProperties.getValidateCerts().get());
    }
    if (sslProperties.getValidatePeers().isPresent()) {
      factory.setValidatePeerCerts(sslProperties.getValidatePeers().get());
    }

    if (CollectionUtils.isNotEmpty(sslProperties.getIncludedProtocols())) {
      factory.setIncludeProtocols(sslProperties.getIncludedProtocols().toArray(new String[0]));
    }

    if (CollectionUtils.isNotEmpty(sslProperties.getExcludedProtocols())) {
      factory.setExcludeProtocols(sslProperties.getExcludedProtocols().toArray(new String[0]));
    }

    if (CollectionUtils.isNotEmpty(sslProperties.getIncludedCipherSuites())) {
      factory.setIncludeCipherSuites(
          sslProperties.getIncludedCipherSuites().toArray(new String[0]));
    }

    if (CollectionUtils.isNotEmpty(sslProperties.getExcludedCipherSuites())) {
      factory.setExcludeCipherSuites(
          sslProperties.getExcludedCipherSuites().toArray(new String[0]));
    }

    return factory;
  }


}
