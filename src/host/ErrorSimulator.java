package host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;


public class ErrorSimulator {
	
	
	public enum PacketAction {
		SEND, RECEIVE
	}
	
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
	
	private InetAddress serverAddress;
	private int serverPort;
	
	private DatagramPacket receivePacketClient;
	private DatagramSocket receiveSocket;
	
	
	public ErrorSimulator(String serverAddressString, int serverPort) {
	    try {
	    	receiveSocket = new DatagramSocket(68);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	    
	    try {
	    	serverAddress = InetAddress.getByName(serverAddressString);
	    } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
	    }
	    
	    this.serverPort = serverPort;
	}
	
	
	public void printPacketInfo(DatagramPacket packet, PacketAction action) {
		System.out.println("\n");
		System.out.println("Error Simulator: " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + "packet:");
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "To " : "From ") + "host: " + packet.getAddress());
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "Destination host " : "Host ") + "port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("   " + "Length: " + len);
		System.out.println("   " + "Containing: ");
		String dataString = new String(packet.getData(),0,len);
		System.out.println("       - String: " + dataString);
		System.out.println("       - Bytes: " + Arrays.toString(dataString.getBytes()));
	}
	
	
	public void receiveFromClientAndSendToServer() {
	    // Construct a DatagramPacket for receiving packets
	    byte dataFromClient[] = new byte[517];
	    receivePacketClient = new DatagramPacket(dataFromClient, dataFromClient.length);
	    System.out.println("Error Simulator: waiting for packet.\n");

	    // Block until a datagram packet is received from the receive socket
	    try {        
	        System.out.println("Waiting...");
	        receiveSocket.receive(receivePacketClient);
	    } catch (IOException e) {
	        System.out.print("IO Exception: likely:");
	        System.out.println("Receive Socket Timed Out.\n" + e);
	        e.printStackTrace();
	        System.exit(1);
	    }
	    
	    System.out.println("\n** Error Sim (port 68): received packet from " + receivePacketClient.getAddress());
	    System.out.println("   Starting new thread...\n");
	    
	    Thread requestThread = new Thread(new ErrorSimulatorRequest(receivePacketClient), 
	    		"ErrorSim Request Thread (For Host " + receivePacketClient.getAddress() + ")");
	    
	    requestThread.start();
	   
	}
	
	
	public static void main(String args[]) {
		ErrorSimulator newErrorSim = new ErrorSimulator("localhost", 69);
		
		while (true) {
			newErrorSim.receiveFromClientAndSendToServer();
		}
		
	}

}
