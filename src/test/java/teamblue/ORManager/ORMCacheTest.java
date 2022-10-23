package teamblue.ORManager;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_BOOKS;


public class ORMCacheTest {


    private static final Logger log = LoggerFactory.getLogger(ORMCacheTest.class);
    private H2ORManager orManager;

    @Test
    public void shouldAddToCacheRightNrOfElements_whenSaveAndFindInvoked() throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        //given
        orManager = (H2ORManager) ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        Assertions
                .assertThatThrownBy(() -> H2ORManager.MetaInfo.getCache().get(Book.class).size())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Cannot invoke \"java.util.Set.size()\" because the return value of \"java.util.Map.get(Object)\" is null");

        //when
        orManager.register(Book.class);
        Book harry = new Book("Harry Potter", LocalDate.of(2011, 11, 28));
        Book cypher_fortress = new Book("Cypher Fortress", LocalDate.of(1998, 11, 9));
        List<Book> books = Arrays.asList(harry, cypher_fortress);
        orManager.save(harry);
        orManager.save(cypher_fortress);
        orManager.findAll(Book.class);

        //then
        int nrOfCachedElementsAfterSavingToDB = H2ORManager.MetaInfo.getCache().get(Book.class).size();
        Assertions.assertThat(nrOfCachedElementsAfterSavingToDB).isEqualTo(2);
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