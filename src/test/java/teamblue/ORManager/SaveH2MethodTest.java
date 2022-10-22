package teamblue.ORManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class SaveH2MethodTest {

    private static final Logger log = LoggerFactory.getLogger(SaveH2MethodTest.class);
    private ORManager orManager;

    @Before
    public void setUp() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        try {
            orManager.register(Book.class);
        } catch (SQLException e) {
            log.info("Error in SQL: {}",e.getMessage());
        }
    }

    @Test
    public void shouldSaveSingleBookToDB_whereIdIsGeneratedAndNotNull() {

        Book book = new Book("Test", LocalDate.now());
        orManager.save(book);
        assertThat(book.getId()).isNotNull();
    }

    @Test
    public void shouldSaveSingleBook_whereIdIsNotGenerated(){
        Book book = new Book("Other", LocalDate.now());
        book.setId(2L);
        long lastId = 0L;
        try {
            ResultSet rs = orManager.getConnectionWithDB()
                                    .prepareStatement("SELECT COUNT(*) FROM BOOKS")
                                    .executeQuery();
            while (rs.next()) {
                lastId = rs.getLong(1);
            }
            orManager.save(book);
            assertThat(book).hasFieldOrPropertyWithValue("Id",lastId + 1L);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void afterTest(){
        Connection conn = null;
        try {
            conn = orManager.getConnectionWithDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (conn!=null){
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