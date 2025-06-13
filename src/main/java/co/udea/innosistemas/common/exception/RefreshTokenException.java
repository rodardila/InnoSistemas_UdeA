package co.udea.innosistemas.common.exception;

public class RefreshTokenException extends RuntimeException {
  public RefreshTokenException(String message) {
    super(message);
  }

  public RefreshTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
