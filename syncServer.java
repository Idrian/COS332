import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;

public class syncServer {

	private static int PORT_NUMBER;
	private static final String DONE = "DONE";
	private static Socket socket;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;
	private static ServerSocket serversocket;
	private static String baseDir;
	private static double lastModified;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		System.out.println("Starting File Sync Server!");
		baseDir = "./cos332Server";
		PORT_NUMBER = 8001;
		serversocket = new ServerSocket(PORT_NUMBER);
		InputStream reader;
		OutputStream writer;
		
		File fBaseDir = new File(baseDir);
		Boolean baseDirExists = fBaseDir.exists();

		if(!baseDirExists)
		{
			fBaseDir.mkdir();
		}
		
		System.out.println("Dir to sync: " + baseDir);
		System.out.println("Server Port: "+PORT_NUMBER);
		ArrayList<File> files = new ArrayList<File>(Arrays.asList(fBaseDir.listFiles()));
		
		System.out.println("Files in directory: ");
		for(int i=0;i<files.size();i++)
		{
			System.out.println(files.get(i).getName());
		}
		
		while (true) {
			socket = serversocket.accept();
			
			lastModified = fBaseDir.lastModified();
			

			in = new ObjectInputStream(socket.getInputStream());

			out = new ObjectOutputStream(socket.getOutputStream());
			
			double clientLastModified = (double) in.readObject();

			System.out.println("New client connected! IP: " + socket.getInetAddress().toString() + " Directory: " + baseDir);
			System.out.println("client: " + clientLastModified + ", Server: " + lastModified);

			if(clientLastModified > lastModified)//client is newer
			{
				out.writeObject("send");
				out.flush();
				
				ArrayList<File> clientfiles = new ArrayList<File>();
				
				System.out.println("updating server");
				clientfiles = (ArrayList<File>) in.readObject();
				
				for(int i=0;i<files.size();i++)//check for deletions
				{
					System.out.println("checking :"+files.get(i).getName());
					boolean delete = true;
					for(int j=0;j<clientfiles.size();j++) 
					{
						//System.out.println("client has :"+clientfiles.get(j).getName());
						if(files.get(i).getName().equals(clientfiles.get(j).getName()))
						{
							System.out.println("client has :"+clientfiles.get(j).getName());
							delete = false;
						}
					}
					if(delete)
					{
						System.out.println("Deleting :"+files.get(i).getName());
						files.get(i).delete();
					}
					
				}
				
				for(int i=0;i<files.size();i++)
				{	
					File currFile = new File(baseDir,clientfiles.get(i).getName());
					FileOutputStream wr = new FileOutputStream(currFile);
					byte[] outBuffer = new byte[socket.getReceiveBufferSize()];
					int bytesReceived = 0;
					while((bytesReceived = in.read(outBuffer))>0) {
						wr.write(outBuffer,0,bytesReceived);
					}
					wr.flush();
					wr.close();
					
					/*String currentLine;
					File currFile = new File(baseDir,files.get(i).getName());
					System.out.println(baseDir+"/"+currFile.getName()+" writing");
					
					File tmpFile = new File(baseDir,currFile.getName());
					tmpFile.createNewFile();*/
				}
				
				
			}
			else //server is newer
			{
				out.writeObject("recieve");
				out.flush();
				
				boolean isReady = (boolean) in.readObject();
				
				if(isReady)
				{
					System.out.println("updating client");
					
					for(int i=0;i<files.size();i++)
					{
						byte[] buff = new byte[socket.getSendBufferSize()];
						int bytesRead = 0;
						
						InputStream inputStream = new FileInputStream(files.get(i));
						
						while((bytesRead = inputStream.read(buff))>0) {
							out.write(buff,0,bytesRead);
						}
						
						in.close();
						out.flush();
						
					}
					
					out.writeObject(files);
					out.flush();
				}
			}
				
			
			
			out.close();
			in.close();
			socket.close();
			System.out.println("Client disconnected.");
		}
	}
}
