package client;

public class UnknownTransferIdException extends Exception {
	
	private static final long serialVersionUID = 3L;
	
	public UnknownTransferIdException(String message) {
        super(message);
    }

}
