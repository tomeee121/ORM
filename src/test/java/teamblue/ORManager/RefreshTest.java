package teamblue.ORManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.NoSuchElementException;

import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_BOOKS;

public class RefreshTest {

    private static final Logger log = LoggerFactory.getLogger(FindAllTest.class);
    private ORManager orManager;

    @Before
    public void setUp() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        try {
            orManager.register(Book.class);
        } catch (SQLException e) {
            log.info("Error in SQL: {}", e.getMessage());
        }
    }

    @Test
    public void Refresh_ShouldUpdateTitle() {
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        orManager.save(book1);
        book1.setTitle("New Harry Potter");
        Book refresh = orManager.refresh(book1);
        Assertions.assertEquals("Harry Potter", refresh.getTitle());
    }

    @Test
    public void Refresh_ShouldUpdatePublishDate() {
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        orManager.save(book1);
        book1.setPublishedAt(LocalDate.of(1000, 10, 1));
        Book refresh = orManager.refresh(book1);
        Assertions.assertEquals(LocalDate.of(2011, 11, 28), refresh.getPublishedAt());
    }

    @Test
    public void Refresh_ShouldThrowException_EntityNotSaved() {
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));

        Assertions.assertThrows(NoSuchElementException.class, () -> orManager.refresh(book1));
    }

    @After
    public void tearUp() throws SQLException {
        PreparedStatement dropStmt = orManager.getConnectionWithDB().prepareStatement(DROP_IF_EXISTS_BOOKS);
        dropStmt.executeUpdate();

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
}
