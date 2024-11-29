package io.jdev.jetty;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jdev.jackson.YamlConfigurations;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.jupiter.api.Test;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 10:47 PM
 */
@Slf4j
class ContextHandlerTest {


  @Test
  void noWebApp() throws Exception {
    JettyProperties jettyProperties = YamlConfigurations.load(JettyProperties.class,
        "/jetty_http_versions.yaml");

    assertThatThrownBy(() -> new JettyServer(jettyProperties.getJettyServer(),
        (ContextHandler[]) null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ContextHandler's can't be empty !");

    assertThatThrownBy(() -> new JettyServer(jettyProperties.getJettyServer(),
        new ContextHandler[0])).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ContextHandler's can't be empty !");
  }


}