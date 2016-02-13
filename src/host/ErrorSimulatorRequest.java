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
  
  
  private void modifyRequestPacketUi(DatagramPacket packet) {
	  System.out.println("\n------------- Request Packet Modification Menu ----------------");
	  System.out.println("Enter the error number of the error would you like to simulate: \n");
	  
	  System.out.println("0 : No error (i.e do not modify the packet)");
	  
	  System.out.println("1 : Invalid request packet TFTP opcode");
	  System.out.println("2 : Empty filename");
	  System.out.println("3 : Empty mode");
	  System.out.println("4 : Invalid mode");
	  System.out.println("5 : Duplicate WRQ packet (write request packet");
	  System.out.println("---------------------------------------------------------------\n");
	  
	  // Get the requested error from the user
	  int error =  Keyboard.getInteger();

	  
	  // Make sure it's a valid entry
	  while(error > 5  || error < 0){
		  System.out.println("try again");
		  error = Keyboard.getInteger();
	  }
	  
	  
	  if (error == 1) {
		  //Get input from the user of the 2 bytes they would like to change the original opcode to (i.e. 07 instead of 01 or 02)
		  //Then modify the packet
		  System.out.println("Enter desired opcode (2 digits).\n");
   	 
		  //Get requested new opcode from user
		  int opcode = Keyboard.getInteger();
   	 
		  while(opcode > 99 || opcode < 10 ){
    		 System.out.println("opcode must be 2 digits");
    		 opcode = Keyboard.getInteger();
		  }
   	 
		  changePacketOpcode(packet, opcode);	  
		  
	  } else if (error == 2) {
		  //remove file name from the packet
		  
		  byte[] pData = new byte[packet.getLength()];
		  System.arraycopy(packet.getData(), packet.getOffset(), pData, 0, packet.getLength());
		  
		  int fileNameSize = 0;
		  
		  for (int i = 2; pData[i] != 0; i++){
			  fileNameSize++;
		  }
		  
		  byte[] newData = new byte[pData.length - fileNameSize];
		  newData[0] = pData[0];
		  newData[1] = pData[1];
		  
		  System.arraycopy(pData, fileNameSize + 2, newData, 2, pData.length - fileNameSize - 2);
		  
		  packet.setData(newData);
		  
		
	  } else if (error == 3) {
		//remove mode from the packet
		  
		  changePacketMode(packet, "");
		  

	  } else if (error == 4) {
		//get input from the user of the mode they would like to use
		//then call method that changes the mode to what the user passed in
		  
		  System.out.println("Enter desired mode.");
		  
		  //Get new mode from user
		  String mode = Keyboard.getString();
		  
		  changePacketMode(packet, mode);
		  
		  
	  } else if (error == 5) {
		  try {
			  if (getPacketType(packet).equals(ErrorSimulator.PacketType.WRITE)) {
				  System.out.println("\nSending duplicate WRQ packets: \n");
				  
			      // Process the packet to send
			      printPacketInfo(packet, PacketAction.SEND);
			      
			      // Send the datagram packet to the server via the send/receive socket.
				  // Note that here we send the WRQ packet once but once this method returns, 
				  // the same packet will be sent again
			      try {
			         sendReceiveSocket.send(packet);
			      } catch (IOException e) {
			         e.printStackTrace();
			         System.exit(1);
			      }
			  } else {
				  System.out.println("Error: packet is not a WRQ.");
			  }
		  } catch (InvalidPacketTypeException e) {
			  System.out.println("Error: packet is not a WRQ.");
		  }
				    
	  } else {
		  // Do not modify packet
		  return;
	  }
  }
  
  
  private void modifyAckPacketUi(DatagramPacket packet) {
	  System.out.println("\n------------- ACK Packet Modification Menu ----------------");
	  System.out.println("Enter the error number of the error would you like to simulate: \n");
	  
	  System.out.println("0 : No error (i.e do not modify the packet)");
	  
	  System.out.println("1 : Invalid ACK packet TFTP opcode");
	  System.out.println("2 : Invalid block number");
	  System.out.println("---------------------------------------------------------------\n");
	  
	  // Get the requested error from the user
	  int error =  Keyboard.getInteger();

	  
	  // Make sure it's a valid entry
	  while(error > 2 || error < 0){
		  System.out.println("try again");
		  error = Keyboard.getInteger();
	  }
	  
	  
     if (error == 1) {
		  //Get input from the user of the 2 bytes they would like to change the original opcode to (i.e. 07 instead of 04)
		  //Then modify the packet
		  System.out.println("Enter desired opcode (2 digits).\n");
  	 
		  //Get requested new opcode from user
		  int opcode = Keyboard.getInteger();
  	 
		  while(opcode > 99 || opcode < 10 ){
			  System.out.println("opcode must be 2 digits\n");
			  opcode = Keyboard.getInteger();
		  }
  	 
		  changePacketOpcode(packet, opcode);	  	  
		  
	  } else if (error == 3) {
			//get input from the user of the block number they would like to use
			//then call method that changes the block number to what the user passed in
			  System.out.println("Enter first digit of desired block number: \n");
			  
			  //Get first digit of requested new block number
			  int firstBlockNumber = Keyboard.getInteger();
			  
			  while(firstBlockNumber > 9 || firstBlockNumber < 0){
				  System.out.println("Must be between 0 and 9\n");
				  firstBlockNumber = Keyboard.getInteger();
			  }
			  
			  System.out.println("Enter second digit of desired block number: \n");
			  
			  //Get second digit of requested new block number
			  int secondBlockNumber = Keyboard.getInteger();
			  
			  while(secondBlockNumber > 9 || secondBlockNumber < 0){
				  System.out.println("Must be between 0 and 9\n");
				  secondBlockNumber = Keyboard.getInteger();
			  }
			  
			  changeBlockNumber(packet, firstBlockNumber, secondBlockNumber);
		  
	  } else {
		  // Do not modify packet
		  return;
	  }
  }
  
  
  private void modifyDataPacketUi(DatagramPacket packet) {
	  System.out.println("\n------------- DATA Packet Modification Menu ----------------");
	  System.out.println("Enter the error number of the error would you like to simulate: \n");
	  
	  System.out.println("0 : No error (i.e do not modify the packet)");
	  
	  System.out.println("1 : Invalid DATA packet TFTP opcode");
	  System.out.println("2 : Invalid block number");
	  System.out.println("3 : Large DATA packet (larger than 516 bytes)");
	  System.out.println("---------------------------------------------------------------\n");
	  
	  // Get the requested error from the user
	  int error =  Keyboard.getInteger();

	  
	  // Make sure it's a valid entry
	  while(error > 3 || error < 0){
		  System.out.println("try again");
		  error = Keyboard.getInteger();
	  }
	  
	  
     if (error == 1) {
		  //Get input from the user of the 2 bytes they would like to change the original opcode to (i.e. 07 instead of 03)
		  //Then modify the packet
		  System.out.println("Enter desired opcode (2 digits).\n");
 	 
		  //Get requested new opcode from user
		  int opcode = Keyboard.getInteger();
 	 
		  while(opcode > 99 || opcode < 10 ){
			  System.out.println("opcode must be 2 digits");
			  opcode = Keyboard.getInteger();
		  }
 	 
		  changePacketOpcode(packet, opcode);	  
		  
	  } else if (error == 2) {
			//get input from the user of the block number they would like to use
			//then call method that changes the block number to what the user passed in
			  System.out.println("Enter first digit of desired block number: \n");
			  
			  //Get first digit of requested new block number
			  int firstBlockNumber = Keyboard.getInteger();
			  
			  while(firstBlockNumber > 9 || firstBlockNumber < 0){
				  System.out.println("Must be between 0 and 9\n");
				  firstBlockNumber = Keyboard.getInteger();
			  }
			  
			  System.out.println("Enter second digit of desired block number: \n");
			  
			  //Get second digit of requested new block number
			  int secondBlockNumber = Keyboard.getInteger();
			  
			  while(secondBlockNumber > 9 || secondBlockNumber < 0){
				  System.out.println("Must be between 0 and 9\n");
				  secondBlockNumber = Keyboard.getInteger();
			  }
			  
			  changeBlockNumber(packet, firstBlockNumber, secondBlockNumber);
		  
	  } else if (error == 3) {
		  // Add junk data to the original packet until the packet is 517 bytes long
		  byte[] newData = new byte[517];
		  System.arraycopy(packet.getData(), packet.getOffset(), newData, 0, packet.getLength());
		  
		  int i = newData.length - 1;
		  while (i >= 0 && newData[i] == 0) {
			  newData[i] = (byte) 82;
			  i--;
		  }
		  
		  packet.setData(newData);
		  System.out.println(Integer.toString(packet.getLength()));
	  } else {
		  // Do not modify packet
		  return;
	  }
  }
  
  private void changePacketOpcode(DatagramPacket data, int newOpCode){
	  byte[] pData = new byte[data.getLength()];
	  System.arraycopy(data.getData(), data.getOffset(), pData, 0, data.getLength());
	 
	  pData[0] = (byte)Integer.parseInt(Integer.toString(newOpCode).substring(0,1));
	  pData[1] = (byte)Integer.parseInt(Integer.toString(newOpCode).substring(1, 2));
	 
	  data.setData(pData);
  }
  
  private void changePacketMode(DatagramPacket data, String newMode){
	  byte[] pData = new byte[data.getLength()];
	  System.arraycopy(data.getData(), data.getOffset(), pData, 0, data.getLength());
	  
	  byte[] bArray = newMode.getBytes();
	  
	  int modeSize = 0;
	  
	  for (int i = pData.length - 2; pData[i] != 0; i--){
		  modeSize++;
	  }
	  
	  byte[] newData = new byte[pData.length - modeSize + bArray.length];
	  
	  System.arraycopy(pData, 0, newData, 0, pData.length - modeSize - 1);
	  System.arraycopy(bArray, 0, newData, pData.length - modeSize - 1, bArray.length);
	  
	  data.setData(newData);
	    
  }
  
  private void changeBlockNumber(DatagramPacket data, int firstBlockNumber, int secondBlockNumber){
	  byte[] pData = new byte[data.getLength()];
	  System.arraycopy(data.getData(), data.getOffset(), pData, 0, data.getLength());
	  
	  pData[2] = (byte) firstBlockNumber;
	  pData[3] = (byte) secondBlockNumber;
	  
	  data.setData(pData);
	  
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
      
      // Show the menu UI for packet modification for a request packet
      modifyRequestPacketUi(sendPacketServer);
      
      
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
      byte dataFromClient[] = new byte[517];
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
      
      // Process the packet received
      printPacketInfo(receivePacketClient, PacketAction.RECEIVE);
       
      // Construct a datagram packet to send to the Server
      // This assumes that the Server is running on localhost
      sendPacketServer = new DatagramPacket(receivePacketClient.getData(), receivePacketClient.getLength(),
            receivePacketServer.getAddress(), receivePacketServer.getPort());
      
      
      // Check the type of the packet received (i.e. ACK or DATA) and show the proper packet modification menu UI
      try {
    	  ErrorSimulator.PacketType typeOfPacketReceived = getPacketType(sendPacketServer);
    	  
    	  if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.ACK)) {
    		  modifyAckPacketUi(sendPacketServer);
    		  
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  modifyDataPacketUi(sendPacketServer);
    	  }
      } catch (InvalidPacketTypeException e) {
    	  System.out.println("InvalidPacketTypeException thrown: received packet with invalid opcode from server or client: " + e.getMessage());
      }
      
      
      // Process the packet to send
      printPacketInfo(sendPacketServer, PacketAction.SEND);
          
      // Send the datagram packet to the server via the send/receive socket. 
      try {
        sendReceiveSocket.send(sendPacketServer);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }

      System.out.println("Error Simulator: packet sent");
     
  }
  
  private void receiveFromServerAndSendToClient() {
      // Construct a DatagramPacket for receiving packets
      byte dataFromServer[] = new byte[517];
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
      
      
      // Check the type of the packet received (i.e. ACK or DATA) and show the proper packet modification menu UI
      try {
    	  ErrorSimulator.PacketType typeOfPacketReceived = getPacketType(sendPacketClient);
    	  
    	  if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.ACK)) {
    		  modifyAckPacketUi(sendPacketClient);
    		  
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  modifyDataPacketUi(sendPacketClient);
    	  }
      } catch (InvalidPacketTypeException e) {
    	  System.out.println("InvalidPacketTypeException thrown: received packet with invalid opcode from server or client: " + e.getMessage());
      }
      
      // Process the packet to send
      printPacketInfo(sendPacketClient, PacketAction.SEND);
      
        
      // Send the datagram packet to the Client
      try {
        sendReceiveSocket.send(sendPacketClient);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
        
      
     
      System.out.println("Error Simulator: packet sent");
      
    
  }

}
