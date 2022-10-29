package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import teamblue.model.Book;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class FindAllAsIterableTest {

    private ORManager orManager;

    @BeforeEach
    public void setUpDatabase() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.getConnectionWithDB()
                .prepareStatement("DROP ALL OBJECTS")
                .execute();
    }

    @Test
    @DisplayName("Should return object of book when iterating through books")
    void shouldReturnObjectOfBookWhenIteratingThroughBooks() throws SQLException {
        Book book = new Book("Logan", LocalDate.of(2021,10,20));
        Class<? extends Book> clazz = book.getClass();
        orManager.register(clazz);

        orManager.save(book);
        Iterator<? extends Book> iterator = orManager.findAllAsIterable(clazz).iterator();

        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should return false when there is no data in database")
    void shouldReturnFalseWhenThereIsNoDataInDatabase() throws SQLException {
        Book book = new Book("Logan", LocalDate.of(2021,10,20));
        Class<? extends Book> clazz = book.getClass();
        orManager.register(clazz);

        Iterator<? extends Book> iterator = orManager.findAllAsIterable(clazz).iterator();

        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should return the same object when invoking next method of iterator")
    void shouldReturnTheSameObjectWhenInvokingNextMethodOfIterator() throws SQLException {
        Book book = new Book("Logan", LocalDate.of(2021,10,20));
        Class<? extends Book> clazz = book.getClass();
        orManager.register(clazz);
        orManager.save(book);
        Iterator<? extends Book> iterator = orManager.findAllAsIterable(clazz).iterator();

        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(book);
    }


    @AfterEach
    public void afterTest() {
        Connection conn = null;
        try {
            conn = orManager.getConnectionWithDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (conn != null) {
            try {
                orManager.getConnectionWithDB()
                        .prepareStatement("DROP ALL OBJECTS")
                        .execute();
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
