package io.jdev.jetty;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gentjan kolicaj
 * @Date: 11/15/24 10:17 PM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JettyServerProperties {

  protected boolean dumpAfterStart;
  protected boolean dumpBeforeStop;
  protected boolean stopAtShutdown;
  protected long stopTimeout;
  protected boolean gzipEnabled;
  protected Optional<Boolean> securedRedirect = Optional.empty();
  protected ThreadPoolProperties threadPool;
  protected List<ConnectorProperties> connectors;

}
