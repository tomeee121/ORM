package teamblue.ORManager;

import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import teamblue.classes.BookColumnTest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ColumnAnnotationTest {

    @Before
    public void setUpDatabase() throws SQLException {
        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.getConnectionWithDB().prepareStatement("DROP TABLE IF EXISTS BOOKS").execute();
    }

    @Test
    @DisplayName("@Column annotation on title field @Column(\"Title_Of_Book\")")
    public void Register_ColumnAnnotation_ChangeTitleNameProperly() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.register(BookColumnTest.class);

        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();

        int title = resultSet.findColumn("Title_Of_Book");

        Assert.assertTrue("If findColumn() return number more than 0 it means column has been found",title>0);

    }

    @Test
    @DisplayName("@Column annotation on publishedAt field @Column(\"Published_At\")")
    public void Register_ColumnAnnotation_ChangePublishedNameProperly() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.register(BookColumnTest.class);

        PreparedStatement preparedStatement = orManager.getDataSource().getConnection().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();

        int published_at = resultSet.findColumn("Published_At");

        Assert.assertTrue("If findColumn() return number more than 0 it means column has been found",published_at>0);

    }

    @Test
    @DisplayName("@Column value is 'Title_Of_Book', searching value is 'title'")
    public void Register_ColumnAnnotation_CantFindChangedColumn_ThrowException() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.register(BookColumnTest.class);

        PreparedStatement preparedStatement = orManager.getDataSource().getConnection().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();

        assertThatThrownBy(() -> resultSet.findColumn("title")).isInstanceOf(JdbcSQLSyntaxErrorException.class);

    }



}
