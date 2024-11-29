package io.jdev.jetty;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;


/**
 * @author gentjan kolicaj
 * @Date: 11/22/24 7:40 PM
 */
public abstract class SSLTest {

  public static final TrustManager DUMMY_TRUST_MANAGER = new X509ExtendedTrustManager() {
    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket)
        throws CertificateException {
      // No validation for client certificates
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket)
        throws CertificateException {
      // No validation for server certificates
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s,
        SSLEngine sslEngine) throws CertificateException {
      // No validation for client certificates
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s,
        SSLEngine sslEngine) throws CertificateException {
      // No validation for server certificates
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
        throws CertificateException {
      // No validation for client certificates
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
        throws CertificateException {
      // No validation for server certificates
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  };


  public static SSLContext createSSLContext(TrustManager... trustManagers)
      throws GeneralSecurityException {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagers, new SecureRandom());
    return sslContext;
  }


  public static TrustManager[] createTrustManagers(SSLProperties sslProperties)
      throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    URL url = Thread.currentThread().getContextClassLoader()
        .getResource(sslProperties.getKeyStoreFile());

    try (FileInputStream fis = new FileInputStream(url.getFile())) {
      keyStore.load(fis, sslProperties.getKeyStorePassword().toCharArray());
    }

    // Create TrustManagerFactory
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    return trustManagerFactory.getTrustManagers();
  }

  /**
   * Note: SSLContext fails when keystore certificate is not valid
   *
   * @param sslProperties
   * @return
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public static SSLContext createSSLContext(SSLProperties sslProperties)
      throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    URL url = Thread.currentThread().getContextClassLoader()
        .getResource(sslProperties.getKeyStoreFile());

    try (FileInputStream fis = new FileInputStream(url.getFile())) {
      keyStore.load(fis, sslProperties.getKeyStorePassword().toCharArray());
    }

    // Create TrustManagerFactory
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    // Create SSLContext
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    return sslContext;
  }

  /**
   * Note: SSLContext fails when keystore certificate is not valid.
   *
   * @param keystoreResource
   * @param keystorePassword
   * @return
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public static SSLContext createSSLContext(String keystoreResource, String keystorePassword)
      throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    URL url = Thread.currentThread().getContextClassLoader().getResource(keystoreResource);

    try (FileInputStream fis = new FileInputStream(url.getFile())) {
      keyStore.load(fis, keystorePassword.toCharArray());
    }

    // Create TrustManagerFactory
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    // Create SSLContext
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    return sslContext;
  }

}
