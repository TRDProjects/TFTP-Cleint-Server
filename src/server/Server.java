package server;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class Server {
	
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
	
	
	DatagramPacket receivePacket;
	DatagramSocket receiveSocket;
	
	public Server() {
		try {
			receiveSocket = new DatagramSocket(69);
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
        byte dataFromHost[] = new byte[100];
		receivePacket = new DatagramPacket(dataFromHost, dataFromHost.length);
	    System.out.println("Server: waiting for Packet.\n");

	    // Block until a datagram packet is received from the receive socket
	    try {        
	        System.out.println("Waiting...");
	        receiveSocket.receive(receivePacket);
	    } catch (IOException e) {
	        System.out.print("IO Exception: likely:");
	        System.out.println("Receive Socket Timed Out.\n" + e);
	        e.printStackTrace();
	        System.exit(1);
	    }
	    
	    
	    Thread requestThread = new Thread(new Request(receivePacket), 
	    		"Server Request Thread (For Host " + receivePacket.getAddress() + ")");
	    
	    requestThread.start();
		
	}
	
	
    public static void main(String args[]) {
    	Server newServer = new Server();
    	
    	while (true) {
        	newServer.receiveAndProcessRequest();
    	}
    	
    }

}
