package io.jdev.jetty;

import io.jdev.jetty.Builder.ServletBuilder;
import io.jdev.jetty.Builder.WebSocketBuilder;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.MutablePair;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;


/**
 * @author gentjan kolicaj
 * @Date: 11/29/24 6:07 PM
 */
public abstract class JettyBuilder {

  protected JettyBuilder() {
  }


  public static JettyServletBuilderImpl newServletBuilder() {
    return new JettyServletBuilderImpl();
  }

  public static JettyWebSocketBuilderImpl newWebSocketBuilder() {
    return new JettyWebSocketBuilderImpl();
  }

  public static class JettyServletBuilderImpl implements ServletBuilder<ServletContextHandler> {

    private final Map<Class<? extends Servlet>, String> servlets = new HashMap<>();
    private final Map<Class<? extends Filter>, String> filters = new HashMap<>();
    private final Map<Class<? extends Filter>, MutablePair<String, EnumSet<DispatcherType>>> filtersWithDispatchers = new HashMap<>();
    private final EnumSet<DispatcherType> defaultDispatchers = EnumSet.allOf(DispatcherType.class);
    private int options = 0;
    private String contextPath = "/";

    @Override
    public JettyServletBuilderImpl sessionOption() {
      this.options = ServletContextHandler.SESSIONS;
      return this;
    }

    @Override
    public JettyServletBuilderImpl securityOption() {
      this.options = ServletContextHandler.SECURITY;
      return this;
    }

    @Override
    public JettyServletBuilderImpl contextPath(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    @Override
    public JettyServletBuilderImpl servlet(Class<? extends Servlet> servlet, String pathSpec) {
      this.servlets.put(servlet, pathSpec);
      return this;
    }


    @Override
    public JettyServletBuilderImpl filter(Class<? extends Filter> filterClass, String pathSpec) {
      this.filters.put(filterClass, pathSpec);
      return this;
    }

    @Override
    public JettyServletBuilderImpl filter(Class<? extends Filter> filterClass, String pathSpec,
        EnumSet<DispatcherType> dispatchers) {
      this.filtersWithDispatchers.put(filterClass, new MutablePair<>(pathSpec, dispatchers));
      return this;
    }

    @Override
    public ServletContextHandler build() {
      ServletContextHandler context = new ServletContextHandler(options);
      context.setContextPath(contextPath);

      //Add servlets
      servlets.forEach((k, v) -> {
        if (k != null && v != null) {
          context.addServlet(k, v);
        }
      });

      //Add filters with default dispatcher
      filters.forEach((key, value) -> {
        if (key != null && value != null) {
          context.addFilter(key, value, defaultDispatchers);
        }
      });

      //Add filters with explicit dispatchers
      filtersWithDispatchers.forEach((key, value) -> {
        if (key != null && value != null && (value.getLeft() != null && value.getRight() != null)) {
          context.addFilter(key, value.getLeft(), value.getRight());
        }
      });

      // Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
      context.addServlet(DefaultServlet.class, "/");
      return context;
    }
  }


  public static class JettyWebSocketBuilderImpl implements WebSocketBuilder<ServletContextHandler> {

    private final Map<Class<? extends Servlet>, String> servlets = new HashMap<>();
    private final Map<Class<? extends Filter>, String> filters = new HashMap<>();
    private final Map<Class<? extends Filter>, MutablePair<String, EnumSet<DispatcherType>>> filtersWithDispatchers = new HashMap<>();
    private final EnumSet<DispatcherType> defaultDispatchers = EnumSet.allOf(DispatcherType.class);
    //websocket fields
    private final Set<Class<?>> endpointClasses = new HashSet<>();
    private final Set<ServerEndpointConfig> serverEndpointConfigs = new HashSet<>();
    private final Map<JettyWebSocketCreator, String> jettyApiEndpointClasses = new HashMap<>();
    private int options = 0;
    private String contextPath = "/";

    @Override
    public JettyWebSocketBuilderImpl sessionOption() {
      this.options = ServletContextHandler.SESSIONS;
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl securityOption() {
      this.options = ServletContextHandler.SECURITY;
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl contextPath(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl servlet(Class<? extends Servlet> servlet, String pathSpec) {
      this.servlets.put(servlet, pathSpec);
      return this;
    }


    @Override
    public JettyWebSocketBuilderImpl filter(Class<? extends Filter> filterClass, String pathSpec) {
      this.filters.put(filterClass, pathSpec);
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl filter(Class<? extends Filter> filterClass, String pathSpec,
        EnumSet<DispatcherType> dispatchers) {
      this.filtersWithDispatchers.put(filterClass, new MutablePair<>(pathSpec, dispatchers));
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl endpoint(Class<?> endpointClass) {
      this.endpointClasses.add(endpointClass);
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl jettyApiEndpoint(JettyWebSocketCreator creator,
        String pathSpec) {
      this.jettyApiEndpointClasses.put(creator, pathSpec);
      return this;
    }

    @Override
    public JettyWebSocketBuilderImpl endpoint(ServerEndpointConfig serverConfig) {
      this.serverEndpointConfigs.add(serverConfig);
      return this;
    }

    @Override
    public ServletContextHandler build() {
      ServletContextHandler servletContextHandler = new ServletContextHandler(options);
      servletContextHandler.setContextPath(contextPath);

      //Add servlets
      servlets.forEach((k, v) -> {
        if (k != null && v != null) {
          servletContextHandler.addServlet(k, v);
        }
      });

      //Add filters with default dispatcher
      filters.forEach((key, value) -> {
        if (key != null && value != null) {
          servletContextHandler.addFilter(key, value, defaultDispatchers);
        }
      });

      //Add filters with explicit dispatchers
      filtersWithDispatchers.forEach((key, value) -> {
        if (key != null && value != null && (value.getLeft() != null && value.getRight() != null)) {
          servletContextHandler.addFilter(key, value.getLeft(), value.getRight());
        }
      });

      // Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
      servletContextHandler.addServlet(DefaultServlet.class, "/");

      //=================================================
      //Add jakarta websocket endpoints

      //Add annotated endpoints
      endpointClasses.forEach(endpoint ->
          JakartaWebSocketServletContainerInitializer.configure(servletContextHandler,
              (servletContext, serverContainer) -> serverContainer.addEndpoint(endpoint))
      );

      //Add programmatic endpoints
      serverEndpointConfigs.forEach(serverEndpointConfig ->
          JakartaWebSocketServletContainerInitializer.configure(servletContextHandler,
              (servletContext, serverContainer) -> serverContainer.addEndpoint(
                  serverEndpointConfig))
      );

      //=================================================
      //Add jetty api websocket endpoints
      jettyApiEndpointClasses.forEach((key, value) -> {
        if (key != null && value != null) {
          JettyWebSocketServletContainerInitializer.configure(servletContextHandler,
              (context, configurator) -> {
                configurator.addMapping(value, key);
              });
        }
      });
      return servletContextHandler;
    }
  }

}
