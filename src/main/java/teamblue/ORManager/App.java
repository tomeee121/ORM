package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.model.Book;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class App
{
    public static void main( String[] args ) throws SQLException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/main/resources/db.file");
        orManager.register(Book.class);

        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");

        ResultSet resultSet = preparedStatement.executeQuery();
        //@Column annotation on title field @Column("Title_Of_Book")
        int title = resultSet.findColumn("Title_Of_Book");
        System.out.println(title);

    }
}
