package host;

public class ErrorToSimulate {
	
	public enum ErrorToSimulateType {
		NO_ERROR(0, "No error"),
		INVALID_RQ_OPCODE(1, "Invalid request packet TFTP opcode"),
		EMPTY_FILENAME(2, "Empty filename"),
		EMPTY_MODE(3, "Empty mode"),
		INVALID_MODE(4, "Invalid mode"),
		DUPLICATE_WRQ_PACKET(5, "Duplicate WRQ packet (write request packet)"),
		INVALID_ACK_OPCODE(6, "Invalid ACK packet TFTP opcode"),
		INVALID_ACK_BLOCK_NUMBER(7, "Invalid ACK packet block number"),
		INVALID_DATA_OPCODE(8, "Invalid DATA packet TFTP opcode"),
		INVALID_DATA_BLOCK_NUMBER(9, "Invalid DATA packet block number"),
		LARGE_DATA_PACKET(10, "Large DATA packet (larger than 516 bytes)");
		
		private int errorNumber;
		private String errorString;
		
		private ErrorToSimulateType(int errorNumber, String errorString) {
			this.errorNumber = errorNumber;
			this.errorString = errorString;
		}
		
		public int getErrorNumber() {
			return errorNumber;
		}
		
		public String getErrorString() {
			return errorString;
		}
	}
	
	private ErrorToSimulateType type;
	
	public ErrorToSimulate(int errorNumber) {
		for (ErrorToSimulateType eType: ErrorToSimulateType.values()) {
			if (eType.getErrorNumber() == errorNumber) {
				this.type = eType;
			}
		}
	}
	
	
	private byte[] opcode;
	private byte[] blockNumber;
	private String fileName;
	private String mode;
	
	
	public ErrorToSimulateType getType() {
		return type;
	}
	
	public byte[] getOpcode() {
		return opcode;
	}
	public byte[] getBlockNumber() {
		return blockNumber;
	}
	public String getFileName() {
		return fileName;
	}
	public String getMode() {
		return mode;
	}
	
	public void setOpcode(byte[] code) {
		this.opcode = code;
	}
	public void setBlockNumber(byte[] blockNum) {
		this.blockNumber = blockNum;
	}
	public void setFileName(String file) {
		this.fileName = file;
	}
	public void setMode(String m) {
		this.mode = m;
	}
}
