package client;

public class DiskFullException extends Exception {
	
	private static final long serialVersionUID = 2L;
	
	public DiskFullException(String message) {
        super(message);
    }

}
