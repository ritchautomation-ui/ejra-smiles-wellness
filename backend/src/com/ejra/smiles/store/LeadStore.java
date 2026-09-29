package com.ejra.smiles.store;

import com.ejra.smiles.model.Lead;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * LeadStore.java
 * --------------
 * Holds every lead in TWO structures that are always kept in sync:
 *   1. Lead[]   - fixed-size array (max 100) that stores the records
 *   2. LeadBST  - custom Binary Search Tree keyed by Lead ID
 *
 * Every add / delete goes through this class so the array and the BST
 * can never disagree.
 *
 * Errors (turned into HTTP codes by ApiHandler):
 *   IllegalArgumentException -> 400 invalid / missing input
 *   IllegalStateException    -> 409 duplicate ID or storage full
 *   NoSuchElementException   -> 404 lead not found
 */
public class LeadStore {

    public static final int CAPACITY = 100;

    private final Lead[] leads = new Lead[CAPACITY];
    private int count = 0;
    private final LeadBST bst = new LeadBST();
    private int nextId = 1001;   // used when no Lead ID is supplied (public website form)

    // ------------------------------------------------------------- ADD
    /** Adds a lead with an automatically generated Lead ID. */
    public synchronized Lead addLead(String name, String email, String phone, String service,
                                     String date, String time, String notes,
                                     String source, String status) {
        return addLead(nextId, name, email, phone, service, date, time, notes, source, status);
    }

    /** Adds a lead with a specific Lead ID. Inserts into the array AND the BST. */
    public synchronized Lead addLead(int id, String name, String email, String phone, String service,
                                     String date, String time, String notes,
                                     String source, String status) {
        name = clean(name); email = clean(email); phone = clean(phone);
        service = clean(service); date = clean(date); time = clean(time);
        notes = clean(notes); source = clean(source); status = clean(status);

        if (id <= 0)               throw new IllegalArgumentException("Lead ID must be a positive whole number.");
        if (name.isEmpty())        throw new IllegalArgumentException("Patient name is required.");
        if (email.isEmpty())       throw new IllegalArgumentException("Email is required.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                                   throw new IllegalArgumentException("Email address is not valid.");
        if (phone.isEmpty())       throw new IllegalArgumentException("Phone number is required.");
        if (!phone.matches("^[0-9+\\-\\s()]{7,20}$"))
                                   throw new IllegalArgumentException("Phone number may only contain digits, +, -, spaces and brackets (7-20 characters).");
        if (service.isEmpty())     throw new IllegalArgumentException("Service is required.");
        if (date.isEmpty())        throw new IllegalArgumentException("Preferred date is required.");
        if (!date.matches("^\\d{4}-\\d{2}-\\d{2}$"))
                                   throw new IllegalArgumentException("Preferred date must look like YYYY-MM-DD.");
        if (time.isEmpty())        throw new IllegalArgumentException("Preferred time is required.");
        if (source.isEmpty())      source = Lead.SOURCE_WEBSITE_FORM;
        if (status.isEmpty())      status = Lead.STATUS_NEW;
        if (!contains(Lead.SOURCES, source))   throw new IllegalArgumentException("Invalid source: " + source);
        if (!contains(Lead.STATUSES, status))  throw new IllegalArgumentException("Invalid status: " + status);

        if (bst.search(id) != null)
            throw new IllegalStateException("Duplicate Lead ID: " + id + " already exists.");
        if (count >= CAPACITY)
            throw new IllegalStateException("Lead storage is full (" + CAPACITY + " leads maximum).");

        Lead lead = new Lead(id, name, email, phone, service, date, time, notes,
                             source, status, LocalDate.now().toString());
        leads[count++] = lead;      // 1) store in the array
        bst.insert(lead);           // 2) insert in the BST
        if (id >= nextId) nextId = id + 1;
        return lead;
    }

    // ---------------------------------------------------------- SEARCH
    /** Search by Lead ID using the BST. Returns null when not found. */
    public synchronized Lead findById(int id) { return bst.search(id); }

    public synchronized List<Integer> searchPath(int id) { return bst.searchPath(id); }

    // ---------------------------------------------------------- DELETE
    /** Removes the lead from the array AND the BST. */
    public synchronized Lead deleteLead(int id) {
        Lead lead = bst.search(id);
        if (lead == null) throw new NoSuchElementException("Lead ID " + id + " was not found.");
        int index = -1;
        for (int i = 0; i < count; i++) {
            if (leads[i].getLeadId() == id) { index = i; break; }
        }
        if (index >= 0) {
            for (int i = index; i < count - 1; i++) leads[i] = leads[i + 1];   // shift left
            leads[--count] = null;
        }
        bst.delete(id);
        return lead;
    }

    // ---------------------------------------------------------- UPDATE
    public synchronized Lead updateStatus(int id, String status) {
        status = clean(status);
        if (!contains(Lead.STATUSES, status)) throw new IllegalArgumentException("Invalid status: " + status);
        Lead lead = bst.search(id);
        if (lead == null) throw new NoSuchElementException("Lead ID " + id + " was not found.");
        lead.setStatus(status);
        return lead;
    }

    // ------------------------------------------------------------ VIEW
    /** All leads in the order they were added (array order). */
    public synchronized Lead[] getAll() {
        Lead[] copy = new Lead[count];
        System.arraycopy(leads, 0, copy, 0, count);
        return copy;
    }

    public synchronized int getCount() { return count; }

    // ------------------------------------------------------ BST ACCESS
    public synchronized List<Lead> traverse(String type) {
        switch (type) {
            case "inorder":   return bst.inorder();
            case "preorder":  return bst.preorder();
            case "postorder": return bst.postorder();
            default: throw new IllegalArgumentException("Invalid traversal '" + type
                                 + "'. Use inorder, preorder or postorder.");
        }
    }
    public synchronized String treeJson()   { return bst.toJson(); }
    public synchronized int treeHeight()    { return bst.height(); }
    public synchronized int treeSize()      { return bst.size(); }
    /** True when the array and the BST hold the same number of leads. */
    public synchronized boolean inSync()    { return bst.size() == count; }

    // ----------------------------------------------------------- STATS
    public synchronized String statsJson() {
        int n = 0, c = 0, a = 0, v = 0;
        for (int i = 0; i < count; i++) {
            String s = leads[i].getStatus();
            if (s.equals(Lead.STATUS_NEW)) n++;
            else if (s.equals(Lead.STATUS_CONTACTED)) c++;
            else if (s.equals(Lead.STATUS_APPOINTMENT_REQUESTED)) a++;
            else if (s.equals(Lead.STATUS_CONVERTED)) v++;
        }
        return "{\"total\":" + count + ",\"new\":" + n + ",\"contacted\":" + c
             + ",\"appointmentRequested\":" + a + ",\"converted\":" + v
             + ",\"capacity\":" + CAPACITY
             + ",\"bstSize\":" + bst.size() + ",\"bstHeight\":" + bst.height()
             + ",\"inSync\":" + inSync() + "}";
    }

    // --------------------------------------------------------- HELPERS
    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static boolean contains(String[] arr, String v) {
        for (String s : arr) if (s.equals(v)) return true;
        return false;
    }
}
