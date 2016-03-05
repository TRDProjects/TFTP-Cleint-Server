package client;

import host.InvalidPacketTypeException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import client.IllegalTftpOperationException;
import client.UnknownTransferIdException;
import util.Keyboard;

public class Client {
	
	public static final int PACKET_RETRANSMISSION_TIMEOUT = 1000;
	
	
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
	
	  public void sendPacket(DatagramSocket socket, DatagramPacket packet) {
	      printPacketInfo(packet, PacketAction.SEND);

	      try {
	    	  socket.send(packet);
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	      
	      System.out.println("Client: packet sent");
	  }
		  
	  public DatagramPacket receivePacket(DatagramSocket socket, int bufferLength) throws SocketTimeoutException {
		 
		  // Construct a DatagramPacket for receiving packets
	      byte dataBuffer[] = new byte[bufferLength];
	      DatagramPacket receivedPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
	      System.out.println("Client: waiting for packet.\n");
	      
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
	      
	      // If the packet is a DATA packet, remove the trailing 0 bytes
	      removeTrailingZeroBytesFromDataPacket(receivedPacket);
	      
	      // Process the packet received
	      printPacketInfo(receivedPacket, PacketAction.RECEIVE);
	      
	      return receivedPacket;
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
	
	
	private void validateDataPacket(DatagramPacket packet, byte[] expectedBlockNumber, int expectedPort)
			throws IllegalTftpOperationException, UnknownTransferIdException, PacketAlreadyReceivedException {
		
		removeTrailingZeroBytesFromDataPacket(packet);
			
		// Make sure the address and port the packet is coming from is the same
		if (packet.getAddress().equals(serverAddress) && packet.getPort() == expectedPort) {
				
			byte[] data = packet.getData();
			
			if (data[0] == 0 && data[1] == PacketType.DATA.getOpcode()) {
				
				// Check to make sure the packet is not larger than expected
				if (data.length > 516) {
					throw new IllegalTftpOperationException("DATA packet contains too much data. Maximum is 512 bytes");
				}
				
				// Check to make sure the block number is valid 
				
				short packetBlockNumberShort   = getBlockNumberAsShort(new byte[]{ data[2], data[3]});
				short expectedBlockNumberShort = getBlockNumberAsShort(expectedBlockNumber);
				
				
				if (packetBlockNumberShort == expectedBlockNumberShort) {
					// The DATA packet is valid and it is for the block number we are expecting
					return;
					
				} else if (packetBlockNumberShort < expectedBlockNumberShort) {
					// The DATA packet was already received beforehand
					throw new PacketAlreadyReceivedException("DATA packet with block number " + data[2] + data[3] + 
							" was already received");
				} else {
					throw new IllegalTftpOperationException("Invalid block number. "
							+ "Expected " + expectedBlockNumber[0] + expectedBlockNumber[1] 
									+ " but received " + data[2] + data[3]);
				}

				
			} else {
				throw new IllegalTftpOperationException("Invalid DATA packet opcode. Must be 0" + PacketType.DATA.getOpcode());
			}
				
				
		} else {
			if (packet.getPort() != expectedPort) {
				throw new UnknownTransferIdException("Unknown port: " + packet.getPort());
			} else {
				throw new UnknownTransferIdException("DATA packet received from invalid address: " + packet.getAddress());
			}
		}
		
	}
	
	
	private void validateAckPacket(DatagramPacket packet, byte[] expectedBlockNumber, int expectedPort) 
			throws IllegalTftpOperationException, UnknownTransferIdException, PacketAlreadyReceivedException {
		
		// Make sure the address and port the packet is coming from is the same
		if (packet.getAddress().equals(serverAddress) && packet.getPort() == expectedPort) {
				
			byte[] data = packet.getData();
			
			if (data[0] == 0 && data[1] == PacketType.ACK.getOpcode()) {
				
				// Check to make sure the ACK packet is exactly 4 bytes
				for (int i = data.length - 1; i > 4; i--) {
					if (data[i] != 0) {
						throw new IllegalTftpOperationException("ACK packet is too long. Should be 4 bytes");
					}
				}
				
				// Check to make sure the block number is valid 
				
				short packetBlockNumberShort   = getBlockNumberAsShort(new byte[]{ data[2], data[3]});
				short expectedBlockNumberShort = getBlockNumberAsShort(expectedBlockNumber);
				
				if (packetBlockNumberShort == expectedBlockNumberShort) {
					// The ACK packet is valid and it is for the block number we are expecting
					return;
					
				} else if (packetBlockNumberShort < expectedBlockNumberShort) {
					// The ACK was already received beforehand
					throw new PacketAlreadyReceivedException("ACK packet with block number " + data[2] + data[3] + 
							" was already received");
				} else {
					throw new IllegalTftpOperationException("Invalid block number. "
							+ "Expected " + expectedBlockNumber[0] + expectedBlockNumber[1] 
									+ " but received " + data[2] + data[3]);
				}
				
			} else {
				throw new IllegalTftpOperationException("Invalid ACK packet opcode. Must be 0" + PacketType.ACK.getOpcode());
			}
				
			
		} else {
			if (packet.getPort() != expectedPort) {
				throw new UnknownTransferIdException("Unknown port: " + packet.getPort());
			} else {
				throw new UnknownTransferIdException("ACK packet received from invalid address: " + packet.getAddress());
			}
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
		
		try {
			// Make sure it's a DATA packet
			if (getPacketType(packet).equals(PacketType.DATA)) {
				byte[] data = packet.getData();
				
				// Remove trailing 0 bytes from data
			    int i = data.length - 1;
				while (i >= 0 && data[i] == 0) {
					i--;
			    }
				packet.setData(Arrays.copyOf(data, i + 1));
				
			}
		} catch (InvalidPacketTypeException e) {
			
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
	
	
	private short getBlockNumberAsShort(byte[] blockNumber) {
		return ByteBuffer.wrap(blockNumber).getShort();
	}
	
	
	private void sendFile(DatagramPacket packet, String fileName) {
		int connectionPort = packet.getPort();
		
		// Since the client will be sending the DATA packets, we set a timeout on the socket
		try {
		    sendReceiveSocket.setSoTimeout(PACKET_RETRANSMISSION_TIMEOUT);
		} catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
		}
		
	    // Send data to be written to server
    	try {
        	BufferedInputStream in = new BufferedInputStream(new FileInputStream("src/client/files/" + fileName));
	        
	        byte[] dataFromFile = new byte[512];
	        int n;
	        byte[] blockNumber = {0, 0};
	        
    	    // Validate the first ACK packet received
    	    try {
    	    	validateAckPacket(packet, blockNumber, connectionPort);
    	    	

    	    } catch (IllegalTftpOperationException illegalOperationException) {
    	    	System.out.println("IllegalTftpOperationException: " + illegalOperationException.getMessage());
    	    	System.out.println("Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(packet.getAddress(), 
            			packet.getPort(), 
            			ErrorType.ILLEGAL_TFTP_OPERATION, 
            			illegalOperationException.getMessage());
            	
            	
            	// Send the error packet
            	sendPacket(sendReceiveSocket, sendErrorPacket);
            	
            	in.close();
            	
    	    	return;
    	    	
    	    	
    	    } catch(UnknownTransferIdException unknownTransferIdException) {
    	    	System.out.println("\n*** UnknownTransferId: " + unknownTransferIdException.getMessage());
    	    	System.out.println("*** Sending error packet...");
    	    	
    	    	// Form the error packet
            	DatagramPacket sendErrorPacket = formErrorPacket(packet.getAddress(), 
            			packet.getPort(), 
            			ErrorType.UNKNOWN_TRANSFER_ID, 
            			unknownTransferIdException.getMessage());
            	
            	// Send the error packet
            	sendPacket(sendReceiveSocket, sendErrorPacket);
    	    	
            	
    	    } catch (PacketAlreadyReceivedException alreadyReceivedException) {
    	    	// This is the first ACK so it can't have already been received before. Do nothing...
    	    }
    	    
            // First ACK packet has been validated so we increment the block number now to 01
    	    blockNumber = incrementBlockNumber(blockNumber);
	        
    	    
	        // Read the file in 512 byte chunks
	        while ((n = in.read(dataFromFile)) != -1) {
	        	     	
	        	DatagramPacket sendDataPacket = formDataPacket(packet.getAddress(), packet.getPort(), 
	        			dataFromFile, n, 
	        			blockNumber);
	        
	        	
	        	removeTrailingZeroBytesFromDataPacket(sendDataPacket);
	        	
            	// Send the DATA packet
            	sendPacket(sendReceiveSocket, sendDataPacket);

	    
	    	    // Wait to receive an ACK 	    

	            do {
	        	   
	            	try {
	 		    	   // Attempt to receive a packet
	 		    	   receivePacket = receivePacket(sendReceiveSocket, 517);
	 		    	   
	            	} catch (SocketTimeoutException firstTimeoutException) {
	            		// Resend the DATA packet
	            		System.out.println("\n*** Socket Timout...Resending DATA packet ***");
	            		sendPacket(sendReceiveSocket, sendDataPacket);
	            		
	            		try {
		            		// Attempt to receive a packet for the second time
		            		receivePacket = receivePacket(sendReceiveSocket, 517);
		            		
	            		} catch (SocketTimeoutException secondTimoutException) {
	            			System.out.println("\n ****** Server Unreachable...Ending This Session ******");
	            			in.close();
	            			return;
	            		}
	            	}
		    	    		    
		    	    
		    	    try {
		    	    	PacketType packetType = getPacketType(receivePacket);
		    	    	
		    	    	if (packetType.equals(PacketType.ACK)) {
		    	    	    // Validate the ACK packet
		    	    	    try {
		    	    	    	validateAckPacket(receivePacket, blockNumber, connectionPort);
		    	    	    	
			                    // ACK packet has been validated so we increment the block number now
			    	    	    blockNumber = incrementBlockNumber(blockNumber);
		    	    	    	
		    	    	    } catch (IllegalTftpOperationException illegalOperationException) {
		    	    	    	System.out.println("IllegalTftpOperationException: " + illegalOperationException.getMessage());
		    	    	    	System.out.println("Sending error packet...");
		    	    	    	
		    	    	    	// Form the error packet
		    	            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
		    	            			receivePacket.getPort(), 
		    	            			ErrorType.ILLEGAL_TFTP_OPERATION, 
		    	            			illegalOperationException.getMessage());
		    	            	
		    	            	// Send the error packet
		    	            	sendPacket(sendReceiveSocket, sendErrorPacket);
		    	    		    
		    					System.out.println("\n*** Ending session...***");
		    					
		    	            	in.close();
		    	            	
		    					return;
		    	    	    	
		    	    	    	
		    	    	    } catch(UnknownTransferIdException unknownTransferIdException) {
		    	    	    	System.out.println("\n*** UnknownTransferId: " + unknownTransferIdException.getMessage());
		    	    	    	System.out.println("*** Sending error packet...");
		    	    	    	
		    	    	    	// Form the error packet
		    	            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
		    	            			receivePacket.getPort(), 
		    	            			ErrorType.UNKNOWN_TRANSFER_ID, 
		    	            			unknownTransferIdException.getMessage());
		    	            	
		    	            	// Send the error packet
		    	            	sendPacket(sendReceiveSocket, sendErrorPacket);
		    	    	    	
		    	    	    } catch (PacketAlreadyReceivedException alreadyReceivedException) {
		    	    	    	// Ignore this packet
		    	    	    	System.out.println("\n*** The received ACK packet was already received beforehand...Ignoring it... *** \n");
		    	    	    	continue;
		    	    	    }
		    	    	    
		    	    	        
		    	    	    
		    	    	} else if (packetType.equals(PacketType.ERROR)) {
				    		ErrorType errorType = getErrorType(receivePacket);
				    		
				    		if (errorType != null) {
				    			if (errorType.equals(ErrorType.ILLEGAL_TFTP_OPERATION)) {
				    				System.out.println("\n*** Received ILLEGAL_TFTP_OPERATION error packet...Ending session...***");
				    				
				                	in.close();
				                	
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
		            	
		            	// Send the error packet
		            	sendPacket(sendReceiveSocket, sendErrorPacket);
		    		    
						System.out.println("\n*** Ending session...***");
						
		            	in.close();
		            	
						return;
		    	    }
		    	    
	           } while (receivePacket.getPort() != connectionPort);

	    	    
	        	
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
		
		int connectionPort = receivePacket.getPort();
		
		byte[] blockNumber = {0,1};
		int dataLength = getFileDataFromDataPacket(receivePacket).length;
	
		boolean notDataPacket = false;
		
		// Since the server will be sending the DATA packets, no timeout on the socket is needed
		try {
			// Set the timeout to 0 (infinite)
		    sendReceiveSocket.setSoTimeout(0);
		} catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
		}

    	try {
    		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("src/client/files/" + fileName));
    			
    	    do {
    	    	
    	    	if (!(receivePacket.getData()[2] == 0 && receivePacket.getData()[3] == 0)) {
	    		    // Wait to receive a packet back from the server
	    		    try {
    		    		receivePacket = receivePacket(sendReceiveSocket, 517);
	    		    } catch (SocketTimeoutException e) {
	    		    	
	    		    }
    	    	}
	    		
    		    try {
    		    	PacketType packetType = getPacketType(receivePacket);
    		    	
    		    	if (packetType.equals(PacketType.DATA)) {
    				    // Validate the DATA packet
    				    try {
    				    	validateDataPacket(receivePacket, blockNumber, connectionPort);
    				    	
    		    		    // Update the length of the file data
    		    		    dataLength = getFileDataFromDataPacket(receivePacket).length;
    				    	
    					    // Write to file
    					    out.write(getFileDataFromDataPacket(receivePacket), 0, dataLength);
    		    		    
    		    		    
    		    			// Construct an ACK packet
    		    		    try {
    		    		    	sendPacket = formACKPacket(receivePacket.getAddress(), receivePacket.getPort(), blockNumber);
    		    		    } catch (UnknownHostException e) {
    		    		    	out.close();
    		    		        e.printStackTrace();
    		    		        System.exit(1);
    		    		    }
    		    		    
    		    		    // Send the ACK packet
    		    		    sendPacket(sendReceiveSocket, sendPacket);
    		    		    
    		    			
    		    		    // Increment the block number
    		    		    blockNumber = incrementBlockNumber(blockNumber);
    		    		    
    				    	
    		    	    } catch (IllegalTftpOperationException illegalOperationException) {
    		    	    	System.out.println("IllegalTftpOperationException: " + illegalOperationException.getMessage());
    		    	    	System.out.println("Sending error packet...");
    		    	    	
    		    	    	// Form the error packet
    		            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
    		            			receivePacket.getPort(), 
    		            			ErrorType.ILLEGAL_TFTP_OPERATION, 
    		            			illegalOperationException.getMessage());
    		            	
    		            	// Send the error packet
    		            	sendPacket(sendReceiveSocket, sendErrorPacket);
    		    	    	
    						System.out.println("\n*** Ending session...***");
    						
    						out.close();
    						
    						return;
    		    	    	
    		    	    } catch(UnknownTransferIdException unknownTransferIdException) {
    		    	    	System.out.println("\n*** UnknownTransferId: " + unknownTransferIdException.getMessage());
    		    	    	System.out.println("*** Sending error packet...");
    		    	    	
    		    	    	// Form the error packet
    		            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
    		            			receivePacket.getPort(), 
    		            			ErrorType.UNKNOWN_TRANSFER_ID, 
    		            			unknownTransferIdException.getMessage());
    		            	
    		            	// Send the error packet
    		            	sendPacket(sendReceiveSocket, sendErrorPacket);
    		    	    	
    		    	    } catch (PacketAlreadyReceivedException packetReceivedException) {
    		    	    	System.out.println("\n*** The received DATA packet was already received beforehand...Sending ACK packet for it... *** \n");
    		    	    	byte[] duplicateDataPacketBlockNumber = {receivePacket.getData()[2], receivePacket.getData()[3]};
    		    	    	
    		    			// Construct an ACK for the duplicate DATA packet
    		    		    try {
    		    		    	sendPacket = formACKPacket(receivePacket.getAddress(), receivePacket.getPort(), duplicateDataPacketBlockNumber);
    		    		    } catch (UnknownHostException e) {
    		    		    	out.close();
    		    		        e.printStackTrace();
    		    		        System.exit(1);
    		    		    }
    		    		    
    		    		    // Send the ACK packet for the duplicate DATA packet
    		    		    sendPacket(sendReceiveSocket, sendPacket);
    		    		    
    		    		    // Wait to receive a packet back from the server
    		    		    receivePacket = receivePacket(sendReceiveSocket, 517);
    		    		    
    		    		    // Move to the beginning of the loop
    		    		    continue;
    		    	    }
    				    
    		    	} else if (packetType.equals(PacketType.ERROR)) {
    		    		
    		    		ErrorType errorType = getErrorType(receivePacket);
    		    		
    		    		if (errorType != null) {
    		    			if (errorType.equals(ErrorType.ILLEGAL_TFTP_OPERATION)) {
    		    				System.out.println("\n*** Received ILLEGAL_TFTP_OPERATION error packet...Ending session...***");
    		    				out.close();
    		    				return;
    		    			}
    		    		}
    		    	}
    		    	
    		    } catch (InvalidPacketTypeException e) {
    		    	System.out.println("\n*** Received Packet with invalid opCode ***");
    		    	// send error to server for invalid opcode
        	    	System.out.println("Sending error packet...");
        	    	
        	    	// Form the error packet
                	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
                			receivePacket.getPort(), 
                			ErrorType.ILLEGAL_TFTP_OPERATION, 
                			"Invalid opCode");
                	
                	// Send the error packet
                	sendPacket(sendReceiveSocket, sendErrorPacket);
        		    
    				System.out.println("\n*** Ending session...***");
    				
    				out.close();
    				
    				return;
    		    }

    	    	
    	    } while(dataLength == 512 || notDataPacket);
    	    
    	    
		    // Write the data from the last DATA packet to file
		    out.write(getFileDataFromDataPacket(receivePacket), 0, dataLength);
    	    
    		// Construct the final ACK
    	    try {
    	    	sendPacket = formACKPacket(receivePacket.getAddress(), receivePacket.getPort(), blockNumber);
    	    } catch (UnknownHostException e) {
    	    	out.close();
    	        e.printStackTrace();
    	        System.exit(1);
    	    }
    	    
    	    // Send the final ACK packet
    	    sendPacket(sendReceiveSocket, sendPacket);
    	    
    	    
    	    out.close();
    		
    	} catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
    	}
	    	


	    System.out.println("\n Client (" + Thread.currentThread() + "): Finished receiving file");
	    
	    
	}
	
	
	public void sendAndReceive(PacketType type, String mode, String fileName) {
		
		// Set a timeout on the socket
		try {
		    sendReceiveSocket.setSoTimeout(PACKET_RETRANSMISSION_TIMEOUT);
		} catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
		}
		
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
		
	    
    	// Send the RRQ/WRQ packet
    	sendPacket(sendReceiveSocket, sendPacket);
	    
    	
    	// Wait to receive a packet from the server
    	try {
        	receivePacket = receivePacket(sendReceiveSocket, 517);
    	} catch (SocketTimeoutException firstSocketTimeoutException) {
    		System.out.println("\n *** Socket Timeout...Server not responding to request...Ending Session... ***");
    		return;
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
	    		
	    		System.out.println("\n*** Received error...Ending session... ***\n");
	    		return;
		    	
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
            	
            	// Send the error packet
            	sendPacket(sendReceiveSocket, sendErrorPacket);
    		    
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
            	
            	// Send the error packet
            	sendPacket(sendReceiveSocket, sendErrorPacket);
    		    
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
