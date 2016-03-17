package client;

public class AccessViolationException extends Exception {
	
	private static final long serialVersionUID = 6L;
	
	public AccessViolationException(String message) {
        super(message);
    }

}