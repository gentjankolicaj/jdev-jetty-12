package io.jdev.jetty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.UriCompliance;

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

  //todo: add serializers for jackson
  private UriCompliance uriCompliance = UriCompliance.DEFAULT;
  private HttpCompliance httpCompliance = HttpCompliance.RFC7230;
  private CookieCompliance requestCookieCompliance = CookieCompliance.RFC6265;
  private CookieCompliance responseCookieCompliance = CookieCompliance.RFC6265;


}
