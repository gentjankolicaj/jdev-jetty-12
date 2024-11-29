package io.jdev.jetty;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 5:42 PM
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ConnectorProperties {

  protected String name;
  protected String host;
  protected int port;
  protected TimeoutProperties idleTimeout;
  protected Optional<HttpProperties> http = Optional.empty();

}
