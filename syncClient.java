import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;


public class syncClient {
	
	private static String serverIP;
	private static int PORT_NUMBER;
	private static final String DONE = "DONE";
	private static Socket socket;
	private static ObjectInputStream in;
	private static ObjectOutputStream out;
	private static int fileCount = 0;
	private static String baseDir;
	private static double lastModified;

	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
		baseDir = "./cos332Client";
		serverIP = "localhost";
		PORT_NUMBER = 8001;

		File fBaseDir = new File(baseDir);
		Boolean baseDirExists = fBaseDir.exists();

		if(!baseDirExists)
		{
			fBaseDir.mkdir();
		}
		
		
		
		System.out.println("Client running!");
		System.out.println("Dir to sync: " + baseDir);
		ArrayList<File> files = new ArrayList<File>(Arrays.asList(fBaseDir.listFiles()));
		
		System.out.println("Files in directory: ");
		for(int i=0;i<files.size();i++)
		{
			System.out.println(files.get(i).getName());
		}
		
		System.out.println("Server IP: " + serverIP+":"+PORT_NUMBER);
		
		socket = new Socket(serverIP, PORT_NUMBER);
		out = new ObjectOutputStream(socket.getOutputStream());
		lastModified = fBaseDir.lastModified();
		out.writeObject(lastModified);
		out.flush();
		
		in = new ObjectInputStream(socket.getInputStream()); 
		
		System.out.print("Syncing");
		
		String syncCommand = (String) in.readObject();
		if(syncCommand.equals("send"))//client is newer
		{
			System.out.println("updating server");
			
			out.writeObject(files);
			
			for(int i=0;i<files.size();i++)
			{
				byte[] buff = new byte[socket.getSendBufferSize()];
				int bytesRead = 0;
	
				InputStream inputStream = new FileInputStream(files.get(i));
	
				while((bytesRead = in.read(buff))>0) {
					out.write(buff,0,bytesRead);
				}
				inputStream.close();
				out.flush();
			}
			//out.writeObject(files);
		}
		else //server is newer
		{
			out.writeObject(true);
			
			System.out.println("updating client");
			files = new ArrayList<File>();
			
			
		}
		

		System.out.println();
		System.out.println("Finished sync");
		
		out.close();
		in.close();
		socket.close();
	}
	
	private static void sendFile(File Dir) throws Exception {
		byte[] buff = new byte[socket.getSendBufferSize()];
		int bytesRead = 0;

		InputStream istream = new FileInputStream(Dir);

		while((bytesRead = istream.read(buff))>0) {
			out.write(buff,0,bytesRead);
		}
		istream.close();
		
		out.flush();
		
		in.close();
		out.close();
		socket.close();
		socket = new Socket(serverIP, PORT_NUMBER);
		in = new ObjectInputStream(socket.getInputStream());
		out = new ObjectOutputStream(socket.getOutputStream());
	}
	
	private static void receiveFile(File Dir) throws Exception {
		FileOutputStream wr = new FileOutputStream(Dir);
		byte[] outBuffer = new byte[socket.getReceiveBufferSize()];
		int bytesReceived = 0;
		while((bytesReceived = in.read(outBuffer))>0) {
			wr.write(outBuffer,0,bytesReceived);
		}
		wr.flush();
		wr.close();

		in.close();
		out.close();
		socket.close();
		socket = new Socket(serverIP, PORT_NUMBER);
		in = new ObjectInputStream(socket.getInputStream());
		out = new ObjectOutputStream(socket.getOutputStream());
	}
	

}
