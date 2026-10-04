package com.jandoc.library.service;

import com.jandoc.library.config.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles CRUD and authentication for the "users" collection.
 * Borrowers are classified as Student, Visitor, or Employee.
 */
public class UserService {

    public static final String ROLE_STUDENT = "Student";
    public static final String ROLE_VISITOR = "Visitor";
    public static final String ROLE_EMPLOYEE = "Employee";

    private final MongoCollection<Document> userCollection;

    public UserService() {
        MongoDatabase db = MongoConfig.getDatabase();
        this.userCollection = db.getCollection("users");
    }

    /** Simple SHA-256 hash so passwords are never stored in plain text. */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean emailExists(String email) {
        return userCollection.find(Filters.eq("email", email)).first() != null;
    }

    // Create: Register a new user/borrower
    public boolean registerUser(String name, String email, String password, String role) {
        if (emailExists(email)) {
            return false;
        }
        Document user = new Document("name", name)
                .append("email", email)
                .append("password", hashPassword(password))
                .append("role", role)
                .append("borrowedBooks", new ArrayList<Document>());

        userCollection.insertOne(user);
        return true;
    }

    // Read: Authenticate login credentials; returns the user document or null
    public Document authenticate(String email, String password) {
        Document user = userCollection.find(Filters.eq("email", email)).first();
        if (user == null) return null;
        String stored = user.getString("password");
        if (stored != null && stored.equals(hashPassword(password))) {
            return user;
        }
        return null;
    }

    // Read: Get all users, optionally filtered by a search term
    public List<Document> getAllUsers(String searchTerm) {
        List<Document> results = new ArrayList<>();
        Iterable<Document> iterable;

        if (searchTerm == null || searchTerm.isBlank()) {
            iterable = userCollection.find();
        } else {
            String regex = ".*" + java.util.regex.Pattern.quote(searchTerm) + ".*";
            iterable = userCollection.find(Filters.or(
                    Filters.regex("name", regex, "i"),
                    Filters.regex("email", regex, "i"),
                    Filters.regex("role", regex, "i")
            ));
        }
        for (Document d : iterable) {
            results.add(d);
        }
        return results;
    }

    public Document getUserById(ObjectId id) {
        return userCollection.find(Filters.eq("_id", id)).first();
    }

    // Update: Modify a user's core details (does not touch password unless provided)
    public void updateUser(ObjectId id, String name, String email, String role, String newPassword) {
        List<org.bson.conversions.Bson> updates = new ArrayList<>();
        updates.add(Updates.set("name", name));
        updates.add(Updates.set("email", email));
        updates.add(Updates.set("role", role));
        if (newPassword != null && !newPassword.isBlank()) {
            updates.add(Updates.set("password", hashPassword(newPassword)));
        }
        userCollection.updateOne(Filters.eq("_id", id), Updates.combine(updates.toArray(new org.bson.conversions.Bson[0])));
    }

    // Delete: Remove a user by ObjectId
    public void deleteUser(ObjectId id) {
        userCollection.deleteOne(Filters.eq("_id", id));
    }
}
