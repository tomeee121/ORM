package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
public class App
{
    public static void main( String[] args ) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InterruptedException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/main/resources/db.file");
        orManager.register(Book.class);

        Book harry = new Book("Harry", LocalDate.now());
        orManager.save(harry);
        findAllCheck(orManager);

    }

    public static void findAllCheck(ORManager orManager) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        List<Book> all = orManager.findAll(Book.class);
        System.out.println("Found elements number: "+all.size());
        System.out.println(all.get(0));
    }
}
