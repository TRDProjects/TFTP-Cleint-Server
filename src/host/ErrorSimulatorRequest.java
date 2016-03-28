package host;

import host.ErrorSimulator.PacketAction;
import host.ErrorSimulator.PacketType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;


public class ErrorSimulatorRequest implements Runnable {
	
  public static final int RECEIVE_TIMOUT_BEFORE_CLOSING_THREAD = 2000;
	 
  private DatagramPacket receivePacket, sendPacket;
	      
  private DatagramSocket sendReceiveSocket;
  
  private DatagramPacket requestPacket;
  
  private ErrorToSimulate errorToSimulate;
  
  private int currentAckPacketNumber, currentDataPacketNumber;
  
  private InetAddress serverAddress;
  private InetAddress clientAddress;
  private int serverRequestThreadPort;
  private int clientPort;
  
  public ErrorSimulatorRequest(DatagramPacket requestPacket, ErrorToSimulate errorToSimulate) {
    this.requestPacket = requestPacket;
    this.errorToSimulate = errorToSimulate;
    this.currentAckPacketNumber = 0;
    this.currentDataPacketNumber = 0;
    
    try {
      sendReceiveSocket = new DatagramSocket();
      
      sendReceiveSocket.setSoTimeout(RECEIVE_TIMOUT_BEFORE_CLOSING_THREAD);
      
    } catch (SocketException se) {
     se.printStackTrace();
      System.exit(1);
    }
    
    this.clientAddress = requestPacket.getAddress();
    this.clientPort = requestPacket.getPort();
    
    try {
    	serverAddress = InetAddress.getByName(ErrorSimulator.SERVER_ADDRESS);
    } catch (UnknownHostException e) {
        e.printStackTrace();
        System.exit(1);
    }
  }
  
 
 
  public void printPacketInfo(DatagramPacket packet, ErrorSimulator.PacketAction action) {
	String packetType = "";
	try {
		packetType = getPacketType(packet).name();
	} catch (InvalidPacketTypeException e) {
		packetType = "";
	}
	
    System.out.println("\n");
    System.out.println("ErrorSimulator (" + Thread.currentThread() + "): " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + packetType + " packet:");
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
	  
  public DatagramPacket receivePacket(DatagramSocket socket, int bufferLength) throws SocketTimeoutException {
	  
	  // Construct a DatagramPacket for receiving packets
      byte dataBuffer[] = new byte[bufferLength];
      DatagramPacket receivedPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
      System.out.println("Error Simulator: Waiting for Packet.\n");
      
      // Block until a datagram packet is received from the socket
      try {        
          System.out.println("Waiting...");
          socket.receive(receivedPacket);
      } catch (SocketTimeoutException te) {
    	  throw te;
      } catch (IOException e) {
          System.out.print("IO Exception: " + e);
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

  public void simulateErrorAndSendPacket(DatagramPacket packet, int packetNumber) {
	boolean sendThePacket = true;
	
 	try {
 		PacketType packetType = getPacketType(packet);
 		
 		if (!errorToSimulate.wasExecuted()) {
 	     	if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_RQ_OPCODE) {
 	     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
 	     		
 	     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting invalid opCode ****");
 	     			
 	     			// Modify the opCode and return the packet
 	     			changePacketOpcode(packet, errorToSimulate.getOpcode());
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_FILENAME) {
 	     		
 	     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
 	     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting empty file name ****");
 	     			
 	         		// Remove the filename and return the packet
 	     			emptyFileNameFromRequestPacket(packet);
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_MODE) {
 	     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
 	     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting empty mode ****");
 	     			
 	         		// Remove the mode and return the packet
 	     			changePacketMode(packet, "");
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_MODE) {
 	     		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
 	     			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting invalid mode ****");
 	     			
 	         		// Change the mode and return the packet
 	     			changePacketMode(packet, errorToSimulate.getMode());
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_OPCODE) {
 	     		if (packetType.equals(PacketType.ACK) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			System.out.println("\n **** Modifying ACK Packet #" + packetNumber + "...Setting invalid opCode ****");
 	     			
 	     			// Modify the ACK packet opCode and return the packet
 	     			changePacketOpcode(packet, errorToSimulate.getOpcode());
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_BLOCK_NUMBER) {
 	     		if (packetType.equals(PacketType.ACK) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			System.out.println("\n **** Modifying ACK Packet #" + packetNumber + "...Setting invalid block number ****");
 	     			
 	     			// Modify the ACK packet block number and return the packet
 	     			changePacketBlockNumber(packet, errorToSimulate.getBlockNumber());
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_OPCODE) {
 	     		if (packetType.equals(PacketType.DATA) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting invalid opCode ****")
 	     			;
 	     			// Modify the DATA packet opCode and return the packet
 	     			changePacketOpcode(packet, errorToSimulate.getOpcode());
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_BLOCK_NUMBER) {
 	     		if (packetType.equals(PacketType.DATA) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting invalid block number ****");
 	     			
 	     			// Modify the DATA packet block number and return the packet
 	     			changePacketBlockNumber(packet, errorToSimulate.getBlockNumber());
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     		
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.LARGE_DATA_PACKET) {
 	     		if (packetType.equals(PacketType.DATA) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting packet length larger than 516 bytes ****");
 	     			
 	     			makeDataPacketLarger(packet);
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     		}
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.LOSE_PACKET) {
 	     		if (packetType.equals(errorToSimulate.getPacketType()) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			if (errorToSimulate.getPacketType().equals(PacketType.READ)) {
 	     				System.out.println("\n **** Losing RRQ Packet ***");
 	     			} else if (errorToSimulate.getPacketType().equals(PacketType.WRITE)) {
 	     				System.out.println("\n **** Losing WRQ Packet ***"); 
 	     			} else {
 	     	  			System.out.println("\n **** Losing " + errorToSimulate.getPacketType().name() + " Packet #" + packetNumber + " ****");
 	     			}
 	     			
 	     			
 	     			errorToSimulate.setWasExecuted(true);
 	     			
 	     			// Don't send the packet
 	     			sendThePacket = false;
 	     		}
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DELAY_PACKET) {
 	     		if (packetType.equals(errorToSimulate.getPacketType()) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			if (errorToSimulate.getPacketType().equals(PacketType.READ)) {
 	     				System.out.println("\n **** Delaying RRQ Packet ***"); 
 	     			} else if (errorToSimulate.getPacketType().equals(PacketType.WRITE)) {
 	     				System.out.println("\n **** Delaying WRQ Packet ***"); 
 	     			} else {
 	     	  			System.out.println("\n **** Delaying " + errorToSimulate.getPacketType().name() + 
 	     	  					" Packet #" + packetNumber + 
 	     	  					" by " + errorToSimulate.getDelayTime() + "ms ****");
 	     			}
 	     			
 	     			// Delay sending the packet
 	     			try {
 	     			    Thread.sleep(errorToSimulate.getDelayTime());
 	     			} catch(InterruptedException e){
 	     			    e.printStackTrace();
 	     			}
 	     			
 	     			errorToSimulate.setWasExecuted(true);

 	     		}
 	     	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DUPLICATE_PACKET) {
 	     		if (packetType.equals(errorToSimulate.getPacketType()) && packetNumber == errorToSimulate.getTargetPacketNumber()) {
 	     			if (errorToSimulate.getPacketType().equals(PacketType.READ) || errorToSimulate.getPacketType().equals(PacketType.WRITE)) {
 	     				System.out.println("\n **** Duplicating " + errorToSimulate.getPacketType().name() + " Request Packet ***");
 	     				
 	 	 	     			// Send the first duplicate packet now
 	 	 	     			sendPacket(sendReceiveSocket, packet);
 	 	 	     				
 	 	         		    try {
 	 	         		    	DatagramPacket firstReceivedResponse = receivePacket(sendReceiveSocket, 517);
 	 	         		    	
 	 	         		    	
 	 	 	         		    DatagramPacket firstPacketToSend = new DatagramPacket(
 	 	 	         		    		firstReceivedResponse.getData(), 
 	 	 	         		    		firstReceivedResponse.getData().length,
 	 	 	         		    		clientAddress, clientPort);
 	 	 	         		    
 	 	 	         		    // Send the packet to the client
 	 	 	         		    sendPacket(sendReceiveSocket, firstPacketToSend);
 	 	 	         		    
 	 	 	 	     			// Delay
 	 		 	     			System.out.println("\n >>>> Waiting for " + errorToSimulate.getDelayTime() + " ms <<<<<<\n");
 	 	 	 	     			try {
 	 	 	 	     			    Thread.sleep(errorToSimulate.getDelayTime());
 	 	 	 	     			} catch(InterruptedException e){
 	 	 	 	     			    e.printStackTrace();
 	 	 	 	     			}
 	 	 	         		    
 	 	 	         		    
 	 	 	         		    try {
 	 	 	 	     				DatagramSocket tempSocket = new DatagramSocket();
 	 	 	 	     				
 	 	 	 	         		    // Send the second duplicate packet to the server now
 	 	 	 	         		    sendPacket(tempSocket, packet);
 	 	 	 	         		    
 	 	 	 	         		    DatagramPacket secondReceivedResponse = receivePacket(tempSocket, 517);
 	 	 	 	         		    
 	 	 	 	         		    DatagramPacket secondPacketToSend = new DatagramPacket(
 	 	 	 	         		    		secondReceivedResponse.getData(), 
 	 	 	 	         		    		secondReceivedResponse.getLength(),
 	 	 	 	         		    		clientAddress, clientPort);
 	 	 	 	     				
 	 	 	 	         		    // Send the packet to client
 	 	 	 	         		    sendPacket(tempSocket, secondPacketToSend);
 	 	 	 	         		    
 	 	 	 	         		    DatagramPacket clientResponsePacket = receivePacket(tempSocket, 517);
 	 	 	 	         		    
 	 	 	 	         		    DatagramPacket finalPacketToSendToServer = new DatagramPacket(
 	 	 	 	         		    		clientResponsePacket.getData(), 
 	 	 	 	         		    		clientResponsePacket.getLength(), 
 	 	 	 	         		    		serverAddress, secondReceivedResponse.getPort());
 	 	 	 	         		    
 	 	 	 	         		    // Send the packet to server
 	 	 	 	         		    sendPacket(tempSocket, finalPacketToSendToServer);
 	 	 	 	         		    
 	 	 	 	         		    DatagramPacket clientSentPacket = receivePacket(sendReceiveSocket, 517);
 	 	 	 	         		    sendPacket(sendReceiveSocket, new DatagramPacket(
 	 	 	 	         		    		clientSentPacket.getData(), 
 	 	 	 	         		    		clientSentPacket.getLength(), 
 	 	 	 	         		    		serverAddress, 
 	 	 	 	         		    	    firstReceivedResponse.getPort()));
 	 	 	 	         		    
 	 	 	 	         		    
 	 	 	 	         		    // Already sent both packets so no need to send more
 	 	 	 	         		    sendThePacket = false;
 	 	 	 	         		    
 	 	 	 	         		    errorToSimulate.setWasExecuted(true);
 	 	 	 	         		    
 	 	 	 	         		    
 	 	 	         		    } catch (SocketException se) {
 	 	 	 	     	            se.printStackTrace();
 	 	 	 	     	            System.exit(1);
 	 	 	 	     	        }
 	 	 	         		    
 	 	 	         		    
 	 	         		    } catch (SocketTimeoutException e) {
 	 	         		    	
 	 	         		    }
 	     				
 	     				
 	     			} else {
 	     	  			System.out.println("\n **** Duplicating " + errorToSimulate.getPacketType().name() + 
 	     	  					" Packet #" + packetNumber + 
 	     	  					". Delay time is " + errorToSimulate.getDelayTime() + " ms ****");
 	     	  			
 	 	     			// Send the first duplicate packet now
 	 	     			sendPacket(sendReceiveSocket, packet);
 	 	     			
 	 	     			System.out.println("\n >>>> Waiting for " + errorToSimulate.getDelayTime() + " ms <<<<<<\n");
 	 	     			
 	 	     			// Delay
 	 	     			try {
 	 	     			    Thread.sleep(errorToSimulate.getDelayTime());
 	 	     			} catch(InterruptedException e){
 	 	     			    e.printStackTrace();
 	 	     			}
 	 	     			
 	 	     			errorToSimulate.setWasExecuted(true);
 	 	     			
 	 	     			// The second duplicate packet will be sent when the try block exits
 	     			}
 	     			
 	     		}
 	     	}
 		}
 		
 		
 	} catch (InvalidPacketTypeException e) {
         e.printStackTrace();
         //System.exit(1);
 	}
 	

 	if (sendThePacket) {
 		// Send the packet (either modified or not modified)
 		sendPacket(sendReceiveSocket, packet);
 	}
 }

  @Override
  public void run() {
	  
      // Process the received request packet
      printPacketInfo(requestPacket, PacketAction.RECEIVE);
    
      // Construct a packet to send to the Server
      sendPacket = new DatagramPacket(requestPacket.getData(), requestPacket.getLength(), serverAddress, ErrorSimulator.SERVER_PORT);
      
      // If the user selects an option that simulates an error for a RRQ/WRQ packet then the simulatError method will simulate that error 
      // and send the packet to the server, otherwise the simulateError method will send the original unmodified request packet
      simulateErrorAndSendPacket(sendPacket, 0);
      
      
      // Receive the first packet from the server
      try {
          receivePacket = receivePacket(sendReceiveSocket, 517);
      } catch (SocketTimeoutException e) {
    	  System.out.println("\n**** Socket Timeout...Error Simulator Request Thread # " + Thread.currentThread().getId() + " Finished...Closing...*****");
    	  Thread.currentThread().interrupt();
    	  return;
      }
     
      this.serverRequestThreadPort = receivePacket.getPort();
      
    
      // Construct a datagram packet to send to the Client
      sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientAddress, clientPort);
      
      try {
    	  ErrorSimulator.PacketType typeOfPacketReceived = getPacketType(receivePacket);
    	  
    	  if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.ACK)) {
    		  currentAckPacketNumber++;
    		  simulateErrorAndSendPacket(sendPacket, currentAckPacketNumber);
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  currentDataPacketNumber++;
    		  simulateErrorAndSendPacket(sendPacket, currentDataPacketNumber);
    	  } else {
    		  simulateErrorAndSendPacket(sendPacket, 0);
    	  }
      } catch (InvalidPacketTypeException e) {
    	  // Send the packet without attempting to simulate any error
    	 sendPacket(sendReceiveSocket, sendPacket);
      }
      
      
      
      while (true) {
    	  if (Thread.currentThread().isInterrupted()) {
    		  return;
    	  }
    	  receiveAndSendPackets();
      }
    
  }
  
  
  private void receiveAndSendPackets() {
	  // Receive a packet
	  try {
		  receivePacket = receivePacket(sendReceiveSocket, 517);
	  } catch (SocketTimeoutException e) {
		  // No packet has been received within the time specified...Close thread
    	  System.out.println("\n**** Socket Timeout...Error Simulator Request Thread # " + Thread.currentThread().getId() + " Finished...Closing...*****");
		  sendReceiveSocket.close();
		  Thread.currentThread().interrupt();
		  return;
	  }
	  
	  InetAddress destinationAddress;
	  int destinationPort;
	  
	  // Set the destination address and port
	  if (receivePacket.getAddress().equals(clientAddress)) {
		  destinationAddress = serverAddress;
		  destinationPort = serverRequestThreadPort;
		  
	  } else {
		  destinationAddress = clientAddress;
		  destinationPort = clientPort;
	  }
	  
	  
      // Construct a datagram packet to send to the destination
      String dataReceivedString = new String(receivePacket.getData(), 0, receivePacket.getLength());
      
      sendPacket = new DatagramPacket(dataReceivedString.getBytes(), dataReceivedString.getBytes().length,
    		  destinationAddress, destinationPort);
      
	  
      try {
    	  ErrorSimulator.PacketType typeOfPacketReceived = getPacketType(receivePacket);
    	  
    	  if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.ACK)) {
    		  currentAckPacketNumber++;
    		  simulateErrorAndSendPacket(sendPacket, currentAckPacketNumber);
    	  } else if (typeOfPacketReceived.equals(ErrorSimulator.PacketType.DATA)) {
    		  currentDataPacketNumber++;
    		  simulateErrorAndSendPacket(sendPacket, currentDataPacketNumber);
    	  } else {
    		  simulateErrorAndSendPacket(sendPacket, 0);
    	  }
      } catch (InvalidPacketTypeException e) {
    	  // Send the packet without attempting to simulate any error
    	 sendPacket(sendReceiveSocket, sendPacket);
      }
	
  }
  
  

}
