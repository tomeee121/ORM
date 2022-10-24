package teamblue.ORManager;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_BOOKS;


public class ORMCacheTest {

    private H2ORManager orManager;

    @Before
    public void setUp() throws SQLException {
        orManager = (H2ORManager) ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        MetaInfo.clearCache();
    }

    @Test
    public void shouldAddToCacheRightNrOfElements_whenSaveAndFindInvoked() throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        Assertions
                .assertThatThrownBy(() -> MetaInfo.getCache().get(Book.class).size())
                .isInstanceOf(NullPointerException.class);

        //when
        MetaInfo.clearCache();
        orManager.register(Book.class);
        Book harry = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        Book cypher_fortress = new Book("Cypher Fortress", LocalDate.of(1998, 11, 9));
        orManager.save(harry);
        orManager.save(cypher_fortress);
        List<Book> books = orManager.findAll(Book.class);

        //then
        int nrOfCachedElementsAfterSavingToDB = MetaInfo.getCache().get(Book.class).size();
        Assertions.assertThat(nrOfCachedElementsAfterSavingToDB).isGreaterThan(1);
        Assertions.assertThat(books).containsAnyOf(harry);
        Assertions.assertThat(books).containsAnyOf(cypher_fortress);
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