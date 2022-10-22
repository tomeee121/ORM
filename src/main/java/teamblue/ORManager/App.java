package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class App
{
    public static void main( String[] args ) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InterruptedException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/main/resources/db.file");
        orManager.register(Book.class);

        Thread.sleep(15000);
        findAllCheck(orManager);

//        PreparedStatement preparedStatement = orManager.getConnectionWithDB().prepareStatement("SELECT * FROM BOOKS");
//
//        ResultSet resultSet = preparedStatement.executeQuery();
//        //@Column annotation on title field @Column("Title_Of_Book")
//        int title = resultSet.findColumn("Title_Of_Book");
//        System.out.println(title);

    }

    public static void findAllCheck(ORManager orManager) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        List<Book> all = orManager.findAll(Book.class);
        System.out.println("Found elements number: "+all.size());
    }
}
