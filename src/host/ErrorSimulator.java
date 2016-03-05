package host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import util.Keyboard;


public class ErrorSimulator {
	
	public static final String SERVER_ADDRESS = "localhost";
	public static final int SERVER_PORT = 69;
	
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
	
	
	private DatagramPacket receivePacketClient;
	private DatagramSocket receiveSocket;
	
	
	public ErrorSimulator() {
	    try {
	    	receiveSocket = new DatagramSocket(68);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	    
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
	
	
	public ErrorToSimulate getErrorToSimulateInputFromUser() {
		  
		  ErrorToSimulate errorToSimulate = null;
		  
		  System.out.println("\n------------- Error Simulator Menu ----------------");
		 
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
	      // Nothing to do here. No user input needed
			  
		  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_OPCODE) {
			  
			  System.out.print("   Enter the number of the ACK packet you would like to change the opCode for (i.e. 1): ");
			  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
					  
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
			  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  
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
			  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  
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
			  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  
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
			  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  
		  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.LOSE_PACKET) {
		      System.out.println("   Choose the number corresponding to the type of packet you want to lose: ");
				  
		      System.out.println("     1 : RRQ");
		      System.out.println("     2 : WRQ"); 
		      System.out.println("     3 : ACK");
		      System.out.println("     4 : DATA"); 
		      
		      int packetTypeInt = Keyboard.getInteger();
		      
			  while(packetTypeInt > 4 || packetTypeInt < 1){
				  System.out.println("Invalid option. Try again: \n");
				  packetTypeInt = Keyboard.getInteger();
			  }
			  
			  if (packetTypeInt == 1) {
				  errorToSimulate.setPacketType(PacketType.READ);
			  } else if (packetTypeInt == 2) {
				  errorToSimulate.setPacketType(PacketType.WRITE);
			  } else if (packetTypeInt == 3) {
				  errorToSimulate.setPacketType(PacketType.ACK);
			  } else if (packetTypeInt == 4) {
				  errorToSimulate.setPacketType(PacketType.DATA);
			  }
			  
			  if (!errorToSimulate.getPacketType().equals(PacketType.READ) && !errorToSimulate.getPacketType().equals(PacketType.WRITE)) {
				  System.out.print("   Enter the number of the packet you want to lose (i.e. 1): ");
				  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  } else {
				  errorToSimulate.setTargetPacketNumber(0);
			  }
			  
		  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DELAY_PACKET) {
		      System.out.println("   Choose the number corresponding to the type of packet you want to delay: ");
				  
		      System.out.println("     1 : RRQ");
		      System.out.println("     2 : WRQ"); 
		      System.out.println("     3 : ACK");
		      System.out.println("     4 : DATA"); 
		      
		      int packetTypeInt = Keyboard.getInteger();
		      
			  while(packetTypeInt > 4 || packetTypeInt < 1){
				  System.out.println("Invalid option. Try again: \n");
				  packetTypeInt = Keyboard.getInteger();
			  }
			  
			  if (packetTypeInt == 1) {
				  errorToSimulate.setPacketType(PacketType.READ);
			  } else if (packetTypeInt == 2) {
				  errorToSimulate.setPacketType(PacketType.WRITE);
			  } else if (packetTypeInt == 3) {
				  errorToSimulate.setPacketType(PacketType.ACK);
			  } else if (packetTypeInt == 4) {
				  errorToSimulate.setPacketType(PacketType.DATA);
			  }
			  
			  if (!errorToSimulate.getPacketType().equals(PacketType.READ) && !errorToSimulate.getPacketType().equals(PacketType.WRITE)) {
				  System.out.print("   Enter the number of the packet you want to delay (i.e. 1): ");
				  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  } else {
				  errorToSimulate.setTargetPacketNumber(0);
			  }
			  
			  System.out.print("\n   Enter the delay (in milliseconds) for this packet: ");
			  int delayBetweenDuplicates = Keyboard.getInteger();
			  
			  errorToSimulate.setDelayTime(delayBetweenDuplicates);
			  
			    
		  } else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DUPLICATE_PACKET) {
		      System.out.println("   Choose the number corresponding to the type of packet you want to duplicate: ");
				  
		      System.out.println("     1 : RRQ");
		      System.out.println("     2 : WRQ"); 
		      System.out.println("     3 : ACK");
		      System.out.println("     4 : DATA"); 
		      
		      int packetTypeInt = Keyboard.getInteger();
		      
			  while(packetTypeInt > 4 || packetTypeInt < 1){
				  System.out.println("Invalid option. Try again: \n");
				  packetTypeInt = Keyboard.getInteger();
			  }
			  
			  if (packetTypeInt == 1) {
				  errorToSimulate.setPacketType(PacketType.READ);
			  } else if (packetTypeInt == 2) {
				  errorToSimulate.setPacketType(PacketType.WRITE);
			  } else if (packetTypeInt == 3) {
				  errorToSimulate.setPacketType(PacketType.ACK);
			  } else if (packetTypeInt == 4) {
				  errorToSimulate.setPacketType(PacketType.DATA);
			  }
			  
			  if (!errorToSimulate.getPacketType().equals(PacketType.READ) && !errorToSimulate.getPacketType().equals(PacketType.WRITE)) {
				  System.out.print("   Enter the number of the packet you want to duplicate (i.e. 1): ");
				  errorToSimulate.setTargetPacketNumber(Keyboard.getInteger());
			  } else {
				  errorToSimulate.setTargetPacketNumber(0);
			  }
			  
			  System.out.print("\n   Enter the delay (in milliseconds) between the duplicate packets: ");
			  int delayBetweenDuplicates = Keyboard.getInteger();
			  
			  errorToSimulate.setDelayTime(delayBetweenDuplicates);
					    
		  } else {
			  // No error
		  }
		  
		  return errorToSimulate;
	}
	 
	
	public void receiveFromClientAndSendToServer() {
		
		ErrorToSimulate errorToSimulate = getErrorToSimulateInputFromUser();
		
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
	    
	    Thread requestThread = new Thread(new ErrorSimulatorRequest(receivePacketClient, errorToSimulate), 
	    		"ErrorSim Request Thread (For Host " + receivePacketClient.getAddress() + ")");
	    
	    System.out.println("  ** Starting new thread with ID " + requestThread.getId() + "...\n");
	    
	    requestThread.start();
	    
	    
	    try {
		    requestThread.join();
	    } catch (InterruptedException e) {
	    	
	    }
	    
	    
	   
	}
	
	
	public static void main(String args[]) {
		ErrorSimulator newErrorSim = new ErrorSimulator();
		
		while (true) {
			newErrorSim.receiveFromClientAndSendToServer();
		}
		
	}

}
