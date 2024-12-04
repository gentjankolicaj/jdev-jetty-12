package io.jdev.jetty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jetty.http.HttpVersion;

/**
 * @author gentjan kolicaj
 * @date: 11/21/24 8:49 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@JsonTypeName("http")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpProperties extends HttpConfigProperties {

  private Optional<HttpVersion> version = Optional.empty();
  private Optional<Integer> responseHeaderSize = Optional.empty();
  private Optional<Integer> requestHeaderSize = Optional.empty();
  private Optional<Integer> outputBufferSize = Optional.empty();
  private Optional<Boolean> sendServerVersion = Optional.empty();
  private Optional<Boolean> sendDateHeader = Optional.empty();


}
