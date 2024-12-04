package io.jdev.jetty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gentjan kolicaj
 * @Date: 11/21/24 8:49 PM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SSLProperties {

  private String keyStorePath;
  private String keyStorePassword;
  private String keyManagerPassword;
  private Optional<Boolean> needClientAuth = Optional.of(true);
  private Optional<Boolean> wantClientAuth = Optional.of(true);
  private Optional<String> certAlias = Optional.empty();
  private Optional<String> jceProvider = Optional.empty();
  private Optional<Boolean> validateCerts = Optional.of(true);
  private Optional<Boolean> validatePeers = Optional.of(true);
  private List<String> includedProtocols;
  private List<String> excludedProtocols;
  private List<String> includedCipherSuites;
  private List<String> excludedCipherSuites;

}
