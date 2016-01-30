package host;

import host.ErrorSimulator.PacketAction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ErrorSimulatorRequest implements Runnable {
	
	private DatagramPacket sendPacketClient, receivePacketClient, sendPacketServer, receivePacketServer;
	private DatagramSocket sendReceiveSocket;
	
	private DatagramPacket requestPacket;
	
	public ErrorSimulatorRequest(DatagramPacket requestPacket) {
		this.requestPacket = requestPacket;
		
	    try {
	    	sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	}
	
	public void printPacketInfo(DatagramPacket packet, ErrorSimulator.PacketAction action) {
		System.out.println("\n");
		System.out.println("ErrorSimulator (" + Thread.currentThread() + "): " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + "packet:");
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "To " : "From ") + "host: " + packet.getAddress());
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "Destination host " : "Host ") + "port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("   " + "Length: " + len);
		System.out.println("   " + "Containing: ");
		String dataString = new String(packet.getData(),0,len);
		System.out.println("       - String: " + dataString);
		System.out.println("       - Bytes: " + Arrays.toString(dataString.getBytes()));
	}
	
	

	@Override
	public void run() {

	    // Process the received datagram.
	    printPacketInfo(requestPacket, PacketAction.RECEIVE);
	    
		
		// Construct a datagram packet to send to the Server
		// This assumes that the Server is running on localhost
	    try {
	    	sendPacketServer = new DatagramPacket(requestPacket.getData(), requestPacket.getLength(),
		    		InetAddress.getLocalHost(), 69);
	    } catch (UnknownHostException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }
	    
	    // Process the packet to send
	    printPacketInfo(sendPacketServer, PacketAction.SEND);
	        
	    // Send the datagram packet to the server via the send/receive socket. 
	    try {
	       sendReceiveSocket.send(sendPacketServer);
	    } catch (IOException e) {
	       e.printStackTrace();
	       System.exit(1);
	    }

	    System.out.println("Error Simulator: packet sent");
	    
	    while (true) {
		    receiveFromServerAndSendToClient();
		    receiveFromClientAndSendToServer();
	    }
		
	}
	
	public void receiveFromClientAndSendToServer() {
	    // Construct a DatagramPacket for receiving packets
	    byte dataFromClient[] = new byte[516];
	    receivePacketClient = new DatagramPacket(dataFromClient, dataFromClient.length);
	    System.out.println("Error Simulator: waiting for Packet.\n");

	    // Block until a datagram packet is received from the receive socket
	    try {        
	        System.out.println("Waiting...");
	        sendReceiveSocket.receive(receivePacketClient);
	    } catch (IOException e) {
	        System.out.print("IO Exception: likely:");
	        System.out.println("Receive Socket Timed Out.\n" + e);
	        e.printStackTrace();
	        System.exit(1);
	    }
	    
	    
		// Construct a datagram packet to send to the Server
		// This assumes that the Server is running on localhost
	    sendPacketServer = new DatagramPacket(requestPacket.getData(), requestPacket.getLength(),
	    			receivePacketServer.getAddress(), receivePacketServer.getPort());
	    
	    // Process the packet to send
	    printPacketInfo(sendPacketServer, PacketAction.SEND);
	        
	    // Send the datagram packet to the server via the send/receive socket. 
	    try {
	       sendReceiveSocket.send(sendPacketServer);
	    } catch (IOException e) {
	       e.printStackTrace();
	       System.exit(1);
	    }

	    System.out.println("Error Simulator: packet sent");
	   
	}
	
	private void receiveFromServerAndSendToClient() {
	    // Construct a DatagramPacket for receiving packets
	    byte dataFromServer[] = new byte[516];
	    receivePacketServer = new DatagramPacket(dataFromServer, dataFromServer.length);
	
	    try {
	        // Block until a datagram is received via sendReceiveSocket.  
	        sendReceiveSocket.receive(receivePacketServer);
	    } catch(IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }
	
	    // Process the received datagram.
	    printPacketInfo(receivePacketServer, PacketAction.RECEIVE);
	    
		
		// Construct a datagram packet to send to the Client
    	String dataReceivedString = new String(receivePacketServer.getData(), 0, receivePacketServer.getLength());
    	
	    sendPacketClient = new DatagramPacket(dataReceivedString.getBytes(), dataReceivedString.getBytes().length,
	    		requestPacket.getAddress(), requestPacket.getPort());
	    
	    // Process the packet to send
	    printPacketInfo(sendPacketClient, PacketAction.SEND);
	    
		
	      
      // Send the datagram packet to the Client
      try {
    	  sendReceiveSocket.send(sendPacketClient);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
	      
	    
	   
	    System.out.println("Error Simulator: packet sent");
	    
		
	}

}

