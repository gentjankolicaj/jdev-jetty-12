package io.jdev.jetty;

import java.util.concurrent.TimeUnit;

public record TimeoutProperties(int duration, TimeUnit timeUnit) {

}