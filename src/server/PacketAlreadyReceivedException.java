package server;

public class PacketAlreadyReceivedException extends Exception {
	private static final long serialVersionUID = 4L;
	
	public PacketAlreadyReceivedException(String message) {
        super(message);
    }
}
