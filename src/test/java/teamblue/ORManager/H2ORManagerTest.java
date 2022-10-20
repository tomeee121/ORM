package teamblue.ORManager;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class H2ORManagerTest {

    private static final Logger log = LoggerFactory.getLogger(H2ORManagerTest.class);
    private ORManager orManager;

    @Before
    public void setUp() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("db.file");
        try {
            orManager.register(Book.class);
        } catch (SQLException e) {
            log.info(e.getMessage());
        }
    }

    @Test()
    public void shouldSaveSingleBookToDB_whereIdIsNotNull() {

        Book book = new Book("Test", LocalDate.now());
        orManager.save(book);

        assertThat(book.getId()).isNotNull();
    }

    @Test
    public void shouldThrowException_whereClassWasNotFound(){
        // Given
        class  BookTest{
        }
        BookTest bookTest = new BookTest();
        // When
        //Then
        assertThatThrownBy(() -> orManager.save(bookTest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Class was not found");
    }




}