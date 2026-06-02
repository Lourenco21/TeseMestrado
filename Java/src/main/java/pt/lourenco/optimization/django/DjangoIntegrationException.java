package pt.lourenco.optimization.django;

public class DjangoIntegrationException extends RuntimeException {

    public DjangoIntegrationException(String message) {
        super(message);
    }

    public DjangoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
