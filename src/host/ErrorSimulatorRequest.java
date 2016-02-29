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
  
  private ErrorManager errorManager;
  private int currentAckPacketNumber, currentDataPacketNumber;
  
  public ErrorSimulatorRequest(DatagramPacket requestPacket) {
    this.requestPacket = requestPacket;
    this.errorManager = new ErrorManager();
    this.currentAckPacketNumber = 0;
    this.currentDataPacketNumber = 0;
    
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
  
  
  private ErrorSimulator.PacketType getPacketType(DatagramPacket packet) throws InvalidPacketTypeException {
	  byte[] data = packet.getData();
	  
	  if (data[0] == 0) {
		  
		  if (data[1] == ErrorSimulator.PacketType.READ.getOpcode()) {
			  return ErrorSimulator.PacketType.READ;
			  
		  } else if (data[1] == ErrorSimulator.PacketType.WRITE.getOpcode()) {
			  return ErrorSimulator.PacketType.WRITE;
			  
		  } else if (data[1] == ErrorSimulator.PacketType.ACK.getOpcode()) {
			  return ErrorSimulator.PacketType.ACK;
			  
		  } else if (data[1] == ErrorSimulator.PacketType.DATA.getOpcode()) {
			  return ErrorSimulator.PacketType.DATA;
			  
		  } else if (data[1] == ErrorSimulator.PacketType.ERROR.getOpcode()) {
			  return ErrorSimulator.PacketType.ERROR;
			  
		  } else {
			  throw new InvalidPacketTypeException("Invalid packet type: second byte is " + data[1]);  
		  }
		  
	  } else {
		 throw new InvalidPacketTypeException("Invalid packet type: first byte is not a 0 byte");
	  }
  }
  
  
  public void sendPacket(DatagramSocket socket, DatagramPacket packet) {
      printPacketInfo(packet, PacketAction.SEND);

      try {
    	  socket.send(packet);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      System.out.println("Error Simulator: packet sent");
  }
  
  public DatagramPacket receivePacket(DatagramSocket socket, int bufferLength) {
	  // Construct a DatagramPacket for receiving packets
      byte dataBuffer[] = new byte[bufferLength];
      DatagramPacket receivedPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
      System.out.println("Error Simulator: Waiting for Packet.\n");
      
      // Block until a datagram packet is received from the socket
      try {        
          System.out.println("Waiting...");
          socket.receive(receivedPacket);
      } catch (IOException e) {
          System.out.print("IO Exception: likely:");
          System.out.println("Receive Socket Timed Out.\n" + e);
          e.printStackTrace();
          System.exit(1);
      }
      
      // Process the packet received
      printPacketInfo(receivedPacket, PacketAction.RECEIVE);
      
      return receivedPacket;
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
      
      // Show the menu UI for error simulation
      errorManager.userMenu();
      
      // TODO modify packet if required
      sendPacketServer = errorManager.simulateError(sendPacketServer, 0);
      
      // Send the packet
      sendPacket(sendReceiveSocket, sendPacketServer);
      
      
      while (true) {
        receiveFromServerAndSendToClient();
        receiveFromClientAndSendToServer();
      }
    
  }
  
  public void receiveFromClientAndSendToServer() {
	  // Receive the packet from the client
	  receivePacketClient = receivePacket(sendReceiveSocket, 517);
       
      // Construct a datagram packet to send to the Server
      // This assumes that the Server is running on localhost
      sendPacketServer = new DatagramPacket(receivePacketClient.getData(), receivePacketClient.getLength(),
            receivePacketServer.getAddress(), receivePacketServer.getPort());
      
      // Check the type of the packet received (i.e. ACK or DATA)
      try {
    	  ErrorSimulator.PacketType typeOfPacketReceived = getPacketType(sendPacketServer);
    	  
    	  if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.ACK)) {
    		  currentAckPacketNumber++;
    		  sendPacketServer = errorManager.simulateError(sendPacketServer, currentAckPacketNumber);
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  currentDataPacketNumber++;
    		  sendPacketServer = errorManager.simulateError(sendPacketServer, currentDataPacketNumber);
    	  }
      } catch (InvalidPacketTypeException e) {
    	 // Do nothing
      }
      
      
      // Send the packet
      sendPacket(sendReceiveSocket, sendPacketServer);
      
     
  }
  
  private void receiveFromServerAndSendToClient() {
	  // Receive the packet from the server
	  receivePacketServer = receivePacket(sendReceiveSocket, 517);
      
    
      // Construct a datagram packet to send to the Client
      String dataReceivedString = new String(receivePacketServer.getData(), 0, receivePacketServer.getLength());
      
      sendPacketClient = new DatagramPacket(dataReceivedString.getBytes(), dataReceivedString.getBytes().length,
          requestPacket.getAddress(), requestPacket.getPort());
      
      
      // Check the type of the packet received (i.e. ACK or DATA)
      try {
    	  ErrorSimulator.PacketType typeOfPacketReceived = getPacketType(sendPacketClient);
    	  
    	  if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.ACK)) {
    		  currentAckPacketNumber++;
    		  sendPacketClient = errorManager.simulateError(sendPacketClient, currentAckPacketNumber);
    		  
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  currentDataPacketNumber++;
    		  sendPacketClient = errorManager.simulateError(sendPacketClient, currentDataPacketNumber);
    	  }
      } catch (InvalidPacketTypeException e) {
    	  //System.out.println("InvalidPacketTypeException thrown: received packet with invalid opcode from server or client: " + e.getMessage());
      }
      
      // Send the packet to the client
      sendPacket(sendReceiveSocket, sendPacketClient);
      
    
  }

}
