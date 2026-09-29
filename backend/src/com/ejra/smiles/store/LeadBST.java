package com.ejra.smiles.store;

import com.ejra.smiles.model.Lead;
import java.util.ArrayList;
import java.util.List;

/**
 * LeadBST.java
 * ------------
 * CUSTOM Binary Search Tree (no java.util tree classes) that organizes leads
 * by Lead ID.
 *
 * BST rule:  every key in a node's LEFT subtree  <  the node's key
 *            every key in a node's RIGHT subtree >  the node's key
 * Duplicate Lead IDs are not allowed.
 */
public class LeadBST {

    /** One tree node: the key (Lead ID), the Lead itself, and two children. */
    private static class Node {
        int key;
        Lead lead;
        Node left, right;
        Node(Lead lead) { this.key = lead.getLeadId(); this.lead = lead; }
    }

    private Node root;
    private int size;

    // ------------------------------------------------------------ INSERT
    /** Inserts a lead. Returns false (and changes nothing) if the ID already exists. */
    public boolean insert(Lead lead) {
        if (search(lead.getLeadId()) != null) return false;
        root = insert(root, lead);
        size++;
        return true;
    }

    private Node insert(Node node, Lead lead) {
        if (node == null) return new Node(lead);              // found the empty spot
        if (lead.getLeadId() < node.key) node.left  = insert(node.left, lead);
        else                             node.right = insert(node.right, lead);
        return node;
    }

    // ------------------------------------------------------------ SEARCH
    /** Returns the lead with this ID, or null if it is not in the tree. */
    public Lead search(int key) {
        Node cur = root;
        while (cur != null) {
            if (key == cur.key) return cur.lead;
            cur = (key < cur.key) ? cur.left : cur.right;      // go left or right
        }
        return null;
    }

    /** The Lead IDs visited while searching (shows the path taken from the root). */
    public List<Integer> searchPath(int key) {
        List<Integer> path = new ArrayList<>();
        Node cur = root;
        while (cur != null) {
            path.add(cur.key);
            if (key == cur.key) break;
            cur = (key < cur.key) ? cur.left : cur.right;
        }
        return path;
    }

    // ------------------------------------------------------------ DELETE
    /** Deletes the node with this ID. Returns false if it does not exist. */
    public boolean delete(int key) {
        if (search(key) == null) return false;
        root = delete(root, key);
        size--;
        return true;
    }

    private Node delete(Node node, int key) {
        if (node == null) return null;
        if (key < node.key)      node.left  = delete(node.left, key);
        else if (key > node.key) node.right = delete(node.right, key);
        else {
            // Case 1 & 2: no child, or one child -> replace node by that child
            if (node.left == null)  return node.right;
            if (node.right == null) return node.left;
            // Case 3: two children -> copy the INORDER SUCCESSOR
            // (smallest key of the right subtree), then delete that successor.
            Node succ = node.right;
            while (succ.left != null) succ = succ.left;
            node.key = succ.key;
            node.lead = succ.lead;
            node.right = delete(node.right, succ.key);
        }
        return node;
    }

    // -------------------------------------------------------- TRAVERSALS
    /** Left -> Root -> Right (ascending Lead ID order). */
    public List<Lead> inorder() {
        List<Lead> out = new ArrayList<>();
        inorder(root, out);
        return out;
    }
    private void inorder(Node n, List<Lead> out) {
        if (n == null) return;
        inorder(n.left, out);
        out.add(n.lead);
        inorder(n.right, out);
    }

    /** Root -> Left -> Right. */
    public List<Lead> preorder() {
        List<Lead> out = new ArrayList<>();
        preorder(root, out);
        return out;
    }
    private void preorder(Node n, List<Lead> out) {
        if (n == null) return;
        out.add(n.lead);
        preorder(n.left, out);
        preorder(n.right, out);
    }

    /** Left -> Right -> Root. */
    public List<Lead> postorder() {
        List<Lead> out = new ArrayList<>();
        postorder(root, out);
        return out;
    }
    private void postorder(Node n, List<Lead> out) {
        if (n == null) return;
        postorder(n.left, out);
        postorder(n.right, out);
        out.add(n.lead);
    }

    // ----------------------------------------------------------- HELPERS
    public int size()        { return size; }
    public boolean isEmpty() { return root == null; }
    public int height()      { return height(root); }
    private int height(Node n) { return n == null ? 0 : 1 + Math.max(height(n.left), height(n.right)); }

    /** Tree structure as nested JSON, used by the dashboard's tree diagram. */
    public String toJson() { return root == null ? "null" : toJson(root); }
    private String toJson(Node n) {
        return "{\"id\":" + n.key
            + ",\"name\":\"" + Lead.esc(n.lead.getName()) + "\""
            + ",\"left\":"  + (n.left  == null ? "null" : toJson(n.left))
            + ",\"right\":" + (n.right == null ? "null" : toJson(n.right)) + "}";
    }
}
