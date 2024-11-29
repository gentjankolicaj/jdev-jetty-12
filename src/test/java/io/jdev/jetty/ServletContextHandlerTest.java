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
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 10:47 PM
 */
@SuppressWarnings("all")
@Slf4j
class ServletContextHandlerTest {

  JettyServer jettyServer;


  @AfterEach
  void testClean() throws Exception {
    if (jettyServer != null) {
      jettyServer.stop();
    }
  }

  @Test
  void noServletContextHandlers() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");

    assertThatThrownBy(() -> new JettyServer(jettyProperties.getJettyServer(),
        (ServletContextHandler[]) null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ServletContextHandlers can't be empty !");

    assertThatThrownBy(() -> new JettyServer(jettyProperties.getJettyServer(),
        new ServletContextHandler[0])).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ServletContextHandlers can't be empty !");
  }

  @Test
  void defaultServlet() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");

    // Setup the basic application "context" for this application at "/"
    // This is also known as the handler tree (in jetty speak)
    ServletContextHandler servletContextHandler = new ServletContextHandler(
        ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/");

    // Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
    // It is important that this is last.
    ServletHolder holderDef = new ServletHolder("default", DefaultServlet.class);
    holderDef.setInitParameter("dirAllowed", "true");
    servletContextHandler.addServlet(holderDef, "/");

    //jetty server creation
    jettyServer = new JettyServer(jettyProperties.getJettyServer(),
        servletContextHandler);
    jettyServer.start();

    // Create a HttpClient instance
    HttpClient client = HttpClient.newHttpClient();
    String scheme = "http";
    String host = "localhost";
    int port = 8081;
    String path = "/";

    // Create a http/1.1 request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_1_1)
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.version()).isEqualTo(Version.HTTP_1_1);

    // Create a http/2 request
    //change port because http2 is on different port & connector
    port = 8082;
    HttpRequest request2 = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_2)
        .build();
    HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
    assertThat(response2.statusCode()).isEqualTo(404);
    assertThat(response2.version()).isEqualTo(Version.HTTP_2);

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