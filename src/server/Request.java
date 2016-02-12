package server;

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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import server.Server.ErrorType;
import server.Server.PacketAction;
import server.Server.PacketType;

public class Request implements Runnable {
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket requestPacket, sendPacket, receivePacket;
	
	private InetAddress clientAddress;
	private int clientPort;
	
	private String fileName;
	
	
	public Request(DatagramPacket requestPacket) {
		this.requestPacket = requestPacket;
		this.fileName = getFileName(requestPacket.getData());
		
	    try {
	    	sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	    
	    this.clientAddress = requestPacket.getAddress();
	    this.clientPort = requestPacket.getPort();
	}
	
	public void printPacketInfo(DatagramPacket packet, Server.PacketAction action) {
		System.out.println("\n");
		System.out.println("Server (" + Thread.currentThread() + "): " + (action.equals(PacketAction.SEND) ? "Sending " : "Received ") + "packet:");
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "To " : "From ") + "host: " + packet.getAddress());
		System.out.println("   " + (action.equals(PacketAction.SEND) ? "Destination host " : "Host ") + "port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("   " + "Length: " + len);
		System.out.println("   " + "Containing: ");
		String dataString = new String(packet.getData(),0,len);
		System.out.println("       - String: " + dataString);
		System.out.println("       - Bytes: " + Arrays.toString(dataString.getBytes()));
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
	
	private DatagramPacket formACKPacket(InetAddress address, int port, byte[] blockNumber) throws UnknownHostException {
		byte[] ackData = new byte[4];
		ackData[0] = 0;
		ackData[1] = Server.PacketType.ACK.getOpcode();
		ackData[2] = blockNumber[0];
		ackData[3] = blockNumber[1];
		
		return new DatagramPacket(ackData, ackData.length, address, port);
		
	}
	
	private void validateDataPacket(DatagramPacket packet, byte[] expectedBlockNumber)
			throws IllegalTftpOperationException, UnknownTransferIdException {
		
		
		// Make sure the address the packet is coming from is the same
		if (packet.getAddress().equals(clientAddress)) {
			
			// Make sure the port the packet is coming from is the same
			if (packet.getPort() == clientPort) {
				
				byte[] data = packet.getData();
				
				if (data[0] == 0 && data[1] == PacketType.DATA.getOpcode()) {
					if (data[2] == expectedBlockNumber[0] && data[3] == expectedBlockNumber[1]) {
						
						byte[] fileDataContainedInPacket = getFileDataFromDataPacket(packet);
						
						// Check to make sure the packet is not larger than expected
						if (fileDataContainedInPacket.length <= 512) {
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
				throw new UnknownTransferIdException("DATA packet received from invalid port");
			}
			
		} else {
			throw new UnknownTransferIdException("DATA packet received from invalid address");
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
	
	
	private void validateAckPacket(DatagramPacket packet, byte[] expectedBlockNumber) 
			throws IllegalTftpOperationException, UnknownTransferIdException {
		
		// Make sure the address the packet is coming from is the same
		if (packet.getAddress().equals(clientAddress)) {
			
			// Make sure the port the packet is coming from is the same
			if (packet.getPort() == clientPort) {
				
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
				throw new UnknownTransferIdException("ACK packet received from invalid port");
			}
			
		} else {
			throw new UnknownTransferIdException("ACK packet received from invalid address");
		}
	
	}
	
	private void processReadRequest() {

		System.out.println("Server (" + Thread.currentThread() + "): processing READ request");
		
    	try {
        	BufferedInputStream in = new BufferedInputStream(new FileInputStream("src/server/files/" + fileName));
	        
	        byte[] dataFromFile = new byte[512];
	        int n;
	        
	        byte[] blockNumber = {0, 1};
	        
	        // Read the file in 512 byte chunks
	        while ((n = in.read(dataFromFile)) != -1) {
	        	     	
	        	DatagramPacket sendDataPacket = formDataPacket(requestPacket.getAddress(), requestPacket.getPort(), 
	        			dataFromFile, n, 
	        			blockNumber);
	        
	        	removeTrailingZeroBytesFromDataPacket(sendDataPacket);
	    	    printPacketInfo(sendDataPacket, PacketAction.SEND);
	    	    

	    	    // Send the datagram packet to the server via the send/receive socket. 
	    	    try {
	    	    	sendReceiveSocket.send(sendDataPacket);
	    	    } catch (IOException e) {
	    	        e.printStackTrace();
	    	        System.exit(1);
	    	    }
	    	
	    	    System.out.println("Server: Packet sent.\n");
	    	    
	    	    // Wait to receive an ACK
	    	    
	    	    // Construct a DatagramPacket for receiving the ACK packet
	    	    // Note that an ACK packet should be 4 bytes long but we create a larger buffer for error checking purposes
	    	    byte dataForAck[] = new byte[5];
	    	    receivePacket = new DatagramPacket(dataForAck, dataForAck.length);
	    	
	    	    try {
	    	        // Block until a datagram is received via the send/receive socket.  
	    	        sendReceiveSocket.receive(receivePacket);
	    	    } catch(IOException e) {
	    	        e.printStackTrace();
	    	        System.exit(1);
	    	    }
	    	
	    	    printPacketInfo(receivePacket, PacketAction.RECEIVE);
	    	    
	    	    // Validate the ACK packet
	    	    try {
	    	    	validateAckPacket(receivePacket, blockNumber);
	    	    	
	    	    } catch (IllegalTftpOperationException illegalOperationException) {
	    	    	System.out.println("IllegalTftpOperationException Thrown: " + illegalOperationException.getMessage());
	    	    	System.out.println("Sending error packet and closing thread...");
	    	    	
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
	    	    	
	    		    // Close the thread
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
	    	    	
	    	    }
	    	    
	    	        
                // ACK packet has been validated so we increment the block number now
	    	    blockNumber = incrementBlockNumber(blockNumber);
	   
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
    	
    	System.out.println("\n Server (" + Thread.currentThread() + "): Finished reading file");
    	
	    sendReceiveSocket.close();
	    
		// Close thread
		Thread.currentThread().interrupt();
		return;
		
	}
	
	
	private void processWriteRequest() {
		
		System.out.println("Server (" + Thread.currentThread() + "): processing WRITE request");
		
		byte[] blockNumber = {0,0};
		int dataLength = 512;
	    
	    // Initialize the write file
	    try {
	    	BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("src/server/files/" + fileName));
	    	
		    do {
				// Construct an ACK datagram packet
			    try {
			    	sendPacket = formACKPacket(requestPacket.getAddress(), requestPacket.getPort(), blockNumber);
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
			       Thread.currentThread().interrupt();
			       System.exit(1);
			    }
				
			    // Increment the block number
			    blockNumber = incrementBlockNumber(blockNumber);
			    
	    	    // Construct a DatagramPacket for receiving the DATA packet
	    	    // Note that a DATA packet can be at most 516 bytes long but we create a larger buffer for error checking purposes
		        byte dataFromClient[] = new byte[517];
				receivePacket = new DatagramPacket(dataFromClient, dataFromClient.length);
				
			    System.out.println("Server: waiting for Packet.\n");

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
			    removeTrailingZeroBytesFromDataPacket(receivePacket);
			    printPacketInfo(receivePacket, PacketAction.RECEIVE);
			    
			    
			    // Validate the DATA packet
			    try {
			    	validateDataPacket(receivePacket, blockNumber);
			    	
	    	    } catch (IllegalTftpOperationException illegalOperationException) {
	    	    	System.out.println("IllegalTftpOperationException Thrown: " + illegalOperationException.getMessage());
	    	    	System.out.println("Sending error packet and closing thread...");
	    	    	
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
	    	    	
	    		    // Close the thread
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
	    	    	
	    	    }
			    
			    // Update the length of the file data
			    dataLength = getFileDataFromDataPacket(receivePacket).length;
			    
			    // Write to file
			    out.write(getFileDataFromDataPacket(receivePacket), 0, dataLength);
			    
		    	
		    } while(dataLength == 512);
		    
		    out.close();
	    	
    	} catch (IOException e) {
	        e.printStackTrace();
	        Thread.currentThread().interrupt();
	        System.exit(1);
    	}

		
	    
	    System.out.println("\n Server (" + Thread.currentThread() + "): Finished writing file");
	    
	    
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
	    	System.out.println("InvalidRequestException Thrown: " + invalidPacketException.getMessage());
	    	System.out.println("Sending error packet and closing thread...");
	    	
	    	// Form the error packet
        	DatagramPacket sendErrorPacket = formErrorPacket(requestPacket.getAddress(), 
        			requestPacket.getPort(), 
        			ErrorType.ILLEGAL_TFTP_OPERATION, 
        			invalidPacketException.getMessage());
        	
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
	    	
		    // Close the thread
	    	Thread.currentThread().interrupt();
	    	return;
	    }
		
		
	}

}
