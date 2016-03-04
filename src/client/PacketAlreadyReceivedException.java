package client;

public class PacketAlreadyReceivedException extends Exception {
	private static final long serialVersionUID = 5L;
	
	public PacketAlreadyReceivedException(String message) {
        super(message);
    }
}
