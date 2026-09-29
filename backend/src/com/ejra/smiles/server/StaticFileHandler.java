package com.ejra.smiles.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * StaticFileHandler.java
 * ----------------------
 * LOCAL DEVELOPMENT ONLY: serves the "frontend" folder so that
 *   http://localhost:8080/        -> frontend/public/index.html
 *   http://localhost:8080/admin/  -> frontend/admin/index.html
 * work exactly like they do on Vercel.
 * (On Railway the frontend folder is not copied, so this handler is not used.)
 */
public class StaticFileHandler implements HttpHandler {

    private final File root;

    public StaticFileHandler(File frontendRoot) { this.root = frontendRoot; }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/public/index.html";
        else if (path.equals("/admin") || path.equals("/admin/")) path = "/admin/index.html";
        else if (path.equals("/public") || path.equals("/public/")) path = "/public/index.html";

        File file = new File(root, path).getCanonicalFile();
        // Block "../" tricks that try to leave the frontend folder
        if (!file.getPath().startsWith(root.getCanonicalPath()) || !file.isFile()) {
            byte[] msg = "404 - File not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(msg); }
            return;
        }
        byte[] data = Files.readAllBytes(file.toPath());
        ex.getResponseHeaders().set("Content-Type", mime(file.getName()));
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static String mime(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css"))  return "text/css; charset=utf-8";
        if (n.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (n.endsWith(".json")) return "application/json; charset=utf-8";
        if (n.endsWith(".svg"))  return "image/svg+xml";
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
