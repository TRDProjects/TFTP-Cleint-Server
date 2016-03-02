package host;

import host.ErrorSimulator.PacketAction;
import host.ErrorSimulator.PacketType;
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
  
  private ErrorToSimulate errorToSimulate;
  private int targetPacketNumber;
  
  private int currentAckPacketNumber, currentDataPacketNumber;
  
  public ErrorSimulatorRequest(DatagramPacket requestPacket) {
    this.requestPacket = requestPacket;
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
  
  
  public void userMenu() {
	  System.out.println("\n------------- Modification Menu For Request ----------------");
	 
	  System.out.println("Enter the error number of the error would you like to simulate: \n");
	  
	  for (ErrorToSimulate.ErrorToSimulateType errorType: ErrorToSimulate.ErrorToSimulateType.values()) {
		  System.out.println(errorType.getErrorNumber() + " : " + errorType.getErrorString());
	  }
	  
	  System.out.println("---------------------------------------------------------------\n");
	  
	  
	  // Get the requested error from the user
	  int error =  Keyboard.getInteger();

	  
	  // Make sure it's a valid entry
	  while(error > ErrorToSimulate.ErrorToSimulateType.values().length - 1  || error < 0){
		  System.out.println("try again");
		  error = Keyboard.getInteger();
	  }
	  
	  // Set the type of error to simulate
	  errorToSimulate = new ErrorToSimulate(error);
	  
	  if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_RQ_OPCODE) {
		  
		  //Get input from the user of the 2 bytes they would like to change the original opcode to (i.e. 07 instead of 01 or 02)
  	  System.out.print("   Enter first digit of desired opcode: ");
    	 
		  //Get requested first digit of opcode
		  int firstOpcode = Keyboard.getInteger();
	 
		  while(firstOpcode > 9 || firstOpcode < 0 ){
			  System.out.println("must be between 0 and 9\n");
			  firstOpcode = Keyboard.getInteger();
		  }
		  
  	  System.out.print("   Enter second digit of desired opcode: ");
    	 
		  //Get requested second digit of opcode
		  int secondOpcode = Keyboard.getInteger();
	 
		  while(secondOpcode > 9 || secondOpcode < 0 ){
			  System.out.println("must be between 0 and 9\n");
			  secondOpcode = Keyboard.getInteger();
		  }
		  
		  
		  errorToSimulate.setOpcode(new byte[]{ (byte) firstOpcode, (byte) secondOpcode });
		
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_FILENAME) {
		  // Empty filename
		  errorToSimulate.setFileName("");

		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_MODE) {
	      // Empty mode
		  errorToSimulate.setMode("");

	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_MODE) {
		  System.out.println("Enter desired mode: ");
		 
		  // Get new mode from user
		  String mode = Keyboard.getString();
		  
		  errorToSimulate.setMode(mode);
		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DUPLICATE_WRQ_PACKET) {
      // TODO
		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_OPCODE) {
		  
		  System.out.print("   Enter the number of the ACK packet you would like to change the opCode for (i.e. 1): ");
		  targetPacketNumber = Keyboard.getInteger();
				  
		  //Get input from the user of the 2 bytes they would like to change the original opcode to (i.e. 07 instead of 04)
		  System.out.print("      Enter first digit of desired opcode: \n");
		  
		  //Get requested first digit of opcode
		  int firstOpcode = Keyboard.getInteger();
	 
		  while(firstOpcode > 9 || firstOpcode < 0 ){
			  System.out.println("must be between 0 and 9\n");
			  firstOpcode = Keyboard.getInteger();
		  }
		  
  	 System.out.print("      Enter second digit of desired opcode: \n");
    	 
		  //Get requested second digit of opcode
		  int secondOpcode = Keyboard.getInteger();
	 
		  while(secondOpcode > 9 || secondOpcode < 0 ){
			  System.out.println("must be between 0 and 9\n");
			  secondOpcode = Keyboard.getInteger();
		  }
		  
		  errorToSimulate.setOpcode(new byte[]{ (byte) firstOpcode, (byte) secondOpcode });
		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_BLOCK_NUMBER) {
		  
		  System.out.print("   Enter the number of the ACK packet you would like to change the block number for (i.e. 1): ");
		  targetPacketNumber = Keyboard.getInteger();
		  
		  //get input from the user of the block number they would like to use
		  System.out.print("      Enter first digit of desired block number: \n");
		  
		  //Get first digit of requested new block number
		  int firstBlockNumber = Keyboard.getInteger();
		  
		  while(firstBlockNumber > 9 || firstBlockNumber < 0){
			  System.out.println("Must be between 0 and 9\n");
			  firstBlockNumber = Keyboard.getInteger();
		  }
		  
		  System.out.print("      Enter second digit of desired block number: \n");
		  
		  //Get second digit of requested new block number
		  int secondBlockNumber = Keyboard.getInteger();
		  
		  while(secondBlockNumber > 9 || secondBlockNumber < 0){
			  System.out.println("Must be between 0 and 9\n");
			  secondBlockNumber = Keyboard.getInteger();
		  }
		  
		  errorToSimulate.setBlockNumber(new byte[]{ (byte) firstBlockNumber, (byte) secondBlockNumber });

		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_OPCODE) {
		  
		  System.out.print("   Enter the number of the DATA packet you would like to change the opCode for (i.e. 1): ");
		  targetPacketNumber = Keyboard.getInteger();
		  
		  //Get input from the user of the 2 bytes they would like to change the original opcode to (i.e. 07 instead of 03)
  	 System.out.print("      Enter first digit of desired opcode: \n");
    	 
		  //Get requested first digit of opcode
		  int firstOpcode = Keyboard.getInteger();
	 
		  while(firstOpcode > 9 || firstOpcode < 0 ){
			  System.out.println("must be between 0 and 9\n");
			  firstOpcode = Keyboard.getInteger();
		  }
		  
  	 System.out.print("      Enter second digit of desired opcode: \n");
    	 
		  //Get requested second digit of opcode
		  int secondOpcode = Keyboard.getInteger();
	 
		  while(secondOpcode > 9 || secondOpcode < 0 ){
			  System.out.println("must be between 0 and 9\n");
			  secondOpcode = Keyboard.getInteger();
		  }

		  errorToSimulate.setOpcode(new byte[]{ (byte) firstOpcode, (byte) secondOpcode });
		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_BLOCK_NUMBER) {
		  
		  System.out.print("   Enter the number of the DATA packet you would like to change the block number for (i.e. 1): ");
		  targetPacketNumber = Keyboard.getInteger();
		  
		  //get input from the user of the block number they would like to use
		  System.out.print("      Enter first digit of desired block number: \n");
		  
		  //Get first digit of requested new block number
		  int firstBlockNumber = Keyboard.getInteger();
		  
		  while(firstBlockNumber > 9 || firstBlockNumber < 0){
			  System.out.println("Must be between 0 and 9\n");
			  firstBlockNumber = Keyboard.getInteger();
		  }
		  
		  System.out.print("      Enter second digit of desired block number: \n");
		  
		  //Get second digit of requested new block number
		  int secondBlockNumber = Keyboard.getInteger();
		  
		  while(secondBlockNumber > 9 || secondBlockNumber < 0){
			  System.out.println("Must be between 0 and 9\n");
			  secondBlockNumber = Keyboard.getInteger();
		  }
		  
		  errorToSimulate.setBlockNumber(new byte[]{ (byte) firstBlockNumber, (byte) secondBlockNumber });
		  
		  
	  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.LARGE_DATA_PACKET) {
		  System.out.print("   Enter the number of the DATA packet you would like to make larger (i.e. 1): ");
		  targetPacketNumber = Keyboard.getInteger();
		  
		  
	  } else {
		  // No error
	  }
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
  
  
  private void changePacketOpcode(DatagramPacket packet, byte[] newOpcode){
 	 byte[] pData = new byte[packet.getLength()];
 	 System.arraycopy(packet.getData(), packet.getOffset(), pData, 0, packet.getLength());
		 
 	 pData[0] = newOpcode[0];
 	 pData[1] = newOpcode[1];
		 
 	 packet.setData(pData);
  }
 
 
  private void emptyFileNameFromRequestPacket(DatagramPacket packet) {
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
  }
	 
	 
  private void changePacketMode(DatagramPacket packet, String newMode){
		 byte[] pData = new byte[packet.getLength()];
		 System.arraycopy(packet.getData(), packet.getOffset(), pData, 0, packet.getLength());
		  
		 byte[] bArray = newMode.getBytes();
		  
	     int modeSize = 0;
		  
		 for (int i = pData.length - 2; pData[i] != 0; i--){
			 modeSize++;
		 }
		  
		 byte[] newData = new byte[pData.length - modeSize + bArray.length];
		  
		 System.arraycopy(pData, 0, newData, 0, pData.length - modeSize - 1);
		 System.arraycopy(bArray, 0, newData, pData.length - modeSize - 1, bArray.length);
		  
		 packet.setData(newData);
	   
  }
	 
	 
  private void changePacketBlockNumber(DatagramPacket data, byte[] blockNumber){
	  	byte[] pData = new byte[data.getLength()];
	  	System.arraycopy(data.getData(), data.getOffset(), pData, 0, data.getLength());
	  
	  	pData[2] = blockNumber[0];
	  	pData[3] = blockNumber[1];
	  
	  	data.setData(pData);
	  
  }
	 
  private void makeDataPacketLarger(DatagramPacket packet) {
	  	// Add junk data to the original packet until the packet is 517 bytes long
	 	byte[] newData = new byte[517];
	 	System.arraycopy(packet.getData(), packet.getOffset(), newData, 0, packet.getLength());
		  
	 	int i = newData.length - 1;
	 	while (i >= 0 && newData[i] == 0) {
	 		newData[i] = (byte) 82;
	 		i--;
	 	}
		  
	 	packet.setData(newData);
 }
 

 public void simulateError(DatagramPacket packet, int packetNumber) {
 	try {
 		PacketType packetType = getPacketType(packet);
 		
     	if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_RQ_OPCODE) {
     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
     		
     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting invalid opCode ****");
     			
     			// Modify the opCode and return the packet
     			changePacketOpcode(packet, errorToSimulate.getOpcode());
     			
     		}
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_FILENAME) {
     		
     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting empty file name ****");
     			
         		// Remove the filename and return the packet
     			emptyFileNameFromRequestPacket(packet);
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_MODE) {
     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting empty mode ****");
     			
         		// Remove the mode and return the packet
     			changePacketMode(packet, "");
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_MODE) {
     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting invalid mode ****");
     			
         		// Change the mode and return the packet
     			changePacketMode(packet, errorToSimulate.getMode());
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DUPLICATE_WRQ_PACKET) {
     		if (packetType.equals(PacketType.WRITE)) {
     			System.out.println("\n **** Sending an extra WRQ packet ****");
     		    // Send the packet now, then the packet will be sent again (see end of method)
     		    sendPacket(sendReceiveSocket, packet);
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_OPCODE) {
     		if (packetType.equals(PacketType.ACK) && packetNumber == targetPacketNumber) {
     			System.out.println("\n **** Modifying ACK Packet #" + packetNumber + "...Setting invalid opCode ****");
     			
     			// Modify the ACK packet opCode and return the packet
     			changePacketOpcode(packet, errorToSimulate.getOpcode());
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_BLOCK_NUMBER) {
     		if (packetType.equals(PacketType.ACK) && packetNumber == targetPacketNumber) {
     			System.out.println("\n **** Modifying ACK Packet #" + packetNumber + "...Setting invalid block number ****");
     			
     			// Modify the ACK packet block number and return the packet
     			changePacketBlockNumber(packet, errorToSimulate.getBlockNumber());
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_OPCODE) {
     		if (packetType.equals(PacketType.DATA) && packetNumber == targetPacketNumber) {
     			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting invalid opCode ****")
     			;
     			// Modify the DATA packet opCode and return the packet
     			changePacketOpcode(packet, errorToSimulate.getOpcode());
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_BLOCK_NUMBER) {
     		if (packetType.equals(PacketType.DATA) && packetNumber == targetPacketNumber) {
     			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting invalid block number ****");
     			
     			// Modify the DATA packet block number and return the packet
     			changePacketBlockNumber(packet, errorToSimulate.getBlockNumber());
     			
     		}
     		
     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.LARGE_DATA_PACKET) {
     		if (packetType.equals(PacketType.DATA) && packetNumber == targetPacketNumber) {
     			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting packet length larger than 516 bytes ****");
     			
     			makeDataPacketLarger(packet);
     			
     		}
     	}
 		
 		
 	} catch (InvalidPacketTypeException e) {
         e.printStackTrace();
         //System.exit(1);
 	}
 	

	// Send the packet (either modified or not modified)
	sendPacket(sendReceiveSocket, packet);
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
      userMenu();
      
      simulateError(sendPacketServer, 0);
      
      
      
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
    		  simulateError(sendPacketServer, currentAckPacketNumber);
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  currentDataPacketNumber++;
    		  simulateError(sendPacketServer, currentDataPacketNumber);
    	  } else {
    		  simulateError(sendPacketServer, 0);
    	  }
      } catch (InvalidPacketTypeException e) {
    	 // Do nothing
      }
      
      
     
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
    		  simulateError(sendPacketClient, currentAckPacketNumber);
    		  
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  currentDataPacketNumber++;
    		  simulateError(sendPacketClient, currentDataPacketNumber);
    		  
    	  } else {
    		  simulateError(sendPacketClient, 0);
    	  }
      } catch (InvalidPacketTypeException e) {
    	  //System.out.println("InvalidPacketTypeException thrown: received packet with invalid opcode from server or client: " + e.getMessage());
      }
      
      
    
  }

}
