package client;

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

import server.Server;

public class Client {
	
	
	public static enum Mode { 
		NORMAL, TEST 
	};
	
	private static enum PacketType {
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
	
	private enum PacketAction {
		SEND, RECEIVE
	}
	
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	
	private Mode mode;
	
	public Client(Mode mode) {
		this.mode = mode;
		
	    try {
	        sendReceiveSocket = new DatagramSocket();
	  
	    } catch (SocketException se) {
	    	se.printStackTrace();
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
	
	private DatagramPacket formACKPacket(InetAddress address, int port, byte[] blockNumber) throws UnknownHostException {
		byte[] ackData = new byte[4];
		ackData[0] = 0;
		ackData[1] = Server.PacketType.ACK.getOpcode();
		ackData[2] = blockNumber[0];
		ackData[3] = blockNumber[1];
		
		return new DatagramPacket(ackData, ackData.length, address, port);
		
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
	
	
	private void sendFile(DatagramPacket packet, String fileName) {
	    // Send data to be written to server
    	try {
        	BufferedInputStream in = new BufferedInputStream(new FileInputStream("src/client/files/" + fileName));
	        
	        byte[] dataFromFile = new byte[512];
	        int n;
	        
	        byte[] blockNumber = {0, 1};
	        
	        // Read the file in 512 byte chunks
	        while ((n = in.read(dataFromFile)) != -1) {
	        	     	
	        	DatagramPacket sendDataPacket = formDataPacket(packet.getAddress(), packet.getPort(), 
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
    		//TODO
    		System.out.println("No such file " + fileName);
    	} catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
    	}
    	
	}
	
	
	private void receiveFile(DatagramPacket packet, String fileName) {
		
		byte[] blockNumber = {0,1};
		int dataLength = 512;
	
	    
	 	
	    do {
			// Construct an ACK datagram packet
		    try {
		    	sendPacket = formACKPacket(packet.getAddress(), packet.getPort(), blockNumber);
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
		    
	        byte dataFromServer[] = new byte[516];
	        
			receivePacket = new DatagramPacket(dataFromServer, dataFromServer.length);
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
		    
		    // Update the length of the file data
		    dataLength = getFileDataFromDataPacket(receivePacket).length;
		    
	    	
	    } while(dataLength == 512);

	    
	    System.out.println("Client (" + Thread.currentThread() + "): Finished receiving file");
	    
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
	    byte data[] = new byte[100];
	    receivePacket = new DatagramPacket(data, data.length);
	
	    try {
	        // Block until a datagram is received via the send/receive socket.  
	        sendReceiveSocket.receive(receivePacket);
	    } catch(IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }
	
	    printPacketInfo(receivePacket, PacketAction.RECEIVE);
	    
	    
	    byte[] dataRec = receivePacket.getData();
	    
	    if (dataRec[0] == 0 && dataRec[1] == 4 && dataRec[2] == 0 && dataRec[3] == 0) {
	    	System.out.println("Sending file....");
	    	sendFile(receivePacket, fileName);
	    	
	    } else if (dataRec[0] == 0 && dataRec[1] == 3 && dataRec[2] == 0 && dataRec[3] == 1) {
	    	System.out.println("Receiving file....");
	    	receiveFile(receivePacket, fileName);
	    }

		
		
	}
	
	
	public static void main(String args[]) {
		
		Client newClient = new Client(Mode.TEST);
		
		// Send a READ request
		System.out.println("\n~~~~SENDING A READ REQUEST for file testFileFromServer.txt ~~~~\n");
		//newClient.sendAndReceive(PacketType.READ, "netascii", "testFileFromServer.txt");

		// Send a WRITE request
		System.out.println("\n~~~~SENDING A WRITE REQUEST for file testFileFromClient.txt ~~~~\n");
		newClient.sendAndReceive(PacketType.WRITE, "netascii", "testFileFromClient.txt");
		
		if (newClient.sendReceiveSocket != null) {
			newClient.sendReceiveSocket.close();
		}
		
	}

}
