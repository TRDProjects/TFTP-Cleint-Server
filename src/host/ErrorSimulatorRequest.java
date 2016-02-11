package host;

import host.ErrorSimulator.PacketAction;
import util.Keyboard;

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
  
  public void errorSim(int caller, DatagramPacket data){
	  
	  // caller differentiates who's calling errorSim
	  // 1 means its the client sending to server
	  // 2 means its the server sending to client
	  
	  System.out.println("enter the error number of the error would you like to simulate /n");
	  
	  // No error, error sim does nothing
	  System.out.println("0 : no error");
	  
	  
	  /** ERROR CODE 4 **/	  
	  System.out.println("error # : who will throw the error : cause of error");
	  // Invalid Request Packet errors
	  System.out.println("1  : server : Invalid Request Packet TFTP opcode RRQ");
	  System.out.println("2  : server : Invalid Request Packet TFTP opcode WRQ");
	  System.out.println("3  : server : Invalid Request Packet format");
	  System.out.println("4  : server : Invalid Request Packet filename");
	  System.out.println("5  : server : invalid Request Packet mode");
	  
	  //Invalid ACK packet
	  System.out.println("6  : server : invalid ACK packet length"); //less than or more than 4 bytes
	  System.out.println("7  : server : invalid ACK packet TFTP opcode"); //anything other than 4
	  System.out.println("8  : server : invalid ACK packet block number");
	  System.out.println("9  : client : invalid ACK packet length"); //less than or more than 4 bytes
	  System.out.println("10 : client : invalid ACK packet TFTP opcode"); //anything other than 4
	  System.out.println("11 : client : invalid ACK packet block number");
	  
	  //Invalid DATA packet
	  System.out.println("12 : server : invalid DATA packet length"); //longer than 516 bytes
	  System.out.println("13 : server : invalid DATA packet block number");
	  System.out.println("14 : client : invalid DATA packet length"); //longer than 516 bytes
	  System.out.println("15 : clientgr : invalid DATA packet block number");
	  
	  /** ERROR CODE 5 **/	
	  System.out.println("16 : server : ACK packet from invalid client");
	  System.out.println("17 : cleint : ACK packet from invalid server");
	  System.out.println("18 : server : DATA packet from invalid client");
	  System.out.println("19 : client : DATA packet from invalid server");
	  
	  
	  //get the requested error from the user
	  int error =  Keyboard.getInteger();

	  
	  //make sure it's a valid entry
	  while(error > 19 || error < 0){
		  System.out.println("try again");
		  error = Keyboard.getInteger();
	  }
	  
	  /** ERROR HANDLING **/
	  // do nothing : client to server
	  if(error == 0){
	      try {
	          sendReceiveSocket.send(data);
	        } catch (IOException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }	 
	      }
	  
	  //FROM CLIENT TO SERVER - 1  : server : Invalid Request Packet TFTP opcode RRQ
	  if(error == 1 && caller  == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  this should never happen : servers don't send clients WRQ's
	  if(error == 1 && caller  == 2){
		  // up to you what you want to do here, maybe just redirect to condition 0? (do nothing)
		  // theres more stuff like this below.
	  }
	  
	  //FROM CLIENT TO SERVER - 2  : server : Invalid Request Packet TFTP opcode WRQ
	  if(error == 2 && caller  == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  this should never happen : servers don't send clients WRQ's
	  if(error == 2 && caller  == 2){
		  
	  }
	  
	  //FROM CLIENT TO SERVER -  3  : server : Invalid Request Packet format
	  if(error == 3 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  3  : server : Invalid Request Packet format
	  if(error == 3 && caller == 2){
		  
	  }
	  
	  //FROM CLIENT TO SERVER -  4  : server : Invalid Request Packet filename
	  if(error == 4 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  4  : server : Invalid Request Packet filename
	  if(error == 4 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  5  : server : invalid Request Packet mode
	  if(error == 5 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  5  : server : invalid Request Packet mode
	  if(error == 5 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  6  : server : invalid ACK packet length
	  if(error == 6 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  6  : server : invalid ACK packet length
	  if(error == 6 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  7  : server : invalid ACK packet TFTP opcode
	  if(error == 7 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  7  : server : invalid ACK packet TFTP opcode
	  if(error == 7 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  8  : server : invalid ACK packet block number
	  if(error == 8 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  8  : server : invalid ACK packet block number
	  if(error == 8 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  9  : client : invalid ACK packet length
	  if(error == 9 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  9  : client : invalid ACK packet length
	  if(error == 9 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  10 : client : invalid ACK packet TFTP opcode
	  if(error == 10 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  10 : client : invalid ACK packet TFTP opcode
	  if(error == 10 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  11 : client : invalid ACK packet block number
	  if(error == 11 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  11 : client : invalid ACK packet block number
	  if(error == 11 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  12 : server : invalid DATA packet length
	  if(error == 12 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  12 : server : invalid DATA packet length
	  if(error == 12 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  13 : server : invalid DATA packet block number
	  if(error == 13 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  13 : server : invalid DATA packet block number
	  if(error == 13 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  14 : client : invalid DATA packet length
	  if(error == 14 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  14 : client : invalid DATA packet length
	  if(error == 14 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  15 : client : invalid DATA packet block number
	  if(error == 15 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  15 : client : invalid DATA packet block number
	  if(error == 15 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  16 : server : ACK packet from invalid client
	  if(error == 16 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  16 : server : ACK packet from invalid client
	  if(error == 16 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  17 : cleint : ACK packet from invalid server
	  if(error == 17 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  17 : cleint : ACK packet from invalid server
	  if(error == 17 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  18 : server : DATA packet from invalid client
	  if(error == 18 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  18 : server : DATA packet from invalid client
	  if(error == 18 && caller == 2){
		  
	  }
	  //FROM CLIENT TO SERVER -  19 : client : DATA packet from invalid server
	  if(error == 19 && caller == 1){
		  
	  }
	  //FROM SERVER TO CLIENT -  19 : client : DATA packet from invalid server
	  if(error == 19 && caller == 2){
		  
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
      sendPacketServer = new DatagramPacket(receivePacketClient.getData(), receivePacketClient.getLength(),
            receivePacketServer.getAddress(), receivePacketServer.getPort());
      
      // Process the packet to send
      printPacketInfo(sendPacketServer, PacketAction.SEND);
          
      // Send the datagram packet to the server via the send/receive socket. 
      //sends the datagram packet to the errorSim which will either alter it, or not, and send it on to the server
      try {
    	 errorSim(1, sendPacketServer);
      // sendReceiveSocket.send(sendPacketServer);
      } catch (Exception e) {
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
      //sends the datagram packet to the errorSim which will either alter it, or not, and send it on to the client
      try {
    	errorSim(2, sendPacketClient);
        //sendReceiveSocket.send(sendPacketClient);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
        
      
     
      System.out.println("Error Simulator: packet sent");
      
    
  }

}
