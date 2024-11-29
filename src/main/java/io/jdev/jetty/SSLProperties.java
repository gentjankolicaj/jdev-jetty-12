package io.jdev.jetty;

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
public class SSLProperties {

  protected String keyStoreFile;
  protected String keyStorePassword;
  protected String keyPassword;

}
