package it.polimi.tiw.ria.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Static class containing helpers for generating JWTs
 */
public class AuthUtils {
    private AuthUtils() {
    }

    /**
     * Create a new access token for the specified user
     *
     * @param userId the user id
     * @param iss    the token issuer
     * @param secret the token secret
     * @return a new token
     * @throws NullPointerException if any argument is null
     */
    public static String newToken(String userId, String iss, String secret) {
        return newToken(userId, iss, secret, 10 * 60);
    }

    /**
     * Create a new access token for the specified user with the given duration in seconds.
     *
     * @param userId  the user id
     * @param iss     the token issuer
     * @param secret  the token secret
     * @param seconds the maximum age in seconds
     * @return a new token
     * @throws NullPointerException if any argument is null
     */
    public static String newToken(String userId, String iss, String secret, long seconds) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(iss);
        Objects.requireNonNull(secret);
        Algorithm alg = Algorithm.HMAC256(secret);
        return JWT.create()
                .withIssuer(iss)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(seconds)))
                .withClaim("userId", userId)
                .sign(alg);
    }

    /**
     * Verify the given token, checking that it corresponds to the given user, then return it.
     *
     * @param token  the token to verify
     * @param userId the user id
     * @param iss    the issuer
     * @param secret the secret
     * @return the decoded jwt
     * @throws JWTVerificationException if verification failed
     * @throws NullPointerException     if any argument is null
     */
    public static DecodedJWT verifyToken(String token, String userId, String iss, String secret) {
        Objects.requireNonNull(token);
        Objects.requireNonNull(iss);
        Objects.requireNonNull(secret);
        Algorithm alg = Algorithm.HMAC256(secret);
        return JWT.require(alg)
                .withIssuer(iss)
                .withClaim("userId", userId)
                .build()
                .verify(token);
    }

    /**
     * Create a new refresh token for the given user. The refresh token will have a duration of 1 day.
     *
     * @param userId the user id
     * @param iss    the issuer
     * @param secret the secret
     * @return a new refresh token
     * @throws NullPointerException if any argument is null
     */
    public static String newRefreshToken(String userId, String iss, String secret) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(iss);
        Objects.requireNonNull(secret);
        SecureRandom rand = new SecureRandom();
        byte[] salt = new byte[64];
        rand.nextBytes(salt);
        Algorithm alg = Algorithm.HMAC256(secret);
        return JWT.create()
                .withIssuer(iss)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60 * 60 * 24)))
                .withClaim("userId", userId)
                .withClaim("rand", HexFormat.of().formatHex(salt))
                .sign(alg);
    }

    /**
     * Verify the validity of the given refresh token and return it.
     *
     * @param token  the token
     * @param iss    the issuer
     * @param secret the secret
     * @return the decoded jwt
     * @throws NullPointerException if any argument is null
     */
    public static DecodedJWT verifyRefreshToken(String token, String iss, String secret) {
        Objects.requireNonNull(token);
        Objects.requireNonNull(iss);
        Objects.requireNonNull(secret);
        Algorithm alg = Algorithm.HMAC256(secret);
        return JWT.require(alg)
                .withIssuer(iss)
                .build()
                .verify(token);
    }
}
