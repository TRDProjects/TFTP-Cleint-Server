package client;

public class FileAlreadyExistsException extends Exception {
	
	private static final long serialVersionUID = 2L;
	
	public FileAlreadyExistsException(String message) {
        super(message);
    }

}
