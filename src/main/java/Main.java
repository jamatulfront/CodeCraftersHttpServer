
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Main {
  private static String filesRootDirectory;
  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("--directory")) {
      filesRootDirectory = args[1]; // Get the directory path from the command line
  }
  System.out.println("Serving files from directory: " + filesRootDirectory);
    try {
      ServerSocket serverSocket = new ServerSocket(4221);
      System.out.println("Server is running at port : 4221");
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
    
      while (true) {
        Socket socket =  serverSocket.accept();
        Thread newThread = new Thread(new ClientConnection(socket,filesRootDirectory));
        newThread.start();
      } // Wait for connection from client.

  }catch(IOException exception){
    System.out.println("Failed to start the server");
  }
  
  }
}