package com.ejra.smiles.server;

import com.ejra.smiles.model.Lead;
import com.ejra.smiles.store.LeadStore;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * ApiHandler.java
 * ---------------
 * The REST API. The frontend (Vercel or localhost) talks to this class.
 *
 *   GET    /api/health
 *   GET    /api/leads                 view all leads (array order)
 *   POST   /api/leads                 add a lead  (array + BST insert)
 *   GET    /api/leads/{id}            search by Lead ID (BST search)
 *   PUT    /api/leads/{id}/status     change a lead's status
 *   DELETE /api/leads/{id}            delete (array + BST delete)
 *   GET    /api/stats                 dashboard numbers
 *   GET    /api/bst/inorder | preorder | postorder
 *   GET    /api/bst/tree              tree structure for the diagram
 *
 * CORS: the allowed origin comes from the ALLOWED_ORIGIN environment variable.
 * If it is not set, "*" (everyone) is allowed. See README "CORS".
 */
public class ApiHandler implements HttpHandler {

    private final LeadStore store;
    private final String allowedOrigin;

    public ApiHandler(LeadStore store) {
        this.store = store;
        String env = System.getenv("ALLOWED_ORIGIN");
        // >>> To restrict later, set ALLOWED_ORIGIN=https://YOUR-VERCEL-DOMAIN.vercel.app
        //     (Railway -> Variables), or replace "*" below. <<<
        this.allowedOrigin = (env == null || env.trim().isEmpty()) ? "*" : env.trim();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // ---- CORS headers on EVERY response (including errors) ----
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", allowedOrigin);
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        h.set("Access-Control-Max-Age", "86400");
        if (!"*".equals(allowedOrigin)) h.set("Vary", "Origin");

        // ---- Preflight request from the browser ----
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }

        try {
            route(ex);
        } catch (NoSuchElementException e) {
            error(ex, 404, e.getMessage());
        } catch (IllegalStateException e) {
            error(ex, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            error(ex, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            error(ex, 500, "Unexpected server error. Please try again.");
        }
    }

    // ============================================================ ROUTER
    private void route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String path = ex.getRequestURI().getPath();
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        String[] p = path.split("/");     // ["", "api", "leads", "1001", ...]
        String res = p.length > 2 ? p[2] : "";

        if (res.equals("health") && method.equals("GET")) {
            ok(ex, 200, "{\"success\":true,\"status\":\"running\"}");

        } else if (res.equals("leads") && p.length == 3) {
            if (method.equals("GET"))       viewAll(ex);
            else if (method.equals("POST")) addLead(ex);
            else notAllowed(ex);

        } else if (res.equals("leads") && p.length == 4) {
            int id = parseId(p[3]);
            if (method.equals("GET"))         searchLead(ex, id);
            else if (method.equals("DELETE")) deleteLead(ex, id);
            else notAllowed(ex);

        } else if (res.equals("leads") && p.length == 5 && p[4].equals("status")) {
            if (!method.equals("PUT")) { notAllowed(ex); return; }
            Map<String, String> b = parseJson(readBody(ex));
            Lead l = store.updateStatus(parseId(p[3]), b.get("status"));
            ok(ex, 200, "{\"success\":true,\"message\":\"Status updated.\",\"lead\":" + l.toJson() + "}");

        } else if (res.equals("stats") && method.equals("GET")) {
            ok(ex, 200, "{\"success\":true,\"stats\":" + store.statsJson() + "}");

        } else if (res.equals("bst") && p.length == 4 && method.equals("GET")) {
            bst(ex, p[3]);

        } else {
            error(ex, 404, "Unknown API route: " + method + " " + path);
        }
    }

    // ========================================================== ENDPOINTS
    private void viewAll(HttpExchange ex) throws IOException {
        Lead[] all = store.getAll();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":" + all.length + ",\"leads\":[");
        for (int i = 0; i < all.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(all[i].toJson());
        }
        ok(ex, 200, sb.append("]}").toString());
    }

    private void addLead(HttpExchange ex) throws IOException {
        Map<String, String> b = parseJson(readBody(ex));
        String rawId = b.get("leadId");
        Lead lead;
        if (rawId == null || rawId.trim().isEmpty() || rawId.equals("null")) {
            lead = store.addLead(b.get("name"), b.get("email"), b.get("phone"), b.get("service"),
                    b.get("preferredDate"), b.get("preferredTime"), b.get("notes"),
                    b.get("source"), b.get("status"));            // auto Lead ID
        } else {
            lead = store.addLead(parseId(rawId), b.get("name"), b.get("email"), b.get("phone"),
                    b.get("service"), b.get("preferredDate"), b.get("preferredTime"),
                    b.get("notes"), b.get("source"), b.get("status"));
        }
        ok(ex, 201, "{\"success\":true,\"message\":\"Lead " + lead.getLeadId()
                + " added and inserted into the BST.\",\"lead\":" + lead.toJson() + "}");
    }

    private void searchLead(HttpExchange ex, int id) throws IOException {
        Lead lead = store.findById(id);                       // BST search
        if (lead == null) throw new NoSuchElementException("Lead ID " + id + " was not found in the BST.");
        ok(ex, 200, "{\"success\":true,\"lead\":" + lead.toJson()
                + ",\"path\":" + intList(store.searchPath(id)) + "}");
    }

    private void deleteLead(HttpExchange ex, int id) throws IOException {
        Lead lead = store.deleteLead(id);                     // array + BST delete
        ok(ex, 200, "{\"success\":true,\"message\":\"Lead " + id + " (" + Lead.esc(lead.getName())
                + ") was deleted from the array and the BST.\"}");
    }

    private void bst(HttpExchange ex, String what) throws IOException {
        if (what.equals("tree")) {
            ok(ex, 200, "{\"success\":true,\"size\":" + store.treeSize() + ",\"height\":"
                    + store.treeHeight() + ",\"tree\":" + store.treeJson() + "}");
            return;
        }
        List<Lead> order = store.traverse(what);              // throws 400 if invalid
        StringBuilder ids = new StringBuilder("[");
        StringBuilder leads = new StringBuilder("[");
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) { ids.append(','); leads.append(','); }
            ids.append(order.get(i).getLeadId());
            leads.append("{\"leadId\":").append(order.get(i).getLeadId())
                 .append(",\"name\":\"").append(Lead.esc(order.get(i).getName())).append("\"}");
        }
        ok(ex, 200, "{\"success\":true,\"traversal\":\"" + what + "\",\"count\":" + order.size()
                + ",\"ids\":" + ids + "],\"leads\":" + leads + "]}");
    }

    // ============================================================ HELPERS
    private static int parseId(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Lead ID must be a whole number (example: 1006).");
        }
    }

    private static String intList(List<Integer> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) { if (i > 0) sb.append(','); sb.append(list.get(i)); }
        return sb.append(']').toString();
    }

    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void ok(HttpExchange ex, int code, String json) throws IOException { send(ex, code, json); }

    private void error(HttpExchange ex, int code, String msg) throws IOException {
        send(ex, code, "{\"success\":false,\"error\":\"" + Lead.esc(msg) + "\"}");
    }

    private void notAllowed(HttpExchange ex) throws IOException { error(ex, 405, "Method not allowed."); }

    private void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    // ================================================ TINY JSON PARSER
    /** Parses a FLAT JSON object like {"name":"Ana","leadId":5} into a map of strings. */
    static Map<String, String> parseJson(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException("Request body is empty.");
        int[] i = {0};
        skipWs(s, i);
        if (i[0] >= s.length() || s.charAt(i[0]) != '{') throw new IllegalArgumentException("Invalid JSON body.");
        i[0]++;
        while (true) {
            skipWs(s, i);
            if (i[0] >= s.length()) throw new IllegalArgumentException("Invalid JSON body.");
            if (s.charAt(i[0]) == '}') break;
            if (s.charAt(i[0]) != '"') throw new IllegalArgumentException("Invalid JSON body.");
            String key = readString(s, i);
            skipWs(s, i);
            if (i[0] >= s.length() || s.charAt(i[0]) != ':') throw new IllegalArgumentException("Invalid JSON body.");
            i[0]++;
            skipWs(s, i);
            String value;
            if (i[0] < s.length() && s.charAt(i[0]) == '"') {
                value = readString(s, i);
            } else {
                int start = i[0];
                while (i[0] < s.length() && s.charAt(i[0]) != ',' && s.charAt(i[0]) != '}') i[0]++;
                value = s.substring(start, i[0]).trim();
            }
            map.put(key, value);
            skipWs(s, i);
            if (i[0] < s.length() && s.charAt(i[0]) == ',') i[0]++;
        }
        return map;
    }

    private static void skipWs(String s, int[] i) {
        while (i[0] < s.length() && Character.isWhitespace(s.charAt(i[0]))) i[0]++;
    }

    private static String readString(String s, int[] i) {
        StringBuilder sb = new StringBuilder();
        i[0]++;                                   // skip opening quote
        while (i[0] < s.length()) {
            char c = s.charAt(i[0]++);
            if (c == '"') return sb.toString();
            if (c == '\\' && i[0] < s.length()) {
                char n = s.charAt(i[0]++);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u':
                        if (i[0] + 4 > s.length()) throw new IllegalArgumentException("Invalid JSON body.");
                        try { sb.append((char) Integer.parseInt(s.substring(i[0], i[0] + 4), 16)); }
                        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid JSON body."); }
                        i[0] += 4; break;
                    default: sb.append(n);
                }
            } else sb.append(c);
        }
        throw new IllegalArgumentException("Invalid JSON body.");
    }
}
