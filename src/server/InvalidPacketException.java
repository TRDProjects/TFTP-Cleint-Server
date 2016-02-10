package server;

public class InvalidPacketException extends Exception {
	
	private static final long serialVersionUID = 2L;
	
	public InvalidPacketException(String message) {
        super(message);
    }

}
