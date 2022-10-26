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
    public void Refresh_ShouldUpdateTitle(){
        Book book1 = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        Book book2 = new Book("Harry Potter 3", LocalDate.of(1111, 11, 11));
        orManager.save(book1);
        orManager.save(book2);
        book1.setTitle("New Harry Potter");
        System.out.println(book1);
        Book refresh = orManager.refresh(book1);
        Assertions.assertEquals("Harry Potter", refresh.getTitle());
    }

}
