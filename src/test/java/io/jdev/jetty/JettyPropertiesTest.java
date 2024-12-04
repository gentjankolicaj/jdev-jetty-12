package io.jdev.jetty;

import static org.assertj.core.api.Assertions.assertThat;

import io.jdev.jackson.ConfigurationException;
import io.jdev.jackson.YamlConfigurations;
import java.util.List;
import java.util.Optional;
import org.eclipse.jetty.http.HttpVersion;
import org.junit.jupiter.api.Test;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 5:51 PM
 */
@SuppressWarnings("all")
class JettyPropertiesTest {


  @Test
  void jettyYamlTest() throws ConfigurationException {
    JettyProperties jettyProps = YamlConfigurations.load(JettyProperties.class, "/jetty.yaml");
    assertThat(jettyProps).isNotNull();
    assertThat(jettyProps.getJettyServer()).isNotNull();

    //Thread pool test
    ThreadPoolProperties threadPoolProps = jettyProps.getJettyServer().getThreadPool();
    assertThat(threadPoolProps).isNotNull();
    assertThat(threadPoolProps.poolName()).isEqualTo("jetty-pool");
    assertThat(threadPoolProps.daemonThreads()).isTrue();
    assertThat(threadPoolProps.minThreads()).isEqualTo(4);
    assertThat(threadPoolProps.maxThreads()).isEqualTo(50);
    assertThat(threadPoolProps.reservedThreads()).isEqualTo(1);
    assertThat(threadPoolProps.idleTimeout()).isEqualTo(10000);
    assertThat(threadPoolProps.stopTimeout()).isEqualTo(10000);

    List<ConnectorProperties> connectorPropsList = jettyProps.getJettyServer().getConnectors();
    assertThat(connectorPropsList).isNotNull().hasSize(1);

    //connector properties
    ConnectorProperties connectorProps = connectorPropsList.get(0);
    assertThat(connectorProps).isNotNull();
    assertThat(connectorProps.getName()).isEqualTo("first-connector");
    assertThat(connectorProps.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps.getPort()).isEqualTo(8080);
    assertThat(connectorProps.getIdleTimeout()).isNotNull();
    assertThat(connectorProps.getIdleTimeout().duration()).isEqualTo(120);

    //http config
    HttpProperties httpProperties = (HttpProperties) connectorProps.getHttpConfig().get();
    assertThat(httpProperties.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties.getOutputBufferSize().get()).isEqualTo(32768);
  }


  @Test
  void jettyNoHttpYamlTest() throws ConfigurationException {
    JettyProperties jettyProps = YamlConfigurations.load(JettyProperties.class,
        "/jetty_no_protocol.yaml");
    assertThat(jettyProps).isNotNull();
    assertThat(jettyProps.getJettyServer()).isNotNull();

    //Thread pool test
    ThreadPoolProperties threadPoolProps = jettyProps.getJettyServer().getThreadPool();
    assertThat(threadPoolProps).isNotNull();
    assertThat(threadPoolProps.poolName()).isEqualTo("jetty-pool");
    assertThat(threadPoolProps.daemonThreads()).isTrue();
    assertThat(threadPoolProps.minThreads()).isEqualTo(4);
    assertThat(threadPoolProps.maxThreads()).isEqualTo(50);
    assertThat(threadPoolProps.reservedThreads()).isEqualTo(1);
    assertThat(threadPoolProps.idleTimeout()).isEqualTo(10000);
    assertThat(threadPoolProps.stopTimeout()).isEqualTo(10000);

    List<ConnectorProperties> connectorPropsList = jettyProps.getJettyServer().getConnectors();
    assertThat(connectorPropsList).isNotNull().hasSize(1);

    //first connector properties
    ConnectorProperties connectorProps = connectorPropsList.get(0);
    assertThat(connectorProps).isNotNull();
    assertThat(connectorProps.getName()).isEqualTo("first-connector");
    assertThat(connectorProps.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps.getPort()).isEqualTo(8080);
    assertThat(connectorProps.getIdleTimeout()).isNotNull();
    assertThat(connectorProps.getIdleTimeout().duration()).isEqualTo(120);

    //http config
    Optional<HttpConfigProperties> optional = connectorProps.getHttpConfig();
    assertThat(optional).isEmpty();

  }

  @Test
  void jettySSLMultipleConnectorsYamlTest() throws ConfigurationException {
    JettyProperties jettyProps = YamlConfigurations.load(JettyProperties.class,
        "/jetty_ssl_multiple_connectors.yaml");
    assertThat(jettyProps).isNotNull();
    assertThat(jettyProps.getJettyServer()).isNotNull();

    //Thread pool test
    ThreadPoolProperties threadPoolProps = jettyProps.getJettyServer().getThreadPool();
    assertThat(threadPoolProps).isNotNull();
    assertThat(threadPoolProps.poolName()).isEqualTo("jetty-pool");
    assertThat(threadPoolProps.daemonThreads()).isTrue();
    assertThat(threadPoolProps.minThreads()).isEqualTo(4);
    assertThat(threadPoolProps.maxThreads()).isEqualTo(50);
    assertThat(threadPoolProps.reservedThreads()).isEqualTo(1);
    assertThat(threadPoolProps.idleTimeout()).isEqualTo(10000);
    assertThat(threadPoolProps.stopTimeout()).isEqualTo(10000);

    List<ConnectorProperties> connectorPropsList = jettyProps.getJettyServer().getConnectors();
    assertThat(connectorPropsList).isNotNull().hasSize(2);

    //first connector properties
    ConnectorProperties connectorProps = connectorPropsList.get(0);
    assertThat(connectorProps).isNotNull();
    assertThat(connectorProps.getName()).isEqualTo("first-connector");
    assertThat(connectorProps.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps.getPort()).isEqualTo(8443);
    assertThat(connectorProps.getIdleTimeout()).isNotNull();
    assertThat(connectorProps.getIdleTimeout().duration()).isEqualTo(120);

    //http config
    HttpsProperties httpsProperties = (HttpsProperties) connectorProps.getHttpConfig().get();
    assertThat(httpsProperties.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpsProperties.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpsProperties.getOutputBufferSize().get()).isEqualTo(32768);

    SSLProperties sslProperties = httpsProperties.getSsl().get();
    assertThat(sslProperties).isNotNull();
    assertThat(sslProperties.getKeyStorePath()).isNotNull().isEqualTo("ssl/keystore.p12");
    assertThat(sslProperties.getKeyStorePassword()).isNotNull().isEqualTo("1234567");
    assertThat(sslProperties.getKeyManagerPassword()).isNullOrEmpty();

    //second connector properties
    ConnectorProperties connectorProps2 = connectorPropsList.get(1);
    assertThat(connectorProps2).isNotNull();
    assertThat(connectorProps2.getName()).isEqualTo("second-connector");
    assertThat(connectorProps2.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps2.getPort()).isEqualTo(8444);
    assertThat(connectorProps2.getIdleTimeout()).isNotNull();
    assertThat(connectorProps2.getIdleTimeout().duration()).isEqualTo(120);

    //second http config properties
    HttpsProperties httpsProperties2 = (HttpsProperties) connectorProps2.getHttpConfig().get();
    assertThat(httpsProperties2.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpsProperties2.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpsProperties2.getOutputBufferSize().get()).isEqualTo(32768);

    //second ssl properties
    SSLProperties sslProperties2 = httpsProperties2.getSsl().get();
    assertThat(sslProperties2).isNotNull();
    assertThat(sslProperties2.getKeyStorePath()).isNotNull().isEqualTo("ssl/keystore.p12");
    assertThat(sslProperties2.getKeyStorePassword()).isNotNull().isEqualTo("1234567");
    assertThat(sslProperties2.getKeyManagerPassword()).isNullOrEmpty();
  }

  @Test
  void jettyHttpVersionsYamlTest() throws ConfigurationException {
    JettyProperties jettyProps = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");
    assertThat(jettyProps).isNotNull();
    assertThat(jettyProps.getJettyServer()).isNotNull();

    //Thread pool test
    ThreadPoolProperties threadPoolProps = jettyProps.getJettyServer().getThreadPool();
    assertThat(threadPoolProps).isNotNull();
    assertThat(threadPoolProps.poolName()).isEqualTo("jetty-pool");
    assertThat(threadPoolProps.daemonThreads()).isTrue();
    assertThat(threadPoolProps.minThreads()).isEqualTo(4);
    assertThat(threadPoolProps.maxThreads()).isEqualTo(50);
    assertThat(threadPoolProps.reservedThreads()).isEqualTo(1);
    assertThat(threadPoolProps.idleTimeout()).isEqualTo(10000);
    assertThat(threadPoolProps.stopTimeout()).isEqualTo(10000);

    List<ConnectorProperties> connectorPropsList = jettyProps.getJettyServer().getConnectors();
    assertThat(connectorPropsList).isNotNull().hasSize(2);

    //first connector properties
    ConnectorProperties connectorProps = connectorPropsList.get(0);
    assertThat(connectorProps).isNotNull();
    assertThat(connectorProps.getName()).isEqualTo("first-connector");
    assertThat(connectorProps.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps.getPort()).isEqualTo(8081);
    assertThat(connectorProps.getIdleTimeout()).isNotNull();
    assertThat(connectorProps.getIdleTimeout().duration()).isEqualTo(120);

    //first http config
    HttpProperties httpProperties = (HttpProperties) connectorProps.getHttpConfig().get();
    assertThat(httpProperties.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties.getOutputBufferSize().get()).isEqualTo(32768);
    assertThat(httpProperties.getVersion().get()).isEqualTo(HttpVersion.HTTP_1_1);

    //second connector properties
    ConnectorProperties connectorProps2 = connectorPropsList.get(1);
    assertThat(connectorProps2).isNotNull();
    assertThat(connectorProps2.getName()).isEqualTo("second-connector");
    assertThat(connectorProps2.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps2.getPort()).isEqualTo(8082);
    assertThat(connectorProps2.getIdleTimeout()).isNotNull();
    assertThat(connectorProps2.getIdleTimeout().duration()).isEqualTo(120);

    //second http config properties
    HttpProperties httpProperties2 = (HttpProperties) connectorProps2.getHttpConfig().get();
    assertThat(httpProperties2.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties2.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties2.getOutputBufferSize().get()).isEqualTo(32768);
    assertThat(httpProperties2.getVersion().get()).isEqualTo(HttpVersion.HTTP_2);

  }


  @Test
  void jettyProtocolYamlTest() throws ConfigurationException {
    JettyProperties jettyProps = YamlConfigurations.load(JettyProperties.class,
        "/jetty_protocols.yaml");
    assertThat(jettyProps).isNotNull();
    assertThat(jettyProps.getJettyServer()).isNotNull();

    //Thread pool test
    ThreadPoolProperties threadPoolProps = jettyProps.getJettyServer().getThreadPool();
    assertThat(threadPoolProps).isNotNull();
    assertThat(threadPoolProps.poolName()).isEqualTo("jetty-pool");
    assertThat(threadPoolProps.daemonThreads()).isTrue();
    assertThat(threadPoolProps.minThreads()).isEqualTo(4);
    assertThat(threadPoolProps.maxThreads()).isEqualTo(50);
    assertThat(threadPoolProps.reservedThreads()).isEqualTo(1);
    assertThat(threadPoolProps.idleTimeout()).isEqualTo(10000);
    assertThat(threadPoolProps.stopTimeout()).isEqualTo(10000);

    List<ConnectorProperties> connectorPropsList = jettyProps.getJettyServer().getConnectors();
    assertThat(connectorPropsList).isNotNull().hasSize(2);

    //first connector properties
    ConnectorProperties connectorProps = connectorPropsList.get(0);
    assertThat(connectorProps).isNotNull();
    assertThat(connectorProps.getName()).isEqualTo("first-connector");
    assertThat(connectorProps.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps.getPort()).isEqualTo(8080);
    assertThat(connectorProps.getIdleTimeout()).isNotNull();
    assertThat(connectorProps.getIdleTimeout().duration()).isEqualTo(120);

    //http config
    HttpProperties httpProperties = (HttpProperties) connectorProps.getHttpConfig().get();
    assertThat(httpProperties.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties.getOutputBufferSize().get()).isEqualTo(32768);

    //second connector properties
    ConnectorProperties connectorProps2 = connectorPropsList.get(1);
    assertThat(connectorProps2).isNotNull();
    assertThat(connectorProps2.getName()).isEqualTo("second-connector");
    assertThat(connectorProps2.getHost()).isEqualTo("127.0.0.1");
    assertThat(connectorProps2.getPort()).isEqualTo(8443);
    assertThat(connectorProps2.getIdleTimeout()).isNotNull();
    assertThat(connectorProps2.getIdleTimeout().duration()).isEqualTo(120);

    //second http config properties
    HttpsProperties httpProperties2 = (HttpsProperties) connectorProps2.getHttpConfig().get();
    assertThat(httpProperties2.getResponseHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties2.getRequestHeaderSize().get()).isEqualTo(8192);
    assertThat(httpProperties2.getOutputBufferSize().get()).isEqualTo(32768);

    //second ssl properties
    SSLProperties sslProperties2 = httpProperties2.getSsl().get();
    assertThat(sslProperties2).isNotNull();
    assertThat(sslProperties2.getKeyStorePath()).isNotNull().isEqualTo("ssl/keystore.p12");
    assertThat(sslProperties2.getKeyStorePassword()).isNotNull().isEqualTo("1234567");
    assertThat(sslProperties2.getKeyManagerPassword()).isNullOrEmpty();
  }

}