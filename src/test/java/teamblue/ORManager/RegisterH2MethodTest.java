package teamblue.ORManager;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import teamblue.classes.BookTableTest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterH2MethodTest {

    @Before
    public void setUpDatabase() throws SQLException {
        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.getConnectionWithDB().prepareStatement("DROP TABLE IF EXISTS BOOKS").execute();
    }

    @Test
    public void whenRegisterMethodInH2ORManagerInvokedWithBookClass_shouldAddTableWithGivenColumns() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.register(BookTableTest.class);

        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();

        int IDColumnNr = resultSet.findColumn("ID");
        int titleColumnNr = resultSet.findColumn("Title_Of_Book");
        int publishedAtColumnNr = resultSet.findColumn("Published_At");

        Assertions.assertThat(IDColumnNr).isEqualTo(1);
        Assertions.assertThat(titleColumnNr).isEqualTo(2);
        Assertions.assertThat(publishedAtColumnNr).isEqualTo(3);

    }
}
