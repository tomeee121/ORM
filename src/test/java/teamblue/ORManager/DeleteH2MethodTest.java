package teamblue.ORManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteH2MethodTest {

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
    public void smokeTest() throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Book book1 = new Book("Test", LocalDate.now());
        Book book2 = new Book("Test", LocalDate.now());


        orManager.register(Book.class);
        orManager.persist(book1);
        orManager.persist(book2);

        int beforeDelete = orManager.findAll(Book.class).size();
        orManager.delete(book1);
        int afterDelete = orManager.findAll(Book.class).size();

        assertThat(beforeDelete).isNotEqualTo(afterDelete);
    }

    @Test
    public void shouldReturnTrue_whenBookWasDeleted() throws SQLException {
        // Given
        Book book1 = new Book("Test", LocalDate.now());
        orManager.register(Book.class);
        orManager.persist(book1);

        //When
        boolean resultOfDelete = orManager.delete(book1);

        //Then
        assertThat(resultOfDelete).isTrue();
    }

    @Test
    public void shouldSetIdToNull_whenObjectIsDeleted() throws SQLException {
        // Given
        Book book1 = new Book("Test", LocalDate.now());
        orManager.register(Book.class);
        orManager.persist(book1);

        //When
        orManager.delete(book1);

        //Then
        assertThat(book1.getId()).isEqualTo(null);
    }
}