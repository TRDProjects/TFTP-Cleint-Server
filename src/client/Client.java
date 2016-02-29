package client;

import host.ErrorSimulator;
import host.InvalidPacketTypeException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import client.IllegalTftpOperationException;
import client.UnknownTransferIdException;
import server.Server.ErrorType;
import server.Server.PacketAction;
import server.Server.PacketType;
import util.Keyboard;

public class Client {
	
	
	public static enum Mode { 
		NORMAL, TEST 
	};
	
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
	
	public enum ErrorType {
		ILLEGAL_TFTP_OPERATION((byte) 4),
		UNKNOWN_TRANSFER_ID((byte) 5);
		
		private byte errorCode;
		
		private ErrorType(byte errorCode) {
			this.errorCode = errorCode;
		}
		
		public byte getErrorCode() {
			return errorCode;
		}
	}
	
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	
	private Mode mode;
	
	private InetAddress serverAddress;
	
	public Client(Mode mode) {
		this.mode = mode;
		
	    try {
	        sendReceiveSocket = new DatagramSocket();
	        
			serverAddress = InetAddress.getLocalHost();
	  
	    } catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
	    } catch (UnknownHostException e) {
	    	e.printStackTrace();
	        System.exit(1);
		}
	 }
	
	
	public void printPacketInfo(DatagramPacket packet, PacketAction action) {
		System.out.println("\n");
		System.out.println("Client: " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + "packet:");
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "To " : "From ") + "host: " + packet.getAddress());
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "Destination host " : "Host ") + "port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("   " + "Length: " + len);
		System.out.println("   " + "Containing: ");
		String dataString = new String(packet.getData(),0,len);
		System.out.println("       - String: " + dataString);
		System.out.println("       - Bytes: " + Arrays.toString(dataString.getBytes()));
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
	
	
	private byte[] incrementBlockNumber(byte[] currentBlockNum) {
		short blockNum = ByteBuffer.wrap(currentBlockNum).getShort();
		
		byte[] bytes = new byte[2];
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		blockNum++;
		buffer.putShort(blockNum);
		return buffer.array();
	}
	
	private DatagramPacket formDataPacket(InetAddress address, int port, byte[] data, int dataLength, byte[] blockNumber) {
		byte[] dataPacket = new byte [4 + data.length];
		dataPacket[0] = 0;
		dataPacket[1] = PacketType.DATA.getOpcode();
		dataPacket[2] = blockNumber[0];
		dataPacket[3] = blockNumber[1];
		
		System.arraycopy(data, 0, dataPacket, 4, dataLength);
		
		return new DatagramPacket(dataPacket, dataPacket.length, address, port);
	}
	
	private void validateDataPacket(DatagramPacket packet, byte[] expectedBlockNumber)
			throws IllegalTftpOperationException, UnknownTransferIdException {
		
		removeTrailingZeroBytesFromDataPacket(packet);
			
		// Make sure the address the packet is coming from is the same
		if (packet.getAddress().equals(serverAddress)) {
				
			byte[] data = packet.getData();
			
			if (data[0] == 0 && data[1] == PacketType.DATA.getOpcode()) {
				if (data[2] == expectedBlockNumber[0] && data[3] == expectedBlockNumber[1]) {
					
					
					System.out.println("FILE DATA PORTION LENGTH IS: " + data.length);
					
					// Check to make sure the packet is not larger than expected
					if (data.length <= 516) {
						// The packet is valid
						return;
						
					} else {
						throw new IllegalTftpOperationException("DATA packet contains too much data. Maximum is 512 bytes");
					}

					
				} else {
					throw new IllegalTftpOperationException("DATA packet with invalid block number. "
							+ "Expected " + expectedBlockNumber[0] + expectedBlockNumber[1] 
									+ " but received " + data[2] + data[3]);
				}
				
			} else {
				throw new IllegalTftpOperationException("Invalid DATA packet opcode. Must be 0" + PacketType.DATA.getOpcode());
			}
				
				
		} else {
			throw new UnknownTransferIdException("DATA packet received from invalid address");
		}
		
	}
	
	
	private void validateAckPacket(DatagramPacket packet, byte[] expectedBlockNumber) 
			throws IllegalTftpOperationException, UnknownTransferIdException {
		
		// Make sure the address the packet is coming from is the same
		if (packet.getAddress().equals(serverAddress)) {
				
			byte[] data = packet.getData();
			
			if (data[0] == 0 && data[1] == PacketType.ACK.getOpcode()) {
				if (data[2] == expectedBlockNumber[0] && data[3] == expectedBlockNumber[1]) {
					
					if (data[data.length - 1] == 0) {
						// The packet is valid
						return;
						
					} else {
						throw new IllegalTftpOperationException("ACK packet is too long. Should be 4 bytes");
					}
					
				} else {
					throw new IllegalTftpOperationException("Invalid block number. "
							+ "Expected " + expectedBlockNumber[0] + expectedBlockNumber[1] 
									+ " but received " + data[2] + data[3]);
				}
				
			} else {
				throw new IllegalTftpOperationException("Invalid ACK packet opcode. Must be 0" + PacketType.ACK.getOpcode());
			}
				
			
		} else {
			throw new UnknownTransferIdException("ACK packet received from invalid address");
		}
	
	}
	
	private DatagramPacket formACKPacket(InetAddress address, int port, byte[] blockNumber) throws UnknownHostException {
		byte[] ackData = new byte[4];
		ackData[0] = 0;
		ackData[1] = Client.PacketType.ACK.getOpcode();
		ackData[2] = blockNumber[0];
		ackData[3] = blockNumber[1];
		
		return new DatagramPacket(ackData, ackData.length, address, port);
		
	}
	
	private DatagramPacket formErrorPacket(InetAddress address, int port, Client.ErrorType errorType, String errorMessage) {
		byte[] errorPacket = new byte[5 + errorMessage.getBytes().length];
		errorPacket[0] = 0;
		errorPacket[1] = PacketType.ERROR.getOpcode();
		errorPacket[2] = 0;
		errorPacket[3] = errorType.getErrorCode();
		
		System.arraycopy(errorMessage.getBytes(), 0, errorPacket, 4, errorMessage.getBytes().length);
		
		errorPacket[errorPacket.length - 1] = 0;
		
		return new DatagramPacket(errorPacket, errorPacket.length, address, port);
	}
	
	
	private byte[] getFileDataFromDataPacket(DatagramPacket packet) {
		byte[] data = Arrays.copyOfRange(packet.getData(), 4, packet.getData().length);
		
		// Remove trailing 0 bytes (if there are any)
	    int i = data.length - 1;
		while (i >= 0 && data[i] == 0) {
			i--;
	    }
		data = Arrays.copyOf(data, i + 1);
		
		return data;
	}
	
	private void removeTrailingZeroBytesFromDataPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		
		// Make sure it's a DATA packet
		if (data[0] == 0 && data[1] == PacketType.DATA.getOpcode()) {
			// Remove trailing 0 bytes from data
		    int i = data.length - 1;
			while (i >= 0 && data[i] == 0) {
				i--;
		    }
			packet.setData(Arrays.copyOf(data, i + 1));
			
		}
	}
	
	
	private ErrorType getErrorType(DatagramPacket packet) {
		byte[] data = packet.getData();
		
		// Check if the packet is an error packet to begin with
		if (data[0] == 0 && data[1] == PacketType.ERROR.getOpcode()) {
			
			// Check if the error is an illegal TFTP operation
			if (data[2] == 0 && data[3] == ErrorType.ILLEGAL_TFTP_OPERATION.getErrorCode()) {
				return ErrorType.ILLEGAL_TFTP_OPERATION;
				
			} else if (data[2] == 0 && data[3] == ErrorType.UNKNOWN_TRANSFER_ID.getErrorCode()) {
				return ErrorType.UNKNOWN_TRANSFER_ID;
			}
			
		}
		
		return null;
	}
	
	private void sendFile(DatagramPacket packet, String fileName) {
		int connectionPort = packet.getPort();
		
		printPacketInfo(packet, PacketAction.RECEIVE);
		
	    // Send data to be written to server
    	try {
        	BufferedInputStream in = new BufferedInputStream(new FileInputStream("src/client/files/" + fileName));
	        
	        byte[] dataFromFile = new byte[512];
	        int n;
	        
	        byte[] blockNumber = {0, 0};
	        
    	    // Validate the first ACK packet received
    	    try {
    	    	validateAckPacket(packet, blockNumber);
    	    	
    	    } catch (IllegalTftpOperationException illegalOperationException) {
    	    	System.out.println("IllegalTftpOperationException: " + illegalOperationException.getMessage());
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(packet.getAddress(), 
            			packet.getPort(), 
            			ErrorType.ILLEGAL_TFTP_OPERATION, 
            			illegalOperationException.getMessage());
            	
    		    // Process the error packet to send
    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
    			

    		    try {
    		    	// Send the error packet
    		    	sendReceiveSocket.send(sendErrorPacket);
    		    } catch (IOException ioException) {
    		       ioException.printStackTrace();
    		       Thread.currentThread().interrupt();
    		       System.exit(1);
    		    }
    	    	

    	    	return;
    	    	
    	    	
    	    } catch(UnknownTransferIdException unknownTransferIdException) {
    	    	System.out.println("UnknownTransferIdException: " + unknownTransferIdException.getMessage());
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(packet.getAddress(), 
            			packet.getPort(), 
            			ErrorType.UNKNOWN_TRANSFER_ID, 
            			unknownTransferIdException.getMessage());
            	
    		    // Process the error packet to send
    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
    			

    		    try {
    		    	// Send the error packet
    		    	sendReceiveSocket.send(sendErrorPacket);
    		    } catch (IOException ioException) {
    		       ioException.printStackTrace();
    		       Thread.currentThread().interrupt();
    		       System.exit(1);
    		    }
    	    	
    	    }
    	    
            // First ACK packet has been validated so we increment the block number now to 01
    	    blockNumber = incrementBlockNumber(blockNumber);
	        
	        // Read the file in 512 byte chunks
	        while ((n = in.read(dataFromFile)) != -1) {
	        	     	
	        	DatagramPacket sendDataPacket = formDataPacket(packet.getAddress(), packet.getPort(), 
	        			dataFromFile, n, 
	        			blockNumber);
	        
	        	
	        	removeTrailingZeroBytesFromDataPacket(sendDataPacket);
	        	
	        	// Process the packet to send
	    	    printPacketInfo(sendDataPacket, PacketAction.SEND);
	    	    

	    	    // Send the datagram packet to the server via the send/receive socket. 
	    	    try {
	    	    	sendReceiveSocket.send(sendDataPacket);
	    	    } catch (IOException e) {
	    	        e.printStackTrace();
	    	        System.exit(1);
	    	    }
	    	
	    	    System.out.println("Client: Packet sent.\n");
	    	    
	    	    // Wait to receive an ACK
	    	    
	    	    
	    	    do {
		    	    // Construct a DatagramPacket for receiving the ACK packet
		    	    byte dataForAck[] = new byte[517];
		    	    receivePacket = new DatagramPacket(dataForAck, dataForAck.length);
		    	
		    	    try {
		    	        // Block until a datagram is received via the send/receive socket.  
		    	        sendReceiveSocket.receive(receivePacket);
		    	    } catch(IOException e) {
		    	        e.printStackTrace();
		    	        System.exit(1);
		    	    }
		    	    
		    	    removeTrailingZeroBytesFromDataPacket(receivePacket);
		    	
		    	    // Process the packet received
		    	    printPacketInfo(receivePacket, PacketAction.RECEIVE);
		    	    
		    	    
		    	    System.out.println("PACKET PORT: " + receivePacket.getPort());
		    	    
		    	    if (receivePacket.getPort() != connectionPort) {
		    	    	System.out.println("Received WRQ ACK packet from another port");
		    	    	System.out.println("Sending error packet...");
		    	    	
		    	    	// Form the error packet
		            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
		            			receivePacket.getPort(), 
		            			ErrorType.UNKNOWN_TRANSFER_ID, 
		            			"client already connected");
		            	
		    		    // Process the error packet to send
		    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
		    			

		    		    try {
		    		    	// Send the error packet to the client
		    		    	sendReceiveSocket.send(sendErrorPacket);
		    		    } catch (IOException ioException) {
		    		       ioException.printStackTrace();
		    		       System.exit(1);
		    		    }
		    	    } else {
		    	    	break;
		    	    }
	    	    } while(receivePacket.getPort() != connectionPort);
	    	    
	    	    
	    	    try {
	    	    	PacketType packetType = getPacketType(receivePacket);
	    	    	
	    	    	if (packetType.equals(PacketType.ACK)) {
	    	    	    // Validate the ACK packet
	    	    	    try {
	    	    	    	validateAckPacket(receivePacket, blockNumber);
	    	    	    	
	    	    	    } catch (IllegalTftpOperationException illegalOperationException) {
	    	    	    	System.out.println("IllegalTftpOperationException: " + illegalOperationException.getMessage());
	    	    	    	System.out.println("Sending error packet...");
	    	    	    	
	    	    	    	// Form the error packet
	    	            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
	    	            			receivePacket.getPort(), 
	    	            			ErrorType.ILLEGAL_TFTP_OPERATION, 
	    	            			illegalOperationException.getMessage());
	    	            	
	    	    		    // Process the error packet to send
	    	    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
	    	    			

	    	    		    try {
	    	    		    	// Send the error packet to the client
	    	    		    	sendReceiveSocket.send(sendErrorPacket);
	    	    		    } catch (IOException ioException) {
	    	    		       ioException.printStackTrace();
	    	    		       System.exit(1);
	    	    		    }
	    	    		    
	    					System.out.println("\n*** Ending session...***");
	    					//System.exit(1);
	    					return;
	    	    	    	
	    	    	    	
	    	    	    } catch(UnknownTransferIdException unknownTransferIdException) {
	    	    	    	System.out.println("UnknownTransferIdException: " + unknownTransferIdException.getMessage());
	    	    	    	System.out.println("Sending error packet...");
	    	    	    	
	    	    	    	// Form the error packet
	    	            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
	    	            			receivePacket.getPort(), 
	    	            			ErrorType.UNKNOWN_TRANSFER_ID, 
	    	            			unknownTransferIdException.getMessage());
	    	            	
	    	    		    // Process the error packet to send
	    	    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
	    	    			

	    	    		    try {
	    	    		    	// Send the error packet to the client
	    	    		    	sendReceiveSocket.send(sendErrorPacket);
	    	    		    } catch (IOException ioException) {
	    	    		       ioException.printStackTrace();
	    	    		       System.exit(1);
	    	    		    }
	    	    	    	
	    	    	    }
	    	    	    
	    	    	        
	                    // ACK packet has been validated so we increment the block number now
	    	    	    blockNumber = incrementBlockNumber(blockNumber);
	    	    	    
	    	    	} else if (packetType.equals(PacketType.ERROR)) {
			    		ErrorType errorType = getErrorType(receivePacket);
			    		
			    		if (errorType != null) {
			    			if (errorType.equals(ErrorType.ILLEGAL_TFTP_OPERATION)) {
			    				System.out.println("\n*** Received ILLEGAL_TFTP_OPERATION error packet...Ending session...***");
			    				//System.exit(1);
			    				return;
			    			}
			    		}
	    	    	}
	    	    	
	    	    } catch (InvalidPacketTypeException e) {
			    	System.out.println("\n*** Received Packet with invalid ACK opCode ***");
			    	
			    	// send error to server for invalid ACK opcode
	    	    	System.out.println("Sending error packet...");
	    	    	
	    	    	// Form the error packet
	            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
	            			receivePacket.getPort(), 
	            			ErrorType.ILLEGAL_TFTP_OPERATION, 
	            			"Invalid opCode for expected ACK packet");
	            	
	    		    // Process the error packet to send
	    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
	    			

	    		    try {
	    		    	// Send the error packet to the client
	    		    	sendReceiveSocket.send(sendErrorPacket);
	    		    } catch (IOException ioException) {
	    		       ioException.printStackTrace();
	    		       System.exit(1);
	    		    }
	    		    
					System.out.println("\n*** Ending session...***");
					//System.exit(1);
					return;
	    	    }

	    	    

	        	
	        }
	        
	        in.close();
    	} catch (FileNotFoundException e) {
    		System.out.println("No such file " + fileName);
    	} catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
    	}
    	
	}
	
	
	private void receiveFile(DatagramPacket receivePacket, String fileName) {
		
		byte[] blockNumber = {0,1};
		int dataLength = 512;
	
	    
	    do {
		    removeTrailingZeroBytesFromDataPacket(receivePacket);
		
            // Process the packet received
		    printPacketInfo(receivePacket, PacketAction.RECEIVE);
		    
		    
		    try {
		    	PacketType packetType = getPacketType(receivePacket);
		    	
		    	if (packetType.equals(PacketType.DATA)) {
				    // Validate the DATA packet
				    try {
				    	validateDataPacket(receivePacket, blockNumber);
				    	
		    	    } catch (IllegalTftpOperationException illegalOperationException) {
		    	    	System.out.println("IllegalTftpOperationException: " + illegalOperationException.getMessage());
		    	    	System.out.println("Sending error packet...");
		    	    	
		    	    	// Form the error packet
		            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
		            			receivePacket.getPort(), 
		            			ErrorType.ILLEGAL_TFTP_OPERATION, 
		            			illegalOperationException.getMessage());
		            	
		    		    // Process the error packet to send
		    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
		    			

		    		    try {
		    		    	// Send the error packet to the client
		    		    	sendReceiveSocket.send(sendErrorPacket);
		    		    } catch (IOException ioException) {
		    		       ioException.printStackTrace();
		    		       Thread.currentThread().interrupt();
		    		       System.exit(1);
		    		    }
		    	    	
						System.out.println("\n*** Ending session...***");
						//System.exit(1);
						return;
		    	    	
		    	    } catch(UnknownTransferIdException unknownTransferIdException) {
		    	    	System.out.println("UnknownTransferIdException: " + unknownTransferIdException.getMessage());
		    	    	System.out.println("Sending error packet...");
		    	    	
		    	    	// Form the error packet
		            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
		            			receivePacket.getPort(), 
		            			ErrorType.UNKNOWN_TRANSFER_ID, 
		            			unknownTransferIdException.getMessage());
		            	
		    		    // Process the error packet to send
		    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
		    			

		    		    try {
		    		    	// Send the error packet to the client
		    		    	sendReceiveSocket.send(sendErrorPacket);
		    		    } catch (IOException ioException) {
		    		       ioException.printStackTrace();
		    		       System.exit(1);
		    		    }
		    	    	
		    	    }
				    
		    	} else if (packetType.equals(PacketType.ERROR)) {
		    		
		    		ErrorType errorType = getErrorType(receivePacket);
		    		
		    		if (errorType != null) {
		    			if (errorType.equals(ErrorType.ILLEGAL_TFTP_OPERATION)) {
		    				System.out.println("\n*** Received ILLEGAL_TFTP_OPERATION error packet...Ending session...***");
		    				//System.exit(1);
		    				return;
		    			}
		    		}
		    	}
		    	
		    } catch (InvalidPacketTypeException e) {
		    	System.out.println("\n*** Received Packet with invalid DATA opCode ***");
		    	// send error to server for invalid DATA opcode
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
            			receivePacket.getPort(), 
            			ErrorType.ILLEGAL_TFTP_OPERATION, 
            			"Invalid opCode for expected DATA packet");
            	
    		    // Process the error packet to send
    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
    			

    		    try {
    		    	// Send the error packet to the client
    		    	sendReceiveSocket.send(sendErrorPacket);
    		    } catch (IOException ioException) {
    		       ioException.printStackTrace();
    		       System.exit(1);
    		    }
    		    
				System.out.println("\n*** Ending session...***");
				//System.exit(1);
				return;
		    }
		    
		   
		    
		    
			// Construct an ACK datagram packet
		    try {
		    	sendPacket = formACKPacket(receivePacket.getAddress(), receivePacket.getPort(), blockNumber);
		    } catch (UnknownHostException e) {
		        e.printStackTrace();
		        System.exit(1);
		    }
		    
		    // Process the packet to send
		    printPacketInfo(sendPacket, PacketAction.SEND);
			

		    try {
		    	sendReceiveSocket.send(sendPacket);
		    } catch (IOException e) {
		       e.printStackTrace();
		       System.exit(1);
		    }
			
		    // Increment the block number
		    blockNumber = incrementBlockNumber(blockNumber);
		    
		    // Note that DATA packets should be 516 bytes maximum 
		    // but our buffer is larger for error checking purposes
	        byte dataFromServer[] = new byte[517];
	        
			receivePacket = new DatagramPacket(dataFromServer, dataFromServer.length);
		    System.out.println("Client: waiting for Packet.\n");

		    // Block until a datagram packet is received from the send/receive socket
		    try {        
		        System.out.println("Waiting...");
		        sendReceiveSocket.receive(receivePacket);
		    } catch (IOException e) {
		        System.out.print("IO Exception: likely:");
		        System.out.println("Receive Socket Timed Out.\n" + e);
		        e.printStackTrace();
		        System.exit(1);
		    }
		    
            // Process the packet received
		    printPacketInfo(receivePacket, PacketAction.RECEIVE);
		    
		    try {
			    PacketType packetType = getPacketType(receivePacket);
			    
			    if (packetType.equals(PacketType.ERROR)) {
			    	
		    		ErrorType errorType = getErrorType(receivePacket);
		    		
		    		if (errorType != null) {
		    			if (errorType.equals(ErrorType.ILLEGAL_TFTP_OPERATION)) {
		    				System.out.println("\n*** Received ILLEGAL_TFTP_OPERATION error packet...Ending session...***");
		    				//System.exit(1);
		    				return;
		    			}
		    		}
			    }
			    
		    } catch (InvalidPacketTypeException e) {
		    	System.out.println("\n*** Received Packet with invalid DATA opCode ***");
		    	// send error to server for invalid DATA opcode
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
            			receivePacket.getPort(), 
            			ErrorType.ILLEGAL_TFTP_OPERATION, 
            			"Invalid opCode for expected DATA packet");
            	
    		    // Process the error packet to send
    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
    			

    		    try {
    		    	// Send the error packet to the client
    		    	sendReceiveSocket.send(sendErrorPacket);
    		    } catch (IOException ioException) {
    		       ioException.printStackTrace();
    		       System.exit(1);
    		    }
    		    
				System.out.println("\n*** Ending session...***");
				//System.exit(1);
				return;
		    }
		    
		    
		    // Update the length of the file data
		    dataLength = getFileDataFromDataPacket(receivePacket).length;
		    
	    	
	    } while(dataLength == 512);

	    
	    System.out.println("\n Client (" + Thread.currentThread() + "): Finished receiving file");
	    
	}
	
	
	public void sendAndReceive(PacketType type, String mode, String fileName) {
		
        byte fileNameInBytes[] = fileName.getBytes();
		byte modeInBytes[] = mode.getBytes();
		
		// The length of msg will always be the length of the fileNameInBytes 
		// array plus the length of the modeInBytes array plus 4
		// (first 2 bytes identify the RequestType, last byte is a 0, 
		// and another 0 byte acts as a delimiter between the file name and mode)
		byte msg[] = new byte[4 + fileNameInBytes.length + modeInBytes.length];
		int msgIndex = 0;
		
		// First byte is a 0 for both READ and WRITE request types
		msg[msgIndex++] = 0;
		
		if (type.equals(PacketType.READ)) {
			// Second byte is a 1 for READ request
			msg[msgIndex++] = 1;
			
		} else if (type.equals(PacketType.WRITE)) {
			// Second byte is a 2 for WRITE request
			msg[msgIndex++] = 2;
		} else {
			// Second byte is a 7 for INVALID request
			msg[msgIndex++] = 7;
		}
		
		// Add the file name (in byte format) to the message
		for (int i = 0; i < fileNameInBytes.length; i++) {
			msg[msgIndex++] = fileNameInBytes[i];	
		}
		
		// Add a 0 byte
		msg[msgIndex++] = 0;
		
		// Add the mode (in byte format) to the message
		for (int i = 0; i < modeInBytes.length; i++) {
			msg[msgIndex++] = modeInBytes[i];
		}
		
		// Add a 0 byte as the final byte of the message
		msg[msgIndex++] = 0;
		
		

	    try {
	        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), (this.mode == Mode.TEST ? 68 : 69));
	    } catch (UnknownHostException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }
		
	    
	    printPacketInfo(sendPacket, PacketAction.SEND);
	    

	    // Send the datagram packet to the server via the send/receive socket. 
	    try {
	    	sendReceiveSocket.send(sendPacket);
	    } catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }
	
	    System.out.println("Client: Packet sent.\n");
	
	    // Construct a DatagramPacket for receiving packets
	    byte data[] = new byte[517];
	    receivePacket = new DatagramPacket(data, data.length);
	
	    try {
	        // Block until a datagram is received via the send/receive socket.  
	        sendReceiveSocket.receive(receivePacket);
	    } catch(IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }

	       
	    
	    try {
	    	PacketType packetType = getPacketType(receivePacket);
	    	
	    	if (packetType.equals(PacketType.ACK)) {
		    	System.out.println("Sending file....");
		    	sendFile(receivePacket, fileName);
		    	
	    	} else if (packetType.equals(PacketType.DATA)) {
		    	System.out.println("Receiving file....");
		    	receiveFile(receivePacket, fileName);
		    	
	    	} else if (packetType.equals(PacketType.ERROR)) {
		    	printPacketInfo(receivePacket, PacketAction.RECEIVE);
		    	
	    		ErrorType errorType = getErrorType(receivePacket);
	    		
	    		if (errorType != null) {
	    			if (errorType.equals(ErrorType.ILLEGAL_TFTP_OPERATION)) {
	    				System.out.println("\n*** Received ILLEGAL_TFTP_OPERATION error packet...Ending session...***");
	    				//System.exit(1);
	    				return;
	    			}
	    		}
	    	}
	    	
	    } catch (InvalidPacketTypeException e) {
	    	
	    	if (type.equals(PacketType.READ)) {
	    		// send error to server for invalid opCode for DATA packet
		    	System.out.println("*** Received Packet with invalid DATA opCode ***");
		    	// send error to server for invalid DATA opcode
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
            			receivePacket.getPort(), 
            			ErrorType.ILLEGAL_TFTP_OPERATION, 
            			"Invalid opCode for expected DATA packet");
            	
    		    // Process the error packet to send
    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
    			

    		    try {
    		    	// Send the error packet to the client
    		    	sendReceiveSocket.send(sendErrorPacket);
    		    } catch (IOException ioException) {
    		       ioException.printStackTrace();
    		       System.exit(1);
    		    }
    		    
				System.out.println("\n*** Ending session...***");
				//System.exit(1);
				return;
    		    
	    	} else if (type.equals(PacketType.WRITE)) {
		    	System.out.println("*** Received Packet with invalid ACK opCode ***");
	    		// send error to server for invalid opCode for ACK packet
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
            			receivePacket.getPort(), 
            			ErrorType.ILLEGAL_TFTP_OPERATION, 
            			"Invalid opCode for expected ACK packet");
            	
    		    // Process the error packet to send
    		    printPacketInfo(sendErrorPacket, PacketAction.SEND);
    			

    		    try {
    		    	// Send the error packet to the client
    		    	sendReceiveSocket.send(sendErrorPacket);
    		    } catch (IOException ioException) {
    		       ioException.printStackTrace();
    		       System.exit(1);
    		    }
    		    
				System.out.println("\n*** Ending session...***");
				//System.exit(1);
				return;
	    	}
	    }
	   

		
		
	}
	
	
	public static void main(String args[]) {
		
		Client newClient = new Client(Mode.TEST);
		
		
		while(true) {
			System.out.println("------------------------------------------------------");
			System.out.println("Select from the following options by entering a number (i.e. 1) or enter 'q' to exit: \n");
			System.out.println("1. Read file");
			System.out.println("2. Write file");
			
			char input = Keyboard.getCharacter();
			
			if (input == 'q') {
				System.out.print("Exiting...");
				break;
				
			} else if (input == '1') { // READ request
				String fileName = "";
				String mode = "";
				
				System.out.print("Enter the name of the file (if * typed then testFileFromServer.txt will be used): ");
				fileName = Keyboard.getString();
				if (fileName.trim().equals("*")) {
					fileName = "testFileFromServer.txt";
				}
				
				System.out.println("Enter the mode (if * typed then netascii will be used): ");
				mode = Keyboard.getString();
				
				if (mode.trim().equals("*")) {
					mode = "netascii";
				}
				System.out.println("\nSending READ request...");
				
				newClient.sendAndReceive(PacketType.READ, mode, fileName);
				
				System.out.println("------------------------------------------------------");
				
				
			} else if (input == '2') { // WRITE request
				String fileName = "";
				String mode = "";
				
				System.out.print("Enter the name of the file (if * typed then testFileFromClient.txt will be used): ");
				fileName = Keyboard.getString();
				if (fileName.trim().equals("*")) {
					fileName = "testFileFromClient.txt";
				}
				
				System.out.println("Enter the mode (if * typed then netascii will be used): ");
				mode = Keyboard.getString();
				
				if (mode.trim().equals("*")) {
					mode = "netascii";
				}
				System.out.println("\nSending WRITE request...");
				
				newClient.sendAndReceive(PacketType.WRITE, mode, fileName);
				
			} else {
				System.out.println("Invalid option");
			}
		}
		
		if (newClient.sendReceiveSocket != null) {
			newClient.sendReceiveSocket.close();
		}
		
	}

}
