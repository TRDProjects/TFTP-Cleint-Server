package host;

import java.net.DatagramPacket;

import host.ErrorSimulator.PacketType;

public class ErrorToSimulate {
	
	public enum ErrorToSimulateType {
		NO_ERROR(0, "No error"),
		INVALID_RQ_OPCODE(1, "Invalid request packet TFTP opcode"),
		EMPTY_FILENAME(2, "Empty filename"),
		EMPTY_MODE(3, "Empty mode"),
		INVALID_MODE(4, "Invalid mode"),
		INVALID_ACK_OPCODE(5, "Invalid ACK packet TFTP opcode"),
		INVALID_ACK_BLOCK_NUMBER(6, "Invalid ACK packet block number"),
		INVALID_DATA_OPCODE(7, "Invalid DATA packet TFTP opcode"),
		INVALID_DATA_BLOCK_NUMBER(8, "Invalid DATA packet block number"),
		LARGE_DATA_PACKET(9, "Large DATA packet (larger than 516 bytes)"),
		
		LOSE_PACKET(10, "Lose a packet"),
		DELAY_PACKET(11, "Delay a packet"),
		DUPLICATE_PACKET(12, "Duplicate a packet");
		
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
	private boolean wasExecuted;
	
	private int targetPacketNumber;
	private byte[] opcode;
	private byte[] blockNumber;
	private String fileName;
	private String mode;
	private PacketType packetType;
	private int delayTime;
	private DatagramPacket packet;
	private long lastTime;
	private boolean hasDelayedPacket;
	
	public ErrorToSimulate(int errorNumber) {
		for (ErrorToSimulateType eType: ErrorToSimulateType.values()) {
			if (eType.getErrorNumber() == errorNumber) {
				this.type = eType;
			}
		}
		
		this.delayTime = 0;
		this.lastTime = 0;
		this.hasDelayedPacket = false;
		this.targetPacketNumber = 0;
		this.wasExecuted = false;
	}

	
	
	public ErrorToSimulateType getType() {
		return type;
	}
	
	public int getTargetPacketNumber() {
		return targetPacketNumber;
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
	public PacketType getPacketType() {
		return packetType;
	}
	public boolean wasExecuted() {
		return wasExecuted;
	}
	public int getDelayTime() {
		return delayTime;
	}
	public long getLastTime() {
		return lastTime;
	}
	public boolean hasDelayedPacket() {
		return hasDelayedPacket;
	}
	
	public void setLastTime(long l) {
		this.lastTime = l;
	}
	public DatagramPacket getPacket() {
		return packet;
	}
	
	public void setPacket(DatagramPacket packet) {
		this.packet = packet;
	}
	public void setHasDelayedPacket(boolean val) {
		this.hasDelayedPacket = val;;
	}
	
	public void setTargetPacketNumber(int num) {
		this.targetPacketNumber = num;
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
	public void setWasExecuted(boolean value) {
		this.wasExecuted = value;
	}
	public void setPacketType(ErrorSimulator.PacketType type) {
		this.packetType = type;
	}
	public void setDelayTime(int delayInMs) {
		this.delayTime = delayInMs;
	}
	
}
