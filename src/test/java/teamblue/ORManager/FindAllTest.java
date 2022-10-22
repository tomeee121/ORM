package teamblue.ORManager;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_BOOKS;


public class FindAllTest {

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
    public void shouldFindProperNumberOfElementsInTable_whenAddedThoseInSetUp() throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        List<Book> books = orManager.findAll(Book.class);
        Assertions.assertThat(books.size()).isEqualTo(2);
    }

    @Test
    public void shouldFindRightBooksTitles_whenAddedThoseInSetUp() throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        List<Book> books = orManager.findAll(Book.class);
        List<String> titles = Arrays.asList("Harry Potter", "Cypher Fortress");
        Assertions.assertThat(titles).containsAnyOf(books.get(0).getTitle());
        Assertions.assertThat(titles).containsAnyOf(books.get(1).getTitle());
    }

    @Test
    public void shouldFindRightBooksAddingLocalDate_whenAddedThoseInSetUp() throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        List<Book> books = orManager.findAll(Book.class);
        List<LocalDate> localDates = Arrays.asList(LocalDate.of(2011, 11, 28),
                LocalDate.of(1998, 11, 9));
        Assertions.assertThat(localDates).containsAnyOf(books.get(0).getPublishedAt());
        Assertions.assertThat(localDates).containsAnyOf(books.get(1).getPublishedAt());
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





//    @Test
//    public void shouldThrowException_whereClassWasNotFound(){
//        // Given
//        class  BookTest{
//        }
//        BookTest bookTest = new BookTest();
//        // When
//        //Then
//        assertThatThrownBy(() -> orManager.save(bookTest))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage("Class was not found");
//    }




}