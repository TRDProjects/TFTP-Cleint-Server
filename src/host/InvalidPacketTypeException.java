package host;

public class InvalidPacketTypeException extends Exception {
	
	private static final long serialVersionUID = 4L;
	
	public InvalidPacketTypeException(String message) {
        super(message);
    }

}
