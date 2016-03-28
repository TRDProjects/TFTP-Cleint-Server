package server;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import util.Keyboard;

public class Server implements Runnable {
	
	public static final int PORT = 69;
	public static final String FILE_PATH = "src/server/files/";
	public static final boolean ALLOW_FILE_OVERWRITING = true;
	
	public static enum PacketType {
		READ((byte) 1), 
		WRITE((byte) 2), 
		DATA((byte) 3), 
		ACK((byte) 4), 
		ERROR((byte) 5);
		
		private byte opcode;
		
		private PacketType(byte opcode) {
			this.opcode = opcode;
		}
		
		public byte getOpcode() {
			return opcode;
		}
	}
	
	public enum PacketAction {
		SEND, RECEIVE
	}
	
	
	public enum ErrorType {
		FILE_NOT_FOUND((byte) 1),
		ACCESS_VIOLATION((byte) 2),
		DISK_FULL((byte) 3),
		ILLEGAL_TFTP_OPERATION((byte) 4),
		UNKNOWN_TRANSFER_ID((byte) 5),
		FILE_ALREADY_EXISTS((byte) 6);
		
		private byte errorCode;
		
		private ErrorType(byte errorCode) {
			this.errorCode = errorCode;
		}
		
		public byte getErrorCode() {
			return errorCode;
		}
	}
	
	
	public enum Mode {
		NETASCII,
		OCTET;
	}
	
	volatile boolean running = true;
	
	
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;
	
	public Server() {
		try {
			receiveSocket = new DatagramSocket(PORT);
		} catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
		}
	}
	
	public void printPacketInfo(DatagramPacket packet, PacketAction action) {
		System.out.println("\n");
		System.out.println("Server: " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + "packet:");
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "To " : "From ") + "host: " + packet.getAddress());
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "Destination host " : "Host ") + "port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("   " + "Length: " + len);
		System.out.println("   " + "Containing: ");
		String dataString = new String(packet.getData(),0,len);
		System.out.println("       - String: " + dataString);
		System.out.println("       - Bytes: " + Arrays.toString(dataString.getBytes()));
	}
	
	
	
	public void receiveAndProcessRequest() {
        byte dataFromHost[] = new byte[517];
		receivePacket = new DatagramPacket(dataFromHost, dataFromHost.length);
	    System.out.println("Server: waiting for Packet.\n");

	    // Block until a datagram packet is received from the receive socket
	    try {        
	        System.out.println("Waiting...");
	        receiveSocket.receive(receivePacket);
	    } catch (SocketException e) {
	    	if (e.getMessage().matches("Socket closed")) {
	    		System.out.println("\n>> Shutting down Server...\n");
	    		return;
	    	}
	    } catch (IOException e) {
	        System.out.print("IO Exception: likely:");
	        System.out.println("Receive Socket Timed Out.\n" + e);
	        e.printStackTrace();
	        System.exit(1);
	        
	    }
	    
	    
	    Thread requestThread = new Thread(new Request(receivePacket), 
	    		"Server Request Thread (For Host " + receivePacket.getAddress() + ")");
	    
	    System.out.println("\n\n >>> Server: starting new request thread with ID " + requestThread.getId() + "\n\n");
	    
	    requestThread.start();
		
	}

    
	@Override
	public void run() {
		while(true) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			receiveAndProcessRequest();
		}
	}
	
	
	
    public static void main(String args[]) {
    	Server newServer = new Server();
    	
        Thread serverThread = new Thread(newServer, "Server Main Thread");
        
        serverThread.start();
        
        System.out.println(">> Server is running...To exit, enter 'q'...\n");
    	
    	while (true) {
        	if (Keyboard.getCharacter() == 'q') {
        		break;
        	}
    	}
    	
    	newServer.running = false;
    	newServer.receiveSocket.close();
    	serverThread.interrupt();
    	
    	System.out.println("Exiting...");
    	System.exit(1);
    	
    	
    }

}
