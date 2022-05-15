package it.polimi.tiw.api.beans;

import it.polimi.tiw.api.ApiError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserBuilderTest {
    @Test
    void testBuild() {
        User.Builder u = new User.Builder();
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(5, e.errors().length)
        );

        u = u.addUsername("pippo");
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(4, e.errors().length)
        );

        u = u.addPassword("a", null);
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(5, e.errors().length)
        );

        u = u.addPassword("a", "b");
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(4, e.errors().length)
        );

        u = u.addPassword("a", "a");
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(3, e.errors().length)
        );

        u = u.addEmail("pippo(at)email.com");
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(3, e.errors().length)
        );

        u = u.addEmail("pippo@email.com");
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(2, e.errors().length)
        );

        u = u.addName("Pippo");
        assertTrue(u.build().match((User user) -> false, (ApiError err) -> true));
        u.build().consume(
                (User user) -> fail(),
                (ApiError e) -> assertEquals(1, e.errors().length)
        );

        u = u.addSurname("Pippo");
        assertTrue(u.build().match((User user) -> true, (ApiError err) -> false));
    }
}