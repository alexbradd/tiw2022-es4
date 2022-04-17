package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.exceptions.UpdateException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represents a User.
 */
public class User implements DatabaseAccessObject {
    private Long id;
    private String username;
    private String saltedPassword;
    private String email;
    private String name;
    private String surname;

    /**
     * The ConnectionRetriever object used to get new connections to the database
     */
    protected static ConnectionRetriever retriever = ProductionConnectionRetriever.getInstance();

    /**
     * The {@link Pattern} used for validating correctness of the usernames passed.
     */
    public static final Pattern USERNAME_REGEX = Pattern.compile("^[a-z0-9_-]{3,20}$", Pattern.CASE_INSENSITIVE);

    /**
     * The {@link Pattern} used for validating correctness of the emails passed.
     */
    public static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}",
            Pattern.CASE_INSENSITIVE);

    /**
     * Creates a new User with the given properties. All strings have a maximum length of 128 characters. The User will
     * gain an id only after being saved to the database. If you need to retrieve an existing user, use
     * {@link #byUsername(String)} or {@link #byId(String)}.
     *
     * @param username      this user's username (see {@link #USERNAME_REGEX})
     * @param clearPassword this user's plain-text password
     * @param email         this user's email (see {@link #EMAIL_REGEX})
     * @param name          this user's name
     * @param surname       this user's surname
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if any parameter is formatted incorrectly.
     */
    public User(String username, String clearPassword, String email, String name, String surname) {
        Objects.requireNonNull(username, "username is required");
        Objects.requireNonNull(clearPassword, "clearPassword is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(surname, "surname is required");

        this.id = null;
        setUsername(username);
        setSaltedPassword(clearPassword);
        setEmail(email);
        setName(name);
        setSurname(surname);
    }

    /**
     * <= 128
     */
    private static void checkLength(String s) {
        if (s.length() > 128)
            throw new IllegalArgumentException("Maximum string length exceeded for " + s);
    }

    /**
     * Crates a new user with the given parameters
     */
    private User(long id, String username, String saltedPassword, String email, String name, String surname) {
        this.id = id;
        this.username = username;
        this.saltedPassword = saltedPassword;
        this.email = email;
        this.name = name;
        this.surname = surname;
    }

    /**
     * Returns an {@link Optional} containing the Base64 encoded user id.
     *
     * @return an {@link Optional} containing the Base64 encoded user id
     */
    public Optional<String> getId() {
        if (!isPersisted()) return Optional.empty();
        return Optional.of(Base64.getUrlEncoder().withoutPadding().encodeToString(longToByteArray(id)));
    }

    /**
     * Returns this user's id as long, if it has one.
     *
     * @return this user's id as long
     */
    protected Optional<Long> getLongId() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns true if the User already exists in the database.
     *
     * @return true if the User already exists in the database.
     */
    public boolean isPersisted() {
        return id != null;
    }

    /**
     * Converts long to byte array
     */
    private static byte[] longToByteArray(long l) {
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            try (DataOutputStream d = new DataOutputStream(s)) {
                d.writeLong(l);
                d.flush();
                return s.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * Getter for the User's username
     *
     * @return the User's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets this User's username to the specified value.
     *
     * @param username the new username
     * @throws NullPointerException     if {@code username} is null
     * @throws IllegalArgumentException if {@code username} is too long or does not match {@link #USERNAME_REGEX}
     */
    public void setUsername(String username) {
        Objects.requireNonNull(username, "username is required");
        checkLength(username);
        if (!USERNAME_REGEX.matcher(username).find())
            throw new IllegalArgumentException(username + " is not a valid username");
        this.username = username;
    }

    /**
     * Calculates salt and hash of the given password and stores it in the database.
     *
     * @param clearPassword the clear text password
     * @throws NullPointerException if {@code clearPassword} is null
     */
    public void setSaltedPassword(String clearPassword) {
        Objects.requireNonNull(clearPassword, "clearPassword is required");

        byte[] salt = genSalt();
        this.saltedPassword = genDigest(clearPassword) + digestSalt(salt) + ':' + toHex(salt);
    }

    /**
     * Generates a random salt
     */
    private byte[] genSalt() {
        SecureRandom r = new SecureRandom();
        byte[] salt = new byte[64];
        r.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes with SHA-256 the string
     */
    private String genDigest(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AAAAA", e);
        }
    }

    /**
     * Converts the byte array into a string of hex numbers
     */
    private String toHex(byte[] array) {
        StringBuilder res = new StringBuilder();
        for (byte b : array)
            res.append(String.format("%02X", b));
        return res.toString();
    }

    /**
     * Calculates the hash of the salt
     */
    private String digestSalt(byte[] salt) {
        try {
            byte[] copy = salt.clone();
            MessageDigest.getInstance("SHA-256").update(copy);
            return toHex(copy);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AAAAA", e);
        }
    }

    /**
     * Getter for this User's email
     *
     * @return this User's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets this User's email to the given one
     *
     * @param email the new email
     * @throws NullPointerException     if {@code email} is null
     * @throws IllegalArgumentException if {@code email} doesn't match {@link #EMAIL_REGEX}.
     */
    public void setEmail(String email) {
        Objects.requireNonNull(email, "email is required");
        checkLength(email);
        if (!EMAIL_REGEX.matcher(email).find())
            throw new IllegalArgumentException(email + " is not a valid email");
        this.email = email;
    }

    /**
     * Getter for this User's name
     *
     * @return this User's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets this User's name to the given one
     *
     * @param name the new name
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is too long
     */
    public void setName(String name) {
        Objects.requireNonNull(name, "name is required");
        checkLength(name);
        this.name = name;
    }

    /**
     * Getter for this User's surname
     *
     * @return this User's surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets this User's name to the given one
     *
     * @param surname the new name
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is too long
     */
    public void setSurname(String surname) {
        Objects.requireNonNull(surname, "surname is required");
        checkLength(surname);
        this.surname = surname;
    }

    /**
     * Saves the current User to database. If the User already existed, the updates are stored.
     *
     * @throws UpdateException if any modification is illegal (e.g. duplicated usernames)
     */
    public void save() {
        try (Connection c = retriever.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (isPersisted()) updateUser(c);
                else saveNewUser(c);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new UpdateException(e);
        }
    }

    /**
     * Updates the already existing user represented by this object
     */
    private void updateUser(Connection c) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "update tiw_app.users set username = ?, password = ?, email = ?, name = ?, surname = ? where id = ?")) {
            injectStringParameters(p, username, saltedPassword, email, name, surname);
            p.setLong(6, getLongId().orElseThrow(IllegalStateException::new));
            p.executeUpdate();
        }
    }

    /**
     * Save a new user with his object's properties into the database
     */
    private void saveNewUser(Connection c) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "insert into tiw_app.users(username, password, email, name, surname) values (?, ?, ?, ?, ?)")) {
            injectStringParameters(p, username, saltedPassword, email, name, surname);
            p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement("select id from tiw_app.users where username = ?")) {
            injectStringParameters(p, username);
            ResultSet r = p.executeQuery();
            r.next();
            this.id = r.getLong("id");
        }
    }

    /**
     * Injects strings into PreparedStatement
     */
    private static void injectStringParameters(PreparedStatement p, String... vars) throws SQLException {
        for (int i = 0; i < vars.length; i++)
            p.setString(i + 1, vars[i]);
    }

    /**
     * Finds and retrieves the data for the User with the given username. If no such user can be found, an empty
     * {@link Optional} is returned.
     *
     * @param username the username to search
     * @return an {@link Optional} containing the constructed User
     * @throws NullPointerException if {@code username} is null
     */
    public static Optional<User> byUsername(String username) {
        Objects.requireNonNull(username, "username is required");
        if (!USERNAME_REGEX.matcher(username).find())
            throw new IllegalArgumentException(username + " is not a valid username");
        try (Connection c = retriever.getConnection()) {
            try (PreparedStatement p = c.prepareStatement("select * from tiw_app.users where username = ?")) {
                injectStringParameters(p, username);
                return packageUserOptional(p);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("an error occurred while fetching data", e);
        }
    }

    /**
     * Finds and retrieves the data for the User with the given username. If no such user can be found, an empty
     * {@link Optional} is returned.
     *
     * @param id the Base64 encoded user id to search
     * @return an {@link Optional} containing the constructed User
     * @throws NullPointerException if {@code username} is null
     */
    public static Optional<User> byId(String id) {
        Objects.requireNonNull(id, "id is required");
        long lId = byteArrayToLong(Base64.getUrlDecoder().decode(id));
        try (Connection c = retriever.getConnection()) {
            try (PreparedStatement p = c.prepareStatement("select * from tiw_app.users where id = ?")) {
                p.setLong(1, lId);
                return packageUserOptional(p);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("an error occurred while fetching data", e);
        }
    }

    /**
     * Executes p and wraps the result in an Optional
     */
    private static Optional<User> packageUserOptional(PreparedStatement p) throws SQLException {
        try (ResultSet r = p.executeQuery()) {
            if (r.next()) {
                User found = new User(
                        r.getLong("id"),
                        r.getString("username"),
                        r.getString("password"),
                        r.getString("email"),
                        r.getString("name"),
                        r.getString("surname")
                );
                return Optional.of(found);
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * Converts a byte array into a long
     */
    private static long byteArrayToLong(byte[] longBytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.put(longBytes);
        byteBuffer.flip();
        return byteBuffer.getLong();
    }
}
