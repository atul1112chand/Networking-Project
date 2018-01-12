import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

//come back to here

public class GNBReceiver {
	
	public static byte[] CRLF = new byte[] { 0x0a, 0x0d };
	static int HEX_PER_LINE=10;
	public static void main(String[] args) throws Exception {
	  try
	  {
		
		String serverip=args[0];
		int serverport=Integer.parseInt(args[1]);
		String file=args[2];
		int seq_no;
		int flag=1,fgt=0;
		DatagramSocket fromSender = new DatagramSocket();
		//String file = file_s;
		//Initializing
		//DatagramSocket cs = new DatagramSocket();	
		InetAddress ip=InetAddress.getByName(serverip);
		byte[] sd=new byte[100];
		String sp = "REQUEST"+ file;
		sd=sp.getBytes();
		DatagramPacket start=new DatagramPacket(sd,sd.length,ip,serverport);//serverport);
		/*cs*/fromSender.send(start);
				
		
		int waitingFor = 0; int receiv=-1;
		int[] forget=new int[4];
		if(args.length>3)
		{
			for(int i=3;i<args.length;i++)
				forget[i-3]=Integer.parseInt(args[i]);
		}
		//ArrayList<RDTPacket> received = new ArrayList<RDTPacket>();
		ArrayList<byte[]> received = new ArrayList<byte[]>();
		
		boolean end = false;
		
		while(!end){
			// 6 is the base size (in bytes) of added messages
			byte[] receivedData = new byte[GNBSender.MSS + 6];
			System.out.println("Waiting for packet");
			System.out.println();
			
			// Receive packet
			DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
			fromSender.receive(receivedPacket);
			receivedData=receivedPacket.getData();
			
			
			String dat=new String(receivedPacket.getData(), "UTF-8");
			receiv = receivedData[3] & 0xFF;//Integer.parseInt(dat.substring(3,5));
			System.out.println("Received CONSIGNMENT " + receiv);//packet.getSeq() + " received (last: " + packet.isLast() + " )");
			

			
			
			
			
			String send = "ACK " + String.valueOf(receiv);
			byte[] ackBytes = concatenateByteArrays(send.getBytes(),CRLF);//send.getBytes();
			
			
			DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, ip,serverport);//receivedPacket.getAddress(), receivedPacket.getPort());
			
			// Send with some probability of loss
			if(receiv == waitingFor){
				fgt=0;
				for(int i=0;i<args.length-3;i++)
					if(receiv==forget[i])
					{
						//System.out.print(receiv);
						fgt=1;
						
						break;
					}
				if(fgt==1 && flag==1){//false){
					System.out.println("Forgot ACK " + receiv);
					if(dat.contains("END") == false){
						waitingFor++;
						//System.out.println("Waiting For = " + waitingFor);				
						received.add(receivedData);
						}
					flag=0;
				}else{	
					fromSender.send(ackPacket);
					System.out.println("Sent ACK " + receiv );
					System.out.println();
					waitingFor++;
					//System.out.println("Waiting For = " + waitingFor);				
					received.add(receivedData);
					flag=1;
				}
			}else if (receiv!=-1){System.out.println("waiting for = " + waitingFor);
				System.out.println("Packet out of order -- Discard " + receiv);
				System.out.println();
				//flag=1;
			}
			
			
			if(dat.contains("END") && waitingFor-1==receiv)
				break;

		}write_file(received, file);
	  }
	  catch (ArrayIndexOutOfBoundsException e){
            		System.out.println("Incorrect Number of Arguments");
        	}catch(Exception e){
           		System.out.println("IO Error");
 		}
		
	}		
	

	 public static void write_file(ArrayList<byte[]> packet, String filename) {
        
        byte[] MESSAGE_START = { 0x52, 0x44, 0x54 }; // "RDT "
    	byte[] MESSAGE_END = { 0x45, 0x4e, 0x44, 0xa, 0xd }; //" END CRLF"
    
   	int MESSAGE_FRONT_OFFSET = 4; //"RDT#"
   	int MESSAGE_BACK_OFFSET = 2; //"CRLF"
    	int MESSAGE_LAST_BACK_OFFSET = 5; //"ENDCRLF"
   
        
        String seqString;
        File myFile;
        FileOutputStream myFOS;
        
        byte[] data = new byte[512]; // each consignment has data length 10 bytes
        int count;                  // for copying / extracting from msg to data

        try {
            myFile = new File("new"+filename);
            myFOS = new FileOutputStream(myFile);
            //System.out.println(packet.size());	
            for (int i=0; i<packet.size();i++){//byte[] msg:messages) {
		byte[] msg = packet.get(i);
                // get sequence number
                seqString = String.valueOf(i);//new String(msg, MESSAGE_START.length, 1);
               
                // get last message
                if (i<packet.size()-1){//!matchByteSequence(msg, msg.length-MESSAGE_END.length , MESSAGE_END.length, MESSAGE_END)) {
		    	
                    myFOS.write(msg, MESSAGE_FRONT_OFFSET, msg.length-MESSAGE_FRONT_OFFSET-MESSAGE_BACK_OFFSET);
                   
             
                } else {String str = new String(msg, "UTF-8");
		    int end = str.indexOf("END");
			
                    myFOS.write(msg, MESSAGE_FRONT_OFFSET, end-MESSAGE_FRONT_OFFSET);
                    //System.out.println("Last Consignment");
                }
            }
            
            myFOS.close();
            
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        
        // extract data
        // write to file
        
    }
           
    static public boolean matchByteSequence(byte[] input, int offset, int length, byte[] ref) {
        
        boolean result = true;
        
        if (length == ref.length) {
            for (int i=0; i<ref.length; i++) {
                if (input[offset+i] != ref[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
	public static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        	byte[] result = new byte[a.length + b.length]; 
        	System.arraycopy(a, 0, result, 0, a.length); 
        	System.arraycopy(b, 0, result, a.length, b.length);
		return result;
    	}

	public static String byteToHex(byte b) {
        	int i = b & 0xFF;
        	return Integer.toHexString(i);
    	}
    
    	public static void printBytesAsHex(byte[] bytes) {
        
        	int i=0;
        	int j=0;
        	while (i<bytes.length) {
            		while (i<bytes.length && j<HEX_PER_LINE) {
                		System.out.print("0x" + byteToHex(bytes[i++]) + " ");
                		j++;
            		}
            	System.out.println(" ");
            	j = 0;
        	}
        
    	}
	
}
