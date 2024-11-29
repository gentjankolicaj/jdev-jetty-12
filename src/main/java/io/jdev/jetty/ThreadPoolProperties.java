package io.jdev.jetty;

public record ThreadPoolProperties(String poolName, boolean daemonThreads,
                                   int minThreads, int maxThreads, int reservedThreads,
                                   int idleTimeout, int stopTimeout) {

}