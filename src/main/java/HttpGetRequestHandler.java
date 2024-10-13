import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class HttpGetRequestHandler {

    private final String filesRootDirectory;

    public HttpGetRequestHandler(String filesRootDirectory) {
        this.filesRootDirectory = filesRootDirectory;
    }

    public String handleGetRequest(String route, Map<String, String> headers, StringBuilder body) {
        logRequest(route, headers, body);
        
        if (route.equals("/")) {
            return buildSimpleResponse("200 OK", "", headers);
        } 
        
        if (route.equals("/user-agent")) {
            return handleUserAgent(headers);
        }
        
        if (route.startsWith("/files")) {
            return handleFileRequest(route, headers);
        }
        
        if (route.startsWith("/echo")) {
            return handleEcho(route, headers);
        }

        return buildSimpleResponse("404 Not Found", "", headers);
    }

    private void logRequest(String route, Map<String, String> headers, StringBuilder body) {
        System.out.println("Route: " + route);
        headers.forEach((key, value) -> System.out.println("Header: " + key + " = " + value));
        System.out.println("Body: " + body);
    }

    // Handle root request
    private String handleUserAgent(Map<String, String> headers) {
        String userAgent = headers.get("User-Agent");
        if (userAgent == null) {
            return buildSimpleResponse("400 Bad Request", "", headers);
        }
        return buildResponseWithContent("200 OK", "text/plain", userAgent, headers);
    }

    // Handle file request
    private String handleFileRequest(String route, Map<String, String> headers) {
        String filePath = getFilePath(route);
        File file = new File(filePath);

        if (!file.exists()) {
            return buildSimpleResponse("404 Not Found", "", headers);
        }

        try {
            String fileContent = readFileContent(file);
            return buildResponseWithContent("200 OK", "application/octet-stream", fileContent, headers, file.length());
        } catch (IOException e) {
            e.printStackTrace();
            return buildSimpleResponse("500 Internal Server Error", "", headers);
        }
    }

    private String getFilePath(String route) {
        String relativePath = route.substring("/files/".length());
        return Paths.get(filesRootDirectory, relativePath).toString();
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
        }
        return contentBuilder.toString();
    }

    // Handle echo request
    private String handleEcho(String route, Map<String, String> headers) {
        String param = route.substring("/echo/".length());
        return buildResponseWithContent("200 OK", "text/plain", param, headers);
    }

    // Helper methods for building responses
    private String buildSimpleResponse(String status, String body, Map<String, String> headers) {
        return buildResponseWithContent(status, "text/plain", body, headers);
    }

    private String buildResponseWithContent(String status, String contentType, String content, Map<String, String> headers) {
        return buildResponseWithContent(status, contentType, content, headers, content.length());
    }

    private String buildResponseWithContent(String status, String contentType, String content, Map<String, String> headers, long contentLength) {

        return new StringBuilder()
                .append("HTTP/1.1 ").append(status).append("\r\n")
                .append("Content-Type: ").append(contentType).append("\r\n")
                .append("Content-Length: ").append(contentLength).append("\r\n")
                .append("\r\n")
                .append(content)
                .toString();
    }
}
