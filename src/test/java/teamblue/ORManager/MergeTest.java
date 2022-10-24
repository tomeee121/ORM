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

public class MergeTest {

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
    public void Merge_ShouldUpdateTitle(){
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        orManager.save(book1);
        book1.setTitle("Harry Potter 2");
        Book merge = orManager.merge(book1);
        Assertions.assertEquals("Harry Potter 2", merge.getTitle());
    }

    @Test
    public void Merge_ShouldUpdatePublishedDate(){
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        orManager.save(book1);
        book1.setPublishedAt(LocalDate.of(2020,12,12));
        Book merge = orManager.merge(book1);
        Assertions.assertEquals(LocalDate.of(2020,12,12), merge.getPublishedAt());
    }

    @Test
    public void Merge_ShouldThrowException_EntityNotSaved(){
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));

        Assertions.assertThrows(NoSuchElementException.class, () -> orManager.merge(book1));

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
