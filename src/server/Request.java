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

import server.Server.PacketAction;
import server.Server.PacketType;

public class Request implements Runnable {
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket requestPacket, sendPacket, receivePacket;
	
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
	
	
	private PacketType getDataRequestType(byte data[]) throws InvalidRequestException {
		
		if (data.length < 2) {
			// First two bytes that indicate a read/write request do not exist
			throw new InvalidRequestException("Read or write request opcode (01 or 02) does not exist");
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
					throw new InvalidRequestException("File name is empty");
					
				} else if (modeString.length() == 0) {
					throw new InvalidRequestException("Mode is empty");

				} else if (!modeString.toLowerCase().equals(Server.Mode.NETASCII.name().toLowerCase()) && 
						!modeString.toLowerCase().equals(Server.Mode.OCTET.name().toLowerCase())) {
					throw new InvalidRequestException("Invalid mode. Received " + modeString + ". Must be netascii or octet.");
					
				} else if (!middleZeroByteExists) {
					throw new InvalidRequestException("0 byte between filename and mode was not found");
					
				} else if (!lastZeroByteExists) {
					throw new InvalidRequestException("Last byte is not a 0 byte");
					
				} else if (numOfZeroBytes != 2) {
					throw new InvalidRequestException("More than two 0 bytes received");
					
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
				throw new InvalidRequestException("Invalid opcode. Must be 01 for a read request or 02 for a write request");
			}
		}
		
		throw new InvalidRequestException("Invalid request");
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
	
	private boolean isValidDataPacket(DatagramPacket packet, byte[] expectedBlockNumber) {
		byte[] data = packet.getData();
		
		if (data[0] == 0 && data[1] == PacketType.DATA.getOpcode() && 
				data[2] == expectedBlockNumber[0] && 
				data[3] == expectedBlockNumber[1]) {
			
			return true;
			
		}
		
		return false;
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
	
	
	private DatagramPacket formDataPacket(InetAddress address, int port, byte[] data, int dataLength, byte[] blockNumber) {
		byte[] dataPacket = new byte [4 + data.length];
		dataPacket[0] = 0;
		dataPacket[1] = PacketType.DATA.getOpcode();
		dataPacket[2] = blockNumber[0];
		dataPacket[3] = blockNumber[1];
		
		System.arraycopy(data, 0, dataPacket, 4, dataLength);
		
		return new DatagramPacket(dataPacket, dataPacket.length, address, port);
	}
	
	
	private boolean isValidACKPacket(DatagramPacket packet, byte[] expectedBlockNumber) {
		byte[] data = packet.getData();
		
		if (data.length == 4) {
			if (data[0] == 0 && data[1] == PacketType.ACK.getOpcode() && 
					data[2] == expectedBlockNumber[0] && 
					data[3] == expectedBlockNumber[1]) {
				return true;
			}
		}
		
		return false;
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
	    	    
	    	    // Construct a DatagramPacket for receiving packets
	    	    byte dataForAck[] = new byte[4];
	    	    receivePacket = new DatagramPacket(dataForAck, dataForAck.length);
	    	
	    	    try {
	    	        // Block until a datagram is received via the send/receive socket.  
	    	        sendReceiveSocket.receive(receivePacket);
	    	    } catch(IOException e) {
	    	        e.printStackTrace();
	    	        System.exit(1);
	    	    }
	    	
	    	    printPacketInfo(receivePacket, PacketAction.RECEIVE);
	    	    
	    	    if (isValidACKPacket(receivePacket, blockNumber)) {
	    	    	blockNumber = incrementBlockNumber(blockNumber);
	    	    }
	        	
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
    	
    	System.out.println("Server (" + Thread.currentThread() + "): Finished reading file");
    	
	    sendReceiveSocket.close();
	    
		// Close thread
		Thread.currentThread().interrupt();
		return;
		
	}
	
	
	private void processWriteRequest() throws InvalidDataException {
		
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
			    
		        byte dataFromClient[] = new byte[516];
		        
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
			    printPacketInfo(receivePacket, PacketAction.RECEIVE);
			    
			    if (!isValidDataPacket(receivePacket, blockNumber)) {
			    	throw new InvalidDataException("Server (" + Thread.currentThread() + "): Invalid DATA packet received");
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

		
	    
	    System.out.println("Server (" + Thread.currentThread() + "): Finished writing file");
	    
	    
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
					try {
						processWriteRequest();
					} catch (InvalidDataException e) {
						// TODO
						System.out.println("Invalid data received");
					}
				} else {
					Thread.currentThread().interrupt();
					return;
				}
	    } catch (InvalidRequestException e) {
	    	// TODO send error packet
	    	
	    	System.out.println("InvalidRequestException Thrown: " + e.getMessage());
	    	System.out.println("Closing thread...");
	    	Thread.currentThread().interrupt();
	    	return;
	    }
		
		
	}

}
