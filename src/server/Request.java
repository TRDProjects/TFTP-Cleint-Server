package server;

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
import client.PacketAlreadyReceivedException;
import client.UnknownTransferIdException;
import server.Server.ErrorType;
import server.Server.PacketAction;
import server.Server.PacketType;

public class Request implements Runnable {
	
	public static final int PACKET_RETRANSMISSION_TIMEOUT = 1000;
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket requestPacket, sendPacket, receivePacket;
	
	private InetAddress clientAddress;
	
	private String fileName;
	
	
	public Request(DatagramPacket requestPacket) {
		this.requestPacket = requestPacket;
		this.fileName = getFileName(requestPacket.getData());
		
	    try {
	    	sendReceiveSocket = new DatagramSocket();
	    	
	    	sendReceiveSocket.setSoTimeout(2000);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	    
	    this.clientAddress = requestPacket.getAddress();
	}
	
	public void printPacketInfo(DatagramPacket packet, Server.PacketAction action) {
		System.out.println("\n");
		System.out.println("Server (Thread ID " + Thread.currentThread().getId() + "): " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + "packet:");
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
      
		System.out.println("Server: packet sent");
	}
	  
	public DatagramPacket receivePacket(DatagramSocket socket, int bufferLength) throws SocketTimeoutException {
		  
		// Construct a DatagramPacket for receiving packets
		byte dataBuffer[] = new byte[bufferLength];
      	DatagramPacket receivedPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
      	System.out.println("Server: waiting for packet.\n");
      
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
	
	
	private PacketType getDataRequestType(byte data[]) throws IllegalTftpOperationException {
		
		if (data.length < 2) {
			// First two bytes that indicate a read/write request do not exist
			throw new IllegalTftpOperationException("Read or write request opcode (01 or 02) does not exist");
		} else {
			// Check if the first and two bytes are either 0 1 or 0 2 for read or write request respectively
			if (data[0] == 0 && ((data[1] == 1) || (data[1] == 2))) {
				byte[] nameOfFile = new byte[512];
				byte[] mode = new byte[512];
				int numOfZeroBytes = 0;
				boolean middleZeroByteExists = false;
			    boolean lastZeroByteExists = false;
				boolean modeStringStarted = false;
			    
				for (int i = 2; i < data.length; i++) {

				    if (modeStringStarted) {
				    	mode[i - 2] = data[i];
				    } else {
				    	nameOfFile[i - 2] = data[i];
				    }
					
					if (data[i] == 0) {
						modeStringStarted = true;
						numOfZeroBytes++;
						
						if (i == data.length - 1) {
							lastZeroByteExists = true;
						} else {
							middleZeroByteExists = true;
						}
					}
				}
				
				String nameOfFileString = new String(nameOfFile, 0, nameOfFile.length).trim();
				String modeString = new String(mode, 0, mode.length).trim();
				
				if (nameOfFileString.length() == 0) {
					throw new IllegalTftpOperationException("File name is empty");
					
				} else if (modeString.length() == 0) {
					throw new IllegalTftpOperationException("Mode is empty");

				} else if (!modeString.toLowerCase().equals(Server.Mode.NETASCII.name().toLowerCase()) && 
						!modeString.toLowerCase().equals(Server.Mode.OCTET.name().toLowerCase())) {
					throw new IllegalTftpOperationException("Invalid mode. Received " + modeString + ". Must be netascii or octet.");
					
				} else if (!middleZeroByteExists) {
					throw new IllegalTftpOperationException("0 byte between filename and mode was not found");
					
				} else if (!lastZeroByteExists) {
					throw new IllegalTftpOperationException("Last byte is not a 0 byte");
					
				} else if (numOfZeroBytes != 2) {
					throw new IllegalTftpOperationException("More than two 0 bytes received");
					
				} else {
					// The data is valid
					if (data[1] == PacketType.READ.getOpcode()) {
						return PacketType.READ;
					} else if (data[1] == PacketType.WRITE.getOpcode()) {
						return PacketType.WRITE;
					}
				}
			    
			} else {
				// Invalid read/write request (i.e. first two bytes are not 0 1 or 0 2)
				throw new IllegalTftpOperationException("Invalid opcode. Must be 01 for a read request or 02 for a write request");
			}
		}
		
		throw new IllegalTftpOperationException("Invalid request");
	}
	
	
	private String getFileName(byte[] data) {
		byte[] fileNameInBytes = new byte[data.length];
		for (int i = 2; data[i] != 0; i++) {
			fileNameInBytes[i - 2] = data[i];
		}
		
		return new String(fileNameInBytes, 0, fileNameInBytes.length).trim();
	}
	
	
	private byte[] incrementBlockNumber(byte[] currentBlockNum) {
		short blockNum = ByteBuffer.wrap(currentBlockNum).getShort();
		
		byte[] bytes = new byte[2];
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		blockNum++;
		buffer.putShort(blockNum);
		return buffer.array();
	}
	
	private short getBlockNumberAsShort(byte[] blockNumber) {
		return ByteBuffer.wrap(blockNumber).getShort();
	}
	
	private DatagramPacket formACKPacket(InetAddress address, int port, byte[] blockNumber) throws UnknownHostException {
		byte[] ackData = new byte[4];
		ackData[0] = 0;
		ackData[1] = Server.PacketType.ACK.getOpcode();
		ackData[2] = blockNumber[0];
		ackData[3] = blockNumber[1];
		
		return new DatagramPacket(ackData, ackData.length, address, port);
		
	}
	
	private void validateDataPacket(DatagramPacket packet, byte[] expectedBlockNumber, int expectedPort)
			throws IllegalTftpOperationException, UnknownTransferIdException, PacketAlreadyReceivedException {
		
		removeTrailingZeroBytesFromDataPacket(packet);
			
		// Make sure the address and port the packet is coming from is the same
		if (packet.getAddress().equals(clientAddress) && packet.getPort() == expectedPort) {
				
			byte[] data = packet.getData();
			
			if (data[0] == 0 && data[1] == PacketType.DATA.getOpcode()) {
				// Check to make sure the packet is not larger than expected
				if (data.length > 516) {
					// The packet is too large
					throw new IllegalTftpOperationException("DATA packet contains too much data. Maximum is 512 bytes");
					
				}
				
				// Check to make sure the block number is valid 

				short packetBlockNumberShort   = getBlockNumberAsShort(new byte[]{ data[2], data[3]});
				short expectedBlockNumberShort = getBlockNumberAsShort(expectedBlockNumber);
				
				if (packetBlockNumberShort == expectedBlockNumberShort) {
					//Block number is valid
					return;
				} else if (packetBlockNumberShort < expectedBlockNumberShort) {
					//Block number was already received, beforehand
					throw new PacketAlreadyReceivedException("DATA packet with block number" + data[2] + data[3] +
							" has already been recieved");
				} else {
					//Block number is too high, has not been seen
					throw new IllegalTftpOperationException("DATA packet with invalid block number. "
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
		if (packet.getAddress().equals(clientAddress) && packet.getPort() == expectedPort) {
				
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
					// The ACK is valid and it is the exact ACK that we are expecting
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
	
	private DatagramPacket formDataPacket(InetAddress address, int port, byte[] data, int dataLength, byte[] blockNumber) {
		byte[] dataPacket = new byte [4 + data.length];
		dataPacket[0] = 0;
		dataPacket[1] = PacketType.DATA.getOpcode();
		dataPacket[2] = blockNumber[0];
		dataPacket[3] = blockNumber[1];
		
		System.arraycopy(data, 0, dataPacket, 4, dataLength);
		
		return new DatagramPacket(dataPacket, dataPacket.length, address, port);
	}
	
	
	private DatagramPacket formErrorPacket(InetAddress address, int port, Server.ErrorType errorType, String errorMessage) {
		byte[] errorPacket = new byte[5 + errorMessage.getBytes().length];
		errorPacket[0] = 0;
		errorPacket[1] = PacketType.ERROR.getOpcode();
		errorPacket[2] = 0;
		errorPacket[3] = errorType.getErrorCode();
		
		System.arraycopy(errorMessage.getBytes(), 0, errorPacket, 4, errorMessage.getBytes().length);
		
		errorPacket[errorPacket.length - 1] = 0;
		
		return new DatagramPacket(errorPacket, errorPacket.length, address, port);
	}
	

	

	private void processReadRequest() {

		System.out.println("Server (" + Thread.currentThread() + "): processing READ request");
		
    	int connectionPort = requestPacket.getPort();
        byte[] dataFromFile = new byte[512];
        int n;
        byte[] blockNumber = {0, 1};
        
        boolean receivedDuplicateAck = false;
        
		// Since the server will be sending the DATA packets, we set a timeout on the socket
		try {
		    sendReceiveSocket.setSoTimeout(PACKET_RETRANSMISSION_TIMEOUT);
		} catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
		}
		
    	try {
        	BufferedInputStream in = new BufferedInputStream(new FileInputStream("src/server/files/" + fileName));

	        
	        // Read the file in 512 byte chunks
	        while ((n = in.read(dataFromFile)) != -1) {
	        	     	
	        	DatagramPacket sendDataPacket = formDataPacket(requestPacket.getAddress(), requestPacket.getPort(), 
	        			dataFromFile, n, 
	        			blockNumber);
	        
	    	    
	    	    // Send the packet
	    	    sendPacket(sendReceiveSocket, sendDataPacket);
	    	    
	    	    // Wait to receive a packet
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
            			System.out.println("\n ****** Client Unreachable...Closing Thread ******");
            			in.close();
            			Thread.currentThread().interrupt();
            			return;
            		}
            	}

	    	    do {
		    	    try {
		    	    	PacketType packetType = getPacketType(receivePacket);
		    	    	
		    	    	if (packetType.equals(PacketType.ACK)) {
		    	    	    // Validate the ACK packet
		    	    	    try {
		    	    	    	validateAckPacket(receivePacket, blockNumber, connectionPort);
		    	    	    	
		    	    	    	receivedDuplicateAck = false;
		    	    	    	
			                    // ACK packet has been validated so we increment the block number now
			    	    	    blockNumber = incrementBlockNumber(blockNumber);
			    	    	    
		    	    	    	
		    	    	    } catch (IllegalTftpOperationException illegalOperationException) {
		    	    	    	System.out.println("IllegalTftpOperationException Thrown: " + illegalOperationException.getMessage());
		    	    	    	System.out.println("Sending error packet...");
		    	    	    	
		    	    	    	// Form the error packet
		    	            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
		    	            			receivePacket.getPort(), 
		    	            			ErrorType.ILLEGAL_TFTP_OPERATION, 
		    	            			illegalOperationException.getMessage());
		    	            	
		    	    		    
		    		    	    // Send the error packet
		    		    	    sendPacket(sendReceiveSocket, sendErrorPacket);
		    	    	    	
		    	    		    // Close the thread
		    		    	    System.out.println("\n*** Closing thread " + Thread.currentThread().getId() + "...\n");
		    		    	    in.close();
		    	    	    	Thread.currentThread().interrupt();
		    	    	    	return;
		    	    	    	
		    	    	    	
		    	    	    } catch(UnknownTransferIdException unknownTransferIdException) {
		    	    	    	System.out.println("UnknownTransferIdException Thrown: " + unknownTransferIdException.getMessage());
		    	    	    	System.out.println("Sending error packet...");
		    	    	    	
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
		    	    	    	
		    	    	    	receivedDuplicateAck = true;
		    	    	    	
		    		    	    // Wait to receive a packet
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
		    	            			System.out.println("\n ****** Client Unreachable...Closing Thread ******");
		    	            			in.close();
		    	            			Thread.currentThread().interrupt();
		    	            			return;
		    	            		}
		    	            	}
		    	            	
		    	    	    	continue;
		    	    	    }
		    	    	      
		    	    	} else if (packetType.equals(PacketType.ERROR)) {
		    				System.out.println("\n*** Received error packet...Closing Thread...***");
		    				in.close();
		    				Thread.currentThread().interrupt();
		    				return;
		    	    	}
		    	    	
		    	    } catch (InvalidPacketTypeException e) {
	    		    	System.out.println("\n*** Received Packet with invalid opCode ***");
	    		    	// send error for invalid opcode
	        	    	System.out.println("Sending error packet...");
	        	    	
	        	    	// Form the error packet
	                	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
	                			receivePacket.getPort(), 
	                			ErrorType.ILLEGAL_TFTP_OPERATION, 
	                			"Invalid opCode");
	                	
	                	// Send the error packet
	                	sendPacket(sendReceiveSocket, sendErrorPacket);
	        		    
	                	in.close();
	    				Thread.currentThread().interrupt();
	    				return;
		    	    }
	    	    } while (receivedDuplicateAck);

	        }
	        
	        in.close();
    	} catch (FileNotFoundException e) {
    		System.out.println("No such file " + fileName);
    		System.out.println("Exiting thread...");
    		Thread.currentThread().interrupt();
    		return;
    	} catch (IOException e) {
	        e.printStackTrace();
	        Thread.currentThread().interrupt();
	        System.exit(1);
    	}
    	
    	System.out.println("\n*** Server (Thread ID " + Thread.currentThread().getId() + "): Finished sending file...Closing thread");
    	
	    sendReceiveSocket.close();
	    
		// Close thread
		Thread.currentThread().interrupt();
		return;
		
	}
	
	
	
	private void processWriteRequest() {
		
		System.out.println("Server (Thread ID " + Thread.currentThread().getId() + "): processing WRITE request");
		
		int connectionPort = requestPacket.getPort();
		
		byte[] blockNumber = {0,0};
		int dataLength = 512;
		
		// Since the client will be sending the DATA packets, no timeout on the socket is needed
		try {
			// Set the timeout to 0 (infinite)
		    sendReceiveSocket.setSoTimeout(0);
		} catch (SocketException se) {
	    	se.printStackTrace();
	        System.exit(1);
		}
	    
	    // Initialize the write file
	    try {
	    	BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("src/server/files/" + fileName));
	    	
			// Construct the first ACK datagram packet with block number 00
		    try {
		    	sendPacket = formACKPacket(requestPacket.getAddress(), requestPacket.getPort(), blockNumber);
		    } catch (UnknownHostException e) {
		        e.printStackTrace();
		        System.exit(1);
		    }
		    
		    
		    // Send the first ACK packet
		    sendPacket(sendReceiveSocket, sendPacket);
		    
			
		    // Increment the block number
		    blockNumber = incrementBlockNumber(blockNumber);
	    	
		    do {

			    // Try to receive a packet
			    try {
			    	receivePacket = receivePacket(sendReceiveSocket, 517);
			    } catch (SocketTimeoutException te) {
        			//Timeout on receiving DATA packet - do not retransmit
			    	System.out.println("\n ****** No response from Client...Closing Thread ******");
        			out.close();
        			Thread.currentThread().interrupt();
        			return;
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
						    
							// Construct an ACK datagram packet
						    try {
						    	sendPacket = formACKPacket(requestPacket.getAddress(), requestPacket.getPort(), blockNumber);
						    } catch (UnknownHostException e) {
						        e.printStackTrace();
						        System.exit(1);
						    }
						    
						    
						    // Send the ACK packet
						    sendPacket(sendReceiveSocket, sendPacket);
						    
						    // Increment the block number
						    blockNumber = incrementBlockNumber(blockNumber);
					    	
			    	    } catch (IllegalTftpOperationException illegalOperationException) {
			    	    	System.out.println("IllegalTftpOperationException Thrown: " + illegalOperationException.getMessage());
			    	    	System.out.println("Sending error packet...");
			    	    	
			    	    	// Form the error packet
			            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
			            			receivePacket.getPort(), 
			            			ErrorType.ILLEGAL_TFTP_OPERATION, 
			            			illegalOperationException.getMessage());
			            	
				    	    // Send the error packet
				    	    sendPacket(sendReceiveSocket, sendErrorPacket);
			    	    	
			    		    // Close the thread
				    	    System.out.println("\n*** Closing thread " + Thread.currentThread().getId() + "...\n");
			    	    	
				    	    out.close();
				    	    Thread.currentThread().interrupt();
			    	    	return;
			    	    	
			    	    	
			    	    } catch(UnknownTransferIdException unknownTransferIdException) {
			    	    	System.out.println("UnknownTransferIdException Thrown: " + unknownTransferIdException.getMessage());
			    	    	System.out.println("Sending error packet...");
			    	    	
			    	    	// Form the error packet
			            	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
			            			receivePacket.getPort(), 
			            			ErrorType.UNKNOWN_TRANSFER_ID, 
			            			unknownTransferIdException.getMessage());
			            	
				    	    // Send the error packet
				    	    sendPacket(sendReceiveSocket, sendErrorPacket);
			    	    	
			    	    } catch(PacketAlreadyReceivedException alreadyReceivedException) {
			    	    	//Ignore this packet, but send an ACK packet with appropriate block number regardless
			    	    	System.out.println("\n*** The received DATA packet was already received beforehand...Sending ACK packet and continuing... *** \n");
			    	    	
			    	    	//Get block number from duplicate DATA packet
			    	    	byte[] duplicateBlockNumber = new byte[2];
			    	    	duplicateBlockNumber[0] = receivePacket.getData()[2];
			    	    	duplicateBlockNumber[1] = receivePacket.getData()[3];
			    	    	
			    	    	//Construct ACK packet
			    	    	DatagramPacket sendAckPacket = formACKPacket(receivePacket.getAddress(),
			    	    			receivePacket.getPort(),
			    	    			duplicateBlockNumber);
			    	    			
			    	    	//Send the packet		
			    	    	sendPacket(sendReceiveSocket, sendAckPacket);

						    
			    	    	continue;
			    	    }
					    
			    	} else if (packetType.equals(PacketType.ERROR)) {
	    				System.out.println("\n*** Received error packet...Closing Thread...***");
	    				out.close();
	    				Thread.currentThread().interrupt();
	    				return;
			    	}
			    	
			    } catch (InvalidPacketTypeException e) {
    		    	System.out.println("\n*** Received Packet with invalid opCode ***");
    		    	// send error for invalid opcode
        	    	System.out.println("Sending error packet...");
        	    	
        	    	// Form the error packet
                	DatagramPacket sendErrorPacket = formErrorPacket(receivePacket.getAddress(), 
                			receivePacket.getPort(), 
                			ErrorType.ILLEGAL_TFTP_OPERATION, 
                			"Invalid opCode");
                	
                	// Send the error packet
                	sendPacket(sendReceiveSocket, sendErrorPacket);
        		    
                	out.close();
    				Thread.currentThread().interrupt();
    				return;
	    	    }
			    
    
		    	
		    } while (dataLength == 512);
		    

		    out.close();
	    	
    	} catch (IOException e) {
	        e.printStackTrace();
	        Thread.currentThread().interrupt();
	        System.exit(1);
    	}

		
	    
	    System.out.println("\n*** Server (Thread ID " + Thread.currentThread().getId() + "): Finished receiving/writing file...Closing thread");
	    
	    
	    sendReceiveSocket.close();
	    
		// Close thread
		Thread.currentThread().interrupt();
		return;
	       
	}
	

	@Override
	public void run() {
		
		if (Thread.currentThread().isInterrupted()) {
			return;
		}

	    // Process the received datagram.
	    printPacketInfo(requestPacket, PacketAction.RECEIVE);
	    
		
		// Get the request type (i.e READ, WRITE, or INVALID)
	    try {
	    	PacketType reqType = getDataRequestType((new String(requestPacket.getData(), 0, requestPacket.getLength())).getBytes());
	    	
				if (reqType.equals(PacketType.READ)) {
					processReadRequest();
					
				} else if (reqType.equals(PacketType.WRITE)) {
				    processWriteRequest();
				    
				} else {
					Thread.currentThread().interrupt();
					return;
				}
	    } catch (IllegalTftpOperationException invalidPacketException) {
	    	System.out.println("IllegalTftpOperationException Thrown: " + invalidPacketException.getMessage());
	    	System.out.println("Sending error packet...");
	    	
	    	// Form the error packet
        	DatagramPacket sendErrorPacket = formErrorPacket(requestPacket.getAddress(), 
        			requestPacket.getPort(), 
        			ErrorType.ILLEGAL_TFTP_OPERATION, 
        			invalidPacketException.getMessage());
        	
    	    // Send the error packet
    	    sendPacket(sendReceiveSocket, sendErrorPacket);
	    	
		    // Close the thread
    	    System.out.println("\n*** Closing thread " + Thread.currentThread().getId() + "...\n");
	    	Thread.currentThread().interrupt();
	    	return;
	    }
		
		
	}

}
