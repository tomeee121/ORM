package teamblue.ORManager;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_BOOKS;
import static teamblue.constants.h2.ConstantsH2.DROP_IF_EXISTS_PUBLISHER;

public class ManyToOneTablesMappingTest {

    private static ORManager orManager = null;

    @Before
    public void setUpDatabase() throws SQLException {
        orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        PreparedStatement drop_table_books = orManager.getConnectionWithDB().prepareStatement(DROP_IF_EXISTS_BOOKS);
        PreparedStatement drop_table_publisher = orManager.getConnectionWithDB().prepareStatement(DROP_IF_EXISTS_PUBLISHER);
        drop_table_books.executeUpdate();
        drop_table_publisher.executeUpdate();
    }

    @Test
    public void whenRegisterMethodInH2ORManagerInvokedWithMappedClassesPublisherBook_shouldAddTableWithForeignKeyConstraint() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.register(teamblue.model.OneToManyModels.Book.class, teamblue.model.OneToManyModels.Publisher.class);

        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();

        int IDColumnNr = resultSet.findColumn("ID");
        int titleColumnNr = resultSet.findColumn("Title_Of_Book");
        int publishedAtColumnNr = resultSet.findColumn("Published_At");
        int publisherIdColumnNr = resultSet.findColumn("publisher_id");

        Assertions.assertThat(IDColumnNr).isEqualTo(1);
        Assertions.assertThat(titleColumnNr).isEqualTo(2);
        Assertions.assertThat(publishedAtColumnNr).isEqualTo(3);
        Assertions.assertThat(publisherIdColumnNr).isEqualTo(4);
    }

    @After
    public void tearUp() throws SQLException {
        PreparedStatement dropBooks = orManager.getConnectionWithDB().prepareStatement(DROP_IF_EXISTS_BOOKS);
        PreparedStatement dropPublisher = orManager.getConnectionWithDB().prepareStatement(DROP_IF_EXISTS_PUBLISHER);
        dropBooks.executeUpdate();
        dropPublisher.executeUpdate();

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
