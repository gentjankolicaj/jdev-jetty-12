package io.jdev.jetty;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jetty.http.HttpVersion;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 8:49 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class HttpProperties {

  protected Optional<SSLProperties> ssl = Optional.empty();
  protected Optional<String> secureScheme = Optional.empty();
  protected Optional<Integer> securePort = Optional.empty();
  protected Optional<HttpVersion> version = Optional.empty();
  protected Optional<Integer> responseHeaderSize = Optional.empty();
  protected Optional<Integer> requestHeaderSize = Optional.empty();
  protected Optional<Integer> outputBufferSize = Optional.empty();
  protected Optional<Boolean> sendServerVersion = Optional.empty();
  protected Optional<Boolean> sendDateHeader = Optional.empty();


}
