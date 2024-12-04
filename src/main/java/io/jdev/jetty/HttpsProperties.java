package io.jdev.jetty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gentjan kolicaj
 * @date: 11/21/24 8:49 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@JsonTypeName("https")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpsProperties extends HttpProperties {

  protected Optional<SSLProperties> ssl = Optional.empty();
  protected Optional<String> secureScheme = Optional.empty();
  protected Optional<Integer> securePort = Optional.empty();


}
