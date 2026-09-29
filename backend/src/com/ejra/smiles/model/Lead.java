package com.ejra.smiles.model;

/**
 * Lead.java
 * ---------
 * One patient lead / appointment request. The Lead ID (an int) is the key
 * used by the Binary Search Tree.
 */
public class Lead {

    // Allowed values (kept as constants so Main and validation agree)
    public static final String SOURCE_WEBSITE_FORM = "Website Form";
    public static final String SOURCE_FACEBOOK     = "Facebook";
    public static final String SOURCE_INSTAGRAM    = "Instagram";
    public static final String SOURCE_WALK_IN      = "Walk-in";

    public static final String STATUS_NEW                   = "New";
    public static final String STATUS_CONTACTED             = "Contacted";
    public static final String STATUS_APPOINTMENT_REQUESTED = "Appointment Requested";
    public static final String STATUS_CONVERTED             = "Converted";

    public static final String[] SOURCES = {
        SOURCE_WEBSITE_FORM, SOURCE_FACEBOOK, SOURCE_INSTAGRAM, SOURCE_WALK_IN };
    public static final String[] STATUSES = {
        STATUS_NEW, STATUS_CONTACTED, STATUS_APPOINTMENT_REQUESTED, STATUS_CONVERTED };

    private final int leadId;
    private final String name;
    private final String email;
    private final String phone;
    private final String service;
    private final String preferredDate;
    private final String preferredTime;
    private final String notes;
    private final String source;
    private String status;
    private final String dateSubmitted;

    public Lead(int leadId, String name, String email, String phone, String service,
                String preferredDate, String preferredTime, String notes,
                String source, String status, String dateSubmitted) {
        this.leadId = leadId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.service = service;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.notes = notes;
        this.source = source;
        this.status = status;
        this.dateSubmitted = dateSubmitted;
    }

    public int getLeadId()          { return leadId; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getPhone()        { return phone; }
    public String getService()      { return service; }
    public String getPreferredDate(){ return preferredDate; }
    public String getPreferredTime(){ return preferredTime; }
    public String getNotes()        { return notes; }
    public String getSource()       { return source; }
    public String getStatus()       { return status; }
    public String getDateSubmitted(){ return dateSubmitted; }
    public void setStatus(String status) { this.status = status; }

    /** Escapes text so it can sit safely inside a JSON string. */
    public static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    public String toJson() {
        return "{\"leadId\":" + leadId
            + ",\"name\":\"" + esc(name) + "\""
            + ",\"email\":\"" + esc(email) + "\""
            + ",\"phone\":\"" + esc(phone) + "\""
            + ",\"service\":\"" + esc(service) + "\""
            + ",\"preferredDate\":\"" + esc(preferredDate) + "\""
            + ",\"preferredTime\":\"" + esc(preferredTime) + "\""
            + ",\"notes\":\"" + esc(notes) + "\""
            + ",\"source\":\"" + esc(source) + "\""
            + ",\"status\":\"" + esc(status) + "\""
            + ",\"dateSubmitted\":\"" + esc(dateSubmitted) + "\"}";
    }
}
