package com.ejra.smiles;

import com.ejra.smiles.model.Lead;
import com.ejra.smiles.server.ApiHandler;
import com.ejra.smiles.server.StaticFileHandler;
import com.ejra.smiles.store.LeadStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Main.java
 * ---------
 * Entry point for EJRA Smiles & Wellness (Java backend).
 *
 * LOCAL:    java -cp out com.ejra.smiles.Main   (run from the project root)
 *   http://localhost:8080/          -> public website   (served from /frontend)
 *   http://localhost:8080/admin/    -> Lead Catcher admin dashboard
 *   http://localhost:8080/api/...   -> Java API (Lead[] array + BST)
 *
 * RAILWAY:  the same class runs inside Docker. Railway provides the PORT
 *           environment variable; the API is what the Vercel frontend calls.
 *
 * Compile:
 *   javac -d out $(find backend/src -name "*.java")
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // Railway sets PORT. Locally it falls back to 8080.
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        LeadStore leadStore = new LeadStore();
        seedSampleLeads(leadStore);

        // Listen on 0.0.0.0 so Railway (and Docker) can reach the server.
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Local development: serve the frontend folder if it exists.
        File frontend = new File("frontend").getAbsoluteFile();
        if (frontend.isDirectory()) {
            server.createContext("/", new StaticFileHandler(frontend));
        } else {
            // Railway: backend only. A friendly message at the root URL.
            server.createContext("/", (HttpExchange ex) -> {
                byte[] msg = ("EJRA Smiles & Wellness API is running. Try /api/leads").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(200, msg.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(msg); }
            });
        }

        // Java backend API (Lead[] array + LeadBST)
        server.createContext("/api", new ApiHandler(leadStore));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("=================================================");
        System.out.println(" EJRA Smiles & Wellness is running on port " + port);
        System.out.println(" API     : http://localhost:" + port + "/api/leads");
        if (frontend.isDirectory()) {
            System.out.println(" Website : http://localhost:" + port + "/");
            System.out.println(" Admin   : http://localhost:" + port + "/admin/");
        }
        System.out.println("=================================================");
    }

    /**
     * Pre-loads sample leads so the dashboard is not empty on first run.
     * The Lead IDs are chosen so the BST is nicely balanced (root 1003), which
     * makes Inorder / Preorder / Postorder look different and easy to explain.
     */
    private static void seedSampleLeads(LeadStore store) {
        store.addLead(1003, "Juan Dela Cruz", "juan@email.com", "09171234567",
                "Dental Cleaning", "2026-10-05", "10:00 AM",
                "Regular cleaning", Lead.SOURCE_WEBSITE_FORM, Lead.STATUS_NEW);
        store.addLead(1001, "Maria Santos", "maria.santos@email.com", "09182223344",
                "Teeth Whitening", "2026-10-08", "2:00 PM",
                "Wedding is coming up", Lead.SOURCE_FACEBOOK, Lead.STATUS_CONTACTED);
        store.addLead(1005, "Pedro Reyes", "pedro.reyes@email.com", "09193334455",
                "Dental Checkup", "2026-09-30", "9:00 AM",
                "Annual checkup", Lead.SOURCE_WALK_IN, Lead.STATUS_CONVERTED);
        store.addLead(1002, "Angela Bautista", "angela.b@email.com", "09204445566",
                "Root Canal Treatment", "2026-10-12", "11:30 AM",
                "Tooth pain for a week", Lead.SOURCE_WEBSITE_FORM,
                Lead.STATUS_APPOINTMENT_REQUESTED);
        store.addLead(1004, "Mark Villanueva", "mark.v@email.com", "09215556677",
                "Orthodontic Consultation", "2026-10-15", "3:30 PM",
                "Interested in braces", Lead.SOURCE_INSTAGRAM, Lead.STATUS_NEW);
    }
}
