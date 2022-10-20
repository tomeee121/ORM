package teamblue.ORManager;

import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.Assert;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ColumnAnnotationTest {
    @Test
    public void Register_ColumnAnnotation_ChangeTitleNameProperly() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/test/resources/db.file");
        orManager.register(BookTest.class);

        H2ORManager H2ORM = (H2ORManager) orManager;

        PreparedStatement preparedStatement = H2ORM.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();
        //@Column annotation on title field @Column("Title_Of_Book")
        int title = resultSet.findColumn("Title_Of_Book");

        Assert.assertTrue("If findColumn() return number more than 0 it means column has been found",title>0);

    }

    @Test
    public void Register_ColumnAnnotation_ChangePublishedNameProperly() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/main/resources/db.file");

        PreparedStatement preparedStatement = orManager.getDataSource().getConnection().prepareStatement("SELECT * FROM BOOKSSELECT * FROM BOOKS");

        //@Column annotation on publishedAt field @Column("Published_At")
        orManager.register(BookTest.class);

        ResultSet resultSet = preparedStatement.executeQuery();
        int published_at = resultSet.findColumn("Published_At");

        Assert.assertTrue("If findColumn() return number more than 0 it means column has been found",published_at>0);

    }

    @Test
    public void Register_ColumnAnnotation_CantFindChangedColumn_ThrowException() throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/main/resources/db.file");

        PreparedStatement preparedStatement = orManager.getDataSource().getConnection().prepareStatement("SELECT * FROM BOOKS");

        //@Column annotation on publishedAt field @Column("Published_At")
        orManager.register(BookTest.class);

        ResultSet resultSet = preparedStatement.executeQuery();

        assertThatThrownBy(() -> resultSet.findColumn("")).isInstanceOf(JdbcSQLSyntaxErrorException.class);

    }



}
