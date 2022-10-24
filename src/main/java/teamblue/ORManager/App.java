package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.model.Book;

import java.lang.reflect.InvocationTargetException;
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
        Book harry = new Book("Potter", LocalDate.now());
        orManager.save(harry);
        Book jj = new Book("jj", LocalDate.now());
        orManager.save(jj);
        List<Book> all2 = orManager.findAll(Book.class);
        List<Book> all3 = orManager.findAll(Book.class);
        List<Book> all4 = orManager.findAll(Book.class);
        List<Book> all5 = orManager.findAll(Book.class);

        System.out.println(all2.size() + " łącznie jest w bazie  elementów ");
//        orManager.findAll(Book.class).forEach(System.out::println);

        System.out.println(orManager.findAll(Book.class) + "ffffff");
        System.out.println(orManager.findAll(Book.class).get(0) + "ffffff");


    }
}
