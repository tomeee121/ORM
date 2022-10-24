package teamblue.ORManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_BOOKS;

public class FindByIdTest {

    private static final Logger log = LoggerFactory.getLogger(FindAllTest.class);
    private ORManager orManager;

    @Before
    public void setUp() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        try {
            orManager.register(Book.class);
            orManager.save(new Book("Harry Potter", LocalDate.of(2011, 11, 28)));
            orManager.save(new Book("Cypher Fortress", LocalDate.of(1998, 11, 9)));
        } catch (SQLException e) {
            log.info("Error in SQL: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("1.Find By Id Test")
    public void findById_ShouldFindCorrectTitle_ByGivenId(){
        Optional<Book> byId = orManager.findById(1, Book.class);
        Book book = byId.get();
        Assertions.assertEquals(book.getTitle(),"Harry Potter");
    }

    @Test
    @DisplayName("2.Find By Id Test")
    public void findById_ShouldFindCorrectLocalDate_ByGivenId(){
        Optional<Book> byId = orManager.findById(1, Book.class);
        Book book = byId.get();
        Assertions.assertEquals(book.getPublishedAt(),LocalDate.of(2011, 11, 28));
    }

    @Test
    @DisplayName("3.Find By Id Test")
    public void findById_ShouldFindCorrectId_ByGivenId(){
        Optional<Book> byId = orManager.findById(1, Book.class);
        Book book = byId.get();
        Assertions.assertEquals(book.getId(),1);
    }

    @Test
    @DisplayName("3.Find By Id Test")
    public void findById_ShouldThrowNSEE_IfThereIsNoSuchId(){
        Assertions.assertThrows(NoSuchElementException.class, () -> orManager.findById(10, Book.class));
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
