package teamblue.ORManager;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.annotations.Id;
import teamblue.model.Book;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;


public class PersistH2MethodTest {
    private static final Logger log = LoggerFactory.getLogger(SaveH2MethodTest.class);
    private ORManager orManager;

    @Before
    public void setUp() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.getConnectionWithDB()
                 .prepareStatement("DROP TABLE IF EXISTS BOOKS")
                 .execute();
    }
    @After
    public void tearDown() throws Exception {
        Connection conn = null;
        try {
            conn = orManager.getConnectionWithDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void shouldThrowException_whenObjectIsSavedSecondTimeWithPersistMethode() throws SQLException{
        Book book = new Book("Title", LocalDate.now());

        orManager.register(book.getClass());
        Book createdBook = (Book) orManager.save(book);

        assertThatThrownBy(() -> orManager.persist(createdBook))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Class should not have ID!");
    }

    @Test
    public void shouldSaveObject_whenObjectHaveNoId() throws SQLException {
        Book book = new Book("Title", LocalDate.now());
        orManager.register(book.getClass());
        orManager.persist(book);
        String fieldName = Arrays.stream(book.getClass()
                                     .getDeclaredFields())
                         .filter(field -> field.isAnnotationPresent(Id.class))
                         .map(field -> field.getName())
                         .findAny()
                         .orElse("");
        assertThat(book).hasFieldOrPropertyWithValue(fieldName, 1L);
    }
}