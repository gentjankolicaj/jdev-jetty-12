package io.jdev.jetty;

import static io.jdev.jetty.JettyBuilderTest.TestEndpoint.URI;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.junit.jupiter.api.Test;

/**
 * @author gentjan kolicaj
 * @Date: 11/29/24 1:51 PM
 */
@Slf4j
class JettyBuilderTest {

  @Test
  void newServletBuilder() {
    ServletContextHandler servletContextHandler1 = JettyBuilder.newServletBuilder()
        .securityOption()
        .contextPath("/")
        .servlet(TestServlet.class, TestServlet.SERVLET_PATH)
        .filter(TestFilter.class, TestServlet.SERVLET_PATH)
        .build();
    assertThat(servletContextHandler1).isNotNull();

    ServletContextHandler servletContextHandler2 = JettyBuilder.newServletBuilder()
        .sessionOption()
        .contextPath("/")
        .servlet(TestServlet.class, TestServlet.SERVLET_PATH)
        .filter(TestFilter.class, TestServlet.SERVLET_PATH)
        .build();
    assertThat(servletContextHandler2).isNotNull();
  }

  @Test
  void newWebSocketBuilder() {
    ServletContextHandler servletContextHandler1 = JettyBuilder.newWebSocketBuilder()
        .sessionOption()
        .contextPath("/")
        .servlet(TestServlet.class, TestServlet.SERVLET_PATH)
        .filter(TestFilter.class, TestServlet.SERVLET_PATH)
        .endpoint(TestEndpoint.class)
        .build();
    assertThat(servletContextHandler1).isNotNull();

    ServletContextHandler servletContextHandler2 = JettyBuilder.newWebSocketBuilder()
        .securityOption()
        .contextPath("/")
        .servlet(TestServlet.class, TestServlet.SERVLET_PATH)
        .filter(TestFilter.class, TestServlet.SERVLET_PATH)
        .endpoint(TestEndpoint.class)
        .build();
    assertThat(servletContextHandler2).isNotNull();
  }


  static class TestServlet extends HttpServlet {

    public static final String SERVLET_PATH = "/hello_world";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      Reader reader = req.getReader();
      PrintWriter writer = resp.getWriter();

      //read from request
      new ReadTask(reader).run();

      //write  from response
      new WriteTask(writer).run();
    }


    @RequiredArgsConstructor
    static class ReadTask {

      private final Reader reader;

      public void run() {
        try {
          List<Integer> characters = new ArrayList<>();
          int character;
          while ((character = reader.read()) != -1) {
            characters.add(character);
          }
          log.info("read from request : {}", characters);
        } catch (IOException ioe) {
          log.debug("", ioe);
        }
      }
    }


    @RequiredArgsConstructor
    static class WriteTask {

      private final PrintWriter writer;
      private final String html = "Hello World";

      public void run() {
        log.info("writing to response:");
        String formatedHtml = String.format(html, TestServlet.class.getSimpleName(),
            getClass().getSimpleName(),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        writer.println(formatedHtml);
        //flush writer after finished writing.
        writer.flush();
      }
    }
  }


  public static class TestFilter implements Filter {

    public static final String API_KEY_HEADER_KEY = "X-API-KEY";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
      Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      if (!(request instanceof HttpServletRequest httpServletRequest
          && response instanceof HttpServletResponse httpServletResponse)) {
        throw new ServletException("non-HTTP request or response");
      }

      //==================================================
      //Do something with request
      //validate api-key from header
      String apiKeyValue = httpServletRequest.getHeader(API_KEY_HEADER_KEY);
      if (isValidAPIKEY(apiKeyValue)) {
        log.info("API-KEY valid: {}", apiKeyValue);

        //Proceed to the next filter in the chain to be invoked
        chain.doFilter(request, response);
      } else {
        httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "api-key invalid.");
        log.warn("API-KEY invalid");
      }


    }

    @Override
    public void destroy() {
      Filter.super.destroy();
    }

    private boolean isValidAPIKEY(String apiKey) {
      //validate api key with a db
      return true;
    }
  }


  @ServerEndpoint(URI)
  public static class TestEndpoint {

    public static final String URI = "/hello";
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
      log.info("server open session: {}", session);
      this.session = session;
      new Thread(() -> pushHello()).start();
    }

    @OnMessage
    public void onText(String message) {
      log.warn("server received-message : {}", message);
    }

    @OnClose
    public void onClose(CloseReason close) {
      log.info("server close reason {}", close);
      this.session = null;
    }

    private void pushHello() {
      String helloMessage = "Hello";
      while (this.session != null) {
        try {
          this.session.getBasicRemote().sendText(helloMessage);
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException | IOException e) {
          e.printStackTrace();
        }
      }
    }

  }


}