package it.polimi.tiw.api.beans;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UserBuilderTest {
    @ParameterizedTest
    @MethodSource("build_invalidBuilderSource")
    void build_invalidParameters(InvalidBuilderTestCase test) {
        test.builder.build()
                .consume(__ -> fail(), e -> assertEquals(test.nErrors, e.errors().length));
    }

    static Stream<InvalidBuilderTestCase> build_invalidBuilderSource() {
        User.Builder b = new User.Builder();
        User.Builder withEmptyUsername = b.addUsername("");
        User.Builder withOverflownUsername = b.addUsername("a".repeat(200));
        User.Builder withUsername = b.addUsername("pippo");
        User.Builder withOnlyOnePassword = withUsername.addPassword("a", null);
        User.Builder withOnePasswordOneEmpty = withUsername.addPassword("a", "");
        User.Builder withOnePasswordOneOverflown = withUsername.addPassword("a", "a".repeat(200));
        User.Builder withMismatchedPassword = withUsername.addPassword("a", "b");
        User.Builder withMatchingPassword = withUsername.addPassword("a", "a");
        User.Builder withEmptySaltedPassword = withUsername.addPassword("");
        User.Builder withOverflownSaltedPassword = withUsername.addPassword("a".repeat(1000));
        User.Builder withEmptyEmail = withMatchingPassword.addEmail("");
        User.Builder withInvalidEmail = withMatchingPassword.addEmail("pippo(at)email.com");
        User.Builder withOverflownEmail = withMatchingPassword.addEmail("pippo(at)email.co" + "m".repeat(200));
        User.Builder withValidEmail = withMatchingPassword.addEmail("pippo@email.com");
        User.Builder withEmptyName = withValidEmail.addName("");
        User.Builder withOverflownName = withValidEmail.addName("a".repeat(200));
        User.Builder withName = withValidEmail.addName("Pippo");
        User.Builder withEmptySurname = withName.addSurname("");
        User.Builder withOverflownSurname = withName.addSurname("a".repeat(200));

        return Stream.of(
                new InvalidBuilderTestCase(b, 5, "empty"),
                new InvalidBuilderTestCase(withEmptyUsername, 5, "with empty username"),
                new InvalidBuilderTestCase(withOverflownUsername, 5, "with overflown username"),
                new InvalidBuilderTestCase(withUsername, 4, "with username"),
                new InvalidBuilderTestCase(withOnlyOnePassword, 5, "with only one password"),
                new InvalidBuilderTestCase(withOnePasswordOneEmpty, 5, "with one password and one empty"),
                new InvalidBuilderTestCase(withOnePasswordOneOverflown, 5, "with one password and one overflown"),
                new InvalidBuilderTestCase(withMismatchedPassword, 4, "with mismatched passwords"),
                new InvalidBuilderTestCase(withOverflownSaltedPassword, 4, "with overflown salted password"),
                new InvalidBuilderTestCase(withEmptySaltedPassword, 4, "with empty salted password"),
                new InvalidBuilderTestCase(withMatchingPassword, 3, "with matching passwords"),
                new InvalidBuilderTestCase(withEmptyEmail, 3, "with empty email"),
                new InvalidBuilderTestCase(withInvalidEmail, 3, "with invalid email"),
                new InvalidBuilderTestCase(withOverflownEmail, 3, "with overflown email"),
                new InvalidBuilderTestCase(withValidEmail, 2, "with valid email"),
                new InvalidBuilderTestCase(withEmptyName, 2, "with empty name"),
                new InvalidBuilderTestCase(withOverflownName, 2, "with overflown name"),
                new InvalidBuilderTestCase(withName, 1, "with name"),
                new InvalidBuilderTestCase(withEmptySurname, 1, "with empty surname"),
                new InvalidBuilderTestCase(withOverflownSurname, 1, "with overflown surname")
        );
    }

    private static class InvalidBuilderTestCase {
        public User.Builder builder;
        public int nErrors;
        public String name;

        public InvalidBuilderTestCase(User.Builder builder, int nErrors, String name) {
            this.builder = builder;
            this.nErrors = nErrors;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Test
    void build_validParameters() {
        User.Builder withoutPassword = new User.Builder()
                .addUsername("pippo")
                .addEmail("pippo@email.com")
                .addName("pippo")
                .addSurname("pluto");
        withoutPassword.addPassword("a", "a")
                .build()
                .consume(this::checkUserNoPassword, e -> fail());
        withoutPassword.addPassword("a")
                .build()
                .consume(this::checkUser, e -> fail());
    }

    private void checkUserNoPassword(User u) {
        assertEquals("pippo", u.getUsername());
        assertEquals("pippo@email.com", u.getEmail());
        assertEquals("pippo", u.getName());
        assertEquals("pluto", u.getSurname());
    }

    private void checkUser(User u) {
        checkUserNoPassword(u);
        assertEquals("a", u.getSaltedPassword());

    }
}