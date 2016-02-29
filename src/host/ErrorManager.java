package host;

import host.ErrorSimulator.PacketType;

import java.lang.reflect.Array;
import java.net.DatagramPacket;

import util.Keyboard;

public class ErrorManager {
	

	private ErrorToSimulate errorToSimulate;
	private int targetPacketNumber;
	
	
    public ErrorManager() {
	  
    }
    
    public ErrorToSimulate getErrorToSimulate() {
    	return errorToSimulate;
    }
    
    public int getTargetPacketNumber() {
    	return targetPacketNumber;
    }
    
    public void setTargetPacketNumber(int num) {
    	this.targetPacketNumber = num;
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
		  
		  
	  } else {
		  // No error
	  }
    }
    
    
    private PacketType getPacketType(DatagramPacket packet) throws InvalidPacketTypeException {
  	  byte[] data = packet.getData();
  	  
  	  if (data[0] == 0) {
  		  
  		  if (data[1] == PacketType.READ.getOpcode()) {
  			  return PacketType.READ;
  			  
  		  } else if (data[1] == PacketType.WRITE.getOpcode()) {
  			  return PacketType.WRITE;
  			  
  		  } else if (data[1] == PacketType.ACK.getOpcode()) {
  			  return PacketType.ACK;
  			  
  		  } else if (data[1] == PacketType.DATA.getOpcode()) {
  			  return PacketType.DATA;
  			  
  		  } else if (data[1] == PacketType.ERROR.getOpcode()) {
  			  return PacketType.ERROR;
  			  
  		  } else {
  			  throw new InvalidPacketTypeException("Invalid packet type: second byte is " + data[1]);  
  		  }
  		  
  	  } else {
  		 throw new InvalidPacketTypeException("Invalid packet type: first byte is not a 0 byte");
  	  }
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
    
    
    public DatagramPacket simulateError(DatagramPacket packet, int packetNumber) {
    	try {
    		PacketType packetType = getPacketType(packet);
    		
        	if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_RQ_OPCODE) {
        		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
        		
        			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting invalid opCode ****");
        			
        			// Modify the opCode and return the packet
        			changePacketOpcode(packet, errorToSimulate.getOpcode());
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_FILENAME) {
        		
        		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
        			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting empty file name ****");
        			
            		// Remove the filename and return the packet
        			emptyFileNameFromRequestPacket(packet);
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.EMPTY_MODE) {
        		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
        			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting empty mode ****");
        			
            		// Remove the mode and return the packet
        			changePacketMode(packet, "");
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_MODE) {
        		if (packetType.equals(PacketType.READ) || packetType.equals(PacketType.WRITE)) {
        			System.out.println("\n **** Modifying RRQ/WRQ Packet...Setting invalid mode ****");
        			
            		// Change the mode and return the packet
        			changePacketMode(packet, errorToSimulate.getMode());
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.DUPLICATE_WRQ_PACKET) {
        		if (packetType.equals(PacketType.WRITE)) {
        			// TODO
        		} else {
        			return packet;
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_OPCODE) {
        		if (packetType.equals(PacketType.ACK) && packetNumber == targetPacketNumber) {
        			System.out.println("\n **** Modifying ACK Packet #" + packetNumber + "...Setting invalid opCode ****");
        			
        			// Modify the ACK packet opCode and return the packet
        			changePacketOpcode(packet, errorToSimulate.getOpcode());
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_ACK_BLOCK_NUMBER) {
        		if (packetType.equals(PacketType.ACK) && packetNumber == targetPacketNumber) {
        			System.out.println("\n **** Modifying ACK Packet #" + packetNumber + "...Setting invalid block number ****");
        			
        			// Modify the ACK packet block number and return the packet
        			changePacketBlockNumber(packet, errorToSimulate.getBlockNumber());
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_OPCODE) {
        		if (packetType.equals(PacketType.DATA) && packetNumber == targetPacketNumber) {
        			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting invalid opCode ****")
        			;
        			// Modify the DATA packet opCode and return the packet
        			changePacketOpcode(packet, errorToSimulate.getOpcode());
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.INVALID_DATA_BLOCK_NUMBER) {
        		if (packetType.equals(PacketType.DATA) && packetNumber == targetPacketNumber) {
        			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting invalid block number ****");
        			
        			// Modify the DATA packet block number and return the packet
        			changePacketBlockNumber(packet, errorToSimulate.getBlockNumber());
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        		
        	} else if (errorToSimulate.getType() == ErrorToSimulate.ErrorToSimulateType.LARGE_DATA_PACKET) {
        		if (packetType.equals(PacketType.DATA) && packetNumber == targetPacketNumber) {
        			System.out.println("\n **** Modifying DATA Packet #" + packetNumber + "...Setting packet length larger than 516 bytes ****");
        			
        			makeDataPacketLarger(packet);
        			return packet;
        			
        		} else {
        			return packet; // No error to simulate for this packet, return the original unmodified packet
        		}
        	}
    		
    		
    	} catch (InvalidPacketTypeException e) {
            e.printStackTrace();
            //System.exit(1);
    	}
    	
    	return packet;
    }
    
	

}
