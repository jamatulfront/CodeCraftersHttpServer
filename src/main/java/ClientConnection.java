import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class ClientConnection implements Runnable{
    final private Socket socket;
   final private String filesRootDirectory;
    public ClientConnection(Socket socket,String filesRootDirectory){
        this.socket = socket;
        this.filesRootDirectory = filesRootDirectory;
    }

 @Override
 public void run() {
  
     try{
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String firstLine = in.readLine();

      if (firstLine != null && !firstLine.isEmpty()) {
            String[] requestLine = firstLine.split(" ");
            String httpMethod = requestLine[0];
            String route = requestLine[1];
       
            // Read the headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.trim().isEmpty()) {
              String[] header = headerLine.split(": ", 2);
              if (header.length == 2) {
                  headers.put(header[0], header[1]);
              }
          }

            // Check for body if the request has one (e.g., POST request)
            StringBuilder body = new StringBuilder();
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                body.append(bodyChars);
            }
            if(httpMethod.equals("GET")) {
                HttpGetRequestHandler getRequestHandler = new HttpGetRequestHandler(filesRootDirectory);
                if(route.startsWith("/echo")){
                  String encoding = headers.get("Accept-Encoding");
                  if(encoding != null && encoding.contains("gzip")){
                    byte[] compressedContent = compressWithGzip(route.substring(6));
                    byte[] byteResponse = buildCompressedResponse("200 OK", "text/plain",compressedContent, "gzip");
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(byteResponse);
                    outputStream.close();
                  }else{
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.print(getRequestHandler.handleGetRequest(route, headers, body));  
                    out.close();               
                  }
                }else{
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.print(getRequestHandler.handleGetRequest(route, headers, body));
                    out.close();
                }

            }
            else if(httpMethod.equals("POST")) {
                String response = handlePostRequest(route, headers, body);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.print(response);
                out.close();
            };
           

          }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 }

 String handlePostRequest(String route, Map<String,String> headers, StringBuilder body){
    System.out.println("handling post request");
    if(route.contains("/files")){
        System.out.println("inside the files");
        try {
            String fileName = route.substring(7);
            File file = new File(filesRootDirectory,fileName);
            File dir = new File(filesRootDirectory);
            if(!dir.exists()){
              dir.mkdir();
            }
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(body.toString());
            bw.close();
        } catch (Exception e) {
            System.out.println("Failed to create or write data");
        }
    }
    return "HTTP/1.1 201 Created\r\n"+ "\r\n";
 }

 private byte[] buildCompressedResponse(String status, String contentType, byte[] compressedContent, String encoding) {
    // Construct the header as bytes
    String header = "HTTP/1.1 " + status + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Encoding: " + encoding + "\r\n" +
                    "Content-Length: " + compressedContent.length + "\r\n" +
                    "\r\n";
    
    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
    
    byte[] responseBytes = new byte[headerBytes.length + compressedContent.length];
    
    System.arraycopy(headerBytes, 0, responseBytes, 0, headerBytes.length);
    System.arraycopy(compressedContent, 0, responseBytes, headerBytes.length, compressedContent.length);
    
    return responseBytes;
}
  private byte[] compressWithGzip(String content) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return byteStream.toByteArray();
    }

}

