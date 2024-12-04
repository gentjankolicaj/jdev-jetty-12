package io.jdev.jetty;

import static org.assertj.core.api.Assertions.assertThat;

import io.jdev.jackson.YamlConfigurations;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 10:47 PM
 */
@Slf4j
class JettyServerTest extends SSLTest {

  JettyServer jettyServer;

  @AfterEach
  void testClean() throws Exception {
    jettyServer.stop();
  }

  @Test
  void jettyYaml() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class, "/jetty.yaml");
    jettyServer = new JettyServer(jettyProperties.getJettyServer(),
        new TestHandler("test  message"));

    jettyServer.start();

    Awaitility.await()
        .timeout(Duration.ofSeconds(2))
        .pollDelay(Duration.ofSeconds(1))
        .untilAsserted(() -> {
          jettyServer.stop();
        });

    //blocking join until close is called.
    jettyServer.join();
  }


  @Test
  void jettyHttpVersionsYaml() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");
    jettyServer = new JettyServer(jettyProperties.getJettyServer(),
        new TestHandler("test  message"));

    jettyServer.start();

    // Create a HttpClient instance
    HttpClient client = HttpClient.newHttpClient();
    String scheme = "http";
    String host = "localhost";
    int port = 8081;
    String path = "";

    // Create a http/1.1 request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_1_1)
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.version()).isEqualTo(Version.HTTP_1_1);

    // Create a http/2 request
    //change port because http2 is on different port & connector
    port = 8082;
    HttpRequest request2 = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_2)
        .build();

    HttpClient httpClient2 = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build();
    HttpResponse<String> response2 = httpClient2.send(request2,
        HttpResponse.BodyHandlers.ofString());
    assertThat(response2.statusCode()).isEqualTo(200);
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

  @Test
  void jettyHttpYaml() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http.yaml");
    jettyServer = new JettyServer(jettyProperties.getJettyServer(),
        new TestHandler("test  message"));

    jettyServer.start();

    // Create a HttpClient instance
    HttpClient client = HttpClient.newHttpClient();
    String scheme = "http";
    String host = "localhost";
    int port = 8081;
    String path = "";

    // Create a http/1.1 request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_1_1)
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.version()).isEqualTo(Version.HTTP_1_1);

    Awaitility.await()
        .timeout(Duration.ofSeconds(2))
        .pollDelay(Duration.ofSeconds(1))
        .untilAsserted(() -> {
          jettyServer.stop();
        });

    //blocking join until close is called.
    jettyServer.join();
  }

  @Test
  void jettyHttpsVersionsYaml() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_https_versions.yaml");
    jettyServer = new JettyServer(jettyProperties.getJettyServer(),
        new TestHandler("test  message"));

    //start jetty server
    jettyServer.start();

    // Create a custom SSLContext that trusts all certificates
    SSLContext sslContext = createSSLContext(DUMMY_TRUST_MANAGER);

    // Build the HttpClient with the custom SSLContext
    HttpClient httpsClient = HttpClient.newBuilder()
        .sslContext(sslContext)
        .build();

    String scheme = "https";
    String host = "127.0.0.1";
    int port = 8444;
    String path = "/";

    // Create a https/1.1 request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_1_1)
        .build();
    HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.version()).isEqualTo(Version.HTTP_1_1);

    // Create a http/2 request
    //change port because http2 is on different port & connector
    port = 8445;
    HttpRequest request2 = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_2)
        .build();

    HttpClient httpClient2 = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .sslContext(sslContext)
        .build();
    HttpResponse<String> response2 = httpClient2.send(request2,
        HttpResponse.BodyHandlers.ofString());
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

  @Test
  void jettyHttpsYaml() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_https.yaml");

    //start jetty server
    jettyServer = new JettyServer(jettyProperties.getJettyServer(),
        new TestHandler("test  message"));
    jettyServer.start();

    // Create a custom SSLContext that trusts all certificates
    SSLContext sslContext = createSSLContext(DUMMY_TRUST_MANAGER);

    // Build the HttpClient with the custom SSLContext
    HttpClient httpsClient = HttpClient.newBuilder()
        .sslContext(sslContext)
        .version(Version.HTTP_2)
        .build();

    String scheme = "https";
    String host = "127.0.0.1";
    int port = 8443;
    String path = "/";

    // Create a https/1.1 request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("%s://%s:%d%s", scheme, host, port, path)))
        .GET()
        .version(Version.HTTP_2)
        .build();
    HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.version()).isEqualTo(Version.HTTP_2);

    Awaitility.await()
        .timeout(Duration.ofSeconds(2))
        .pollDelay(Duration.ofSeconds(1))
        .untilAsserted(() -> {
          jettyServer.stop();
        });

    //blocking join until close is called.
    jettyServer.join();
  }

  @RequiredArgsConstructor
  public class TestHandler extends Handler.Abstract {

    private final String message;

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
      response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain; charset=utf-8");
      response.write(true, BufferUtil.toBuffer(message), callback);
      return true;
    }

  }

}