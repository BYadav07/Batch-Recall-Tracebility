package trace;

/** Thrown when an entry breaks a business rule; the message is shown to the user as is. */
public class BusinessRuleException extends Exception {
    public BusinessRuleException(String message) {
        super(message);
    }
}
