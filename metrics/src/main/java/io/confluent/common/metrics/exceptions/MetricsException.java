package io.confluent.common.metrics.exceptions;

public class MetricsException extends RuntimeException {

  private final static long serialVersionUID = 1L;

  public MetricsException(String message, Throwable cause) {
    super(message, cause);
  }

  public MetricsException(String message) {
    super(message);
  }

  public MetricsException(Throwable cause) {
    super(cause);
  }

  public MetricsException() {
    super();
  }

}
