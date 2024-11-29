package io.jdev.jetty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jdev.jackson.YamlConfigurations;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 10:47 PM
 */
@Slf4j
class WebAppContextTest {

  JettyServer jettyServer;

  @AfterEach
  void testClean() throws Exception {
    if (jettyServer != null) {
      jettyServer.stop();
    }
  }

  @Test
  void noWebAppContext() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");

    assertThatThrownBy(() -> new JettyServer(jettyProperties.getJettyServer(),
        (WebAppContext[]) null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("WebAppContexts can't be empty !");

    assertThatThrownBy(() -> new JettyServer(jettyProperties.getJettyServer(),
        new WebAppContext[0])).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("WebAppContexts can't be empty !");
  }


  @Test
  void webAppContext() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");

    //create webapp (WAR) config
    WebAppContext webAppA = new WebAppContext();
    webAppA.setContextPath("/a");
    webAppA.setWar("./src/test/resources/webapps/webapp-a");

    //jetty server creation
    jettyServer = new JettyServer(jettyProperties.getJettyServer(), webAppA);
    jettyServer.start();

    // Create a HttpClient instance
    HttpClient client = HttpClient.newHttpClient();
    String scheme = "http";
    String host = "localhost";
    int port = 8081;
    String path = "/a/";

    // Create a http/1.1 request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_1_1)
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.version()).isEqualTo(Version.HTTP_1_1);
    assertThat(response.body()).contains("Welcome to WebApp-A");

    // Create a http/2 request
    //change port because http2 is on different port & connector
    port = 8082;
    HttpRequest request2 = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_2)
        .build();
    HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
    assertThat(response2.statusCode()).isEqualTo(200);
    assertThat(response2.version()).isEqualTo(Version.HTTP_2);
    assertThat(response2.body()).contains("Welcome to WebApp-A");

    Awaitility.await()
        .timeout(Duration.ofSeconds(2))
        .pollDelay(Duration.ofSeconds(1))
        .untilAsserted(() -> {
          jettyServer.stop();
        });

    //blocking join until close is called.
    jettyServer.join();
  }


}