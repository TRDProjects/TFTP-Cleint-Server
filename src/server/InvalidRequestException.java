package server;

public class InvalidRequestException extends Exception {
	
	private static final long serialVersionUID = 2L;
	
	public InvalidRequestException(String message) {
        super(message);
    }

}
