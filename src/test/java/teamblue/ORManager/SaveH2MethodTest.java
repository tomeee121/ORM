package teamblue.ORManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.ORManager.modelTest.IdInSecondPosition;
import teamblue.ORManager.modelTest.MissingIdAnnotation;
import teamblue.ORManager.modelTest.WithOneIdField;
import teamblue.annotations.Entity;
import teamblue.annotations.Id;
import teamblue.annotations.Table;
import teamblue.model.Book;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


public class SaveH2MethodTest {

    private static final Logger log = LoggerFactory.getLogger(SaveH2MethodTest.class);
    private ORManager orManager;

    @Before
    public void setUp() throws Exception {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.getConnectionWithDB()
                 .prepareStatement("DROP TABLE IF EXISTS BOOKS")
                 .execute();
    }

    @Test
    public void shouldSaveSingleBookToDB_whereIdIsGeneratedAndNotNull() throws SQLException {

        Book book = new Book("Test", LocalDate.now());

        orManager.register(book.getClass());
        orManager.save(book);

        assertThat(book.getId()).isNotNull();
    }

    @Test
    public void shouldSaveSingleBook_whereIdIsPresentInObject() throws SQLException {
        Book book = new Book("Other", LocalDate.now());
        String tableName = getTableName(book.getClass());

        book.setId(2L);
        orManager.register(book.getClass());

        long newId = getSizeOfTable(tableName) + 1L;
        orManager.save(book);

        assertThat(book).hasFieldOrPropertyWithValue("Id", newId);
    }

    @Test
    public void shouldSaveObject_whenFieldsAreNull() throws SQLException {
        // Given
        Book book = new Book(null, null);
        Book book1 = new Book("title", null);
        Book book2 = new Book(null, LocalDate.of(2022, 10, 2));
        // When
        orManager.register(book.getClass());
        orManager.save(book);
        orManager.save(book1);
        orManager.save(book2);
        // Then
        assertThat(book.getId()).isPositive();
        assertThat(book1.getId()).isPositive();
        assertThat(book2.getId()).isPositive();
    }


    @Test
    public void shouldNotSaveObject_whenObjectHaveNoFields() {
        @Entity
        class WithoutFields {}

        WithoutFields test = new WithoutFields();
        String tableName =  getTableName(test.getClass());
        try {
            orManager.register(test.getClass());
        } catch (SQLException e) {
            log.debug("{}",e.getSQLState());
        }

        long sizeOfTableBeforeSave = getSizeOfTable(tableName);
        orManager.save(test);
        long sizeOfTableAfterSave = getSizeOfTable(tableName);

        assertThat(sizeOfTableBeforeSave).isEqualTo(sizeOfTableAfterSave);
    }

    @Test
    public void shouldSaveObject_whenObjectHaveOneIdField() throws SQLException {

        WithOneIdField test = new WithOneIdField();
        String tableName =  getTableName(test.getClass());
        orManager.register(test.getClass());

        long sizeOfTableBeforeSave = getSizeOfTable(tableName);
        orManager.save(test);
        long sizeOfTableAfterSave = getSizeOfTable(tableName);

        assertThat(sizeOfTableBeforeSave).isNotEqualTo(sizeOfTableAfterSave);
    }

    @Test
    public void shouldSaveObject_whenIdInObjectIsInSecondPosition() throws SQLException {

        IdInSecondPosition test = new IdInSecondPosition();
        String tableName =  getTableName(test.getClass());
        orManager.register(test.getClass());

        long sizeOfTableBeforeSave = getSizeOfTable(tableName);
        orManager.save(test);
        long sizeOfTableAfterSave = getSizeOfTable(tableName);

        assertThat(sizeOfTableBeforeSave).isNotEqualTo(sizeOfTableAfterSave);
    }

    @Test
    public void shouldSaveObject_whenIdIsMissing() throws SQLException {
        MissingIdAnnotation test = new MissingIdAnnotation();
        String tableName =  getTableName(test.getClass());
        orManager.register(test.getClass());

        long sizeOfTableBeforeSave = getSizeOfTable(tableName);
        orManager.save(test);
        long sizeOfTableAfterSave = getSizeOfTable(tableName);

        assertThat(sizeOfTableBeforeSave).isNotEqualTo(sizeOfTableAfterSave);
    }

    @After
    public void afterTest() {
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

    private long getSizeOfTable(String nameOfTable) {
        long size = 0L;
        try {
            ResultSet rs = orManager.getConnectionWithDB()
                                    .prepareStatement("SELECT COUNT(*) FROM " + nameOfTable)
                                    .executeQuery();
            while (rs.next()) {
                size = rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return size;
    }

    private String getTableName(Class<?> clazz){
        if(clazz.isAnnotationPresent(Table.class)){
            return clazz.getAnnotation(Table.class).value();
        } else {
            return clazz.getSimpleName();
        }
    }
}
