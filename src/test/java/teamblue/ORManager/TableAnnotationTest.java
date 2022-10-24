package teamblue.ORManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import teamblue.classes.BookTableTest;
import teamblue.classes.BookTableTest2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableAnnotationTest {

    @Before
    public void setUpDatabase() throws SQLException {
        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.getConnectionWithDB().prepareStatement("DROP TABLE IF EXISTS BOOKS").execute();
        orManager.getConnectionWithDB().prepareStatement("DROP TABLE IF EXISTS Random_Table").execute();
    }

    @Test
    @DisplayName("@Table annotation on BookTest class with \"books\" value in it")
    public void Register_TableAnnotation_TableNameChangedCorrectly() throws SQLException {
        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");

        orManager.register(BookTableTest.class);

        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");

        boolean execute = preparedStatement.execute();

        Assert.assertTrue(execute);

    }

    @Test
    @DisplayName("@Table annotation on BookTest class with default value in it")
    public void Register_TableAnnotation_TableNameChangedCorrectlyWithDefaultValue() throws SQLException {
        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");

        orManager.register(BookTableTest2.class);

        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM Random_Table");

        boolean execute = preparedStatement.execute();

        Assert.assertTrue(execute);

    }

}
