public class InvalidTicketDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidTicketDataException(String message) {
        super(message);
    }

    public InvalidTicketDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTicketDataException(int lineNumber, String message) {
        super("Invalid ticket data at line " + lineNumber + ": " + message);
    }

    public InvalidTicketDataException(int lineNumber, String message, Throwable cause) {
        super("Invalid ticket data at line " + lineNumber + ": " + message, cause);
    }
}

