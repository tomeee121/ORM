package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.model.Book;
import teamblue.model.OneToManyModels.Publisher;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
public class App {
    public static void main(String[] args) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InterruptedException {

        ORManager orManager = ORManagerFactory.withPropertiesFrom("src/main/resources/db.file");

        _1TO1Presentation(orManager);

        _1MPresentation(orManager);
    }

    private static void _1TO1Presentation(ORManager orManager) throws SQLException {
        orManager.register(Book.class);

        Book harry = new Book("Harry", LocalDate.now());
        Book Potter = new Book("Potter", LocalDate.now());
        harry.setPublishedAt(LocalDate.of(2020, 12, 12));
        orManager.save(harry);
        orManager.findById(1, Book.class);
        harry.setTitle("");
        harry.setPublishedAt(LocalDate.of(1111, 11, 11));
        System.out.println(harry);
        orManager.refresh(harry);
        System.out.println(harry);
        harry.setPublishedAt(LocalDate.of(1900, 02, 01));
        orManager.merge(harry);
    }

    private static void _1MPresentation(ORManager orManager) throws SQLException {
        Publisher publisher1M = new Publisher("IT Publishing commerce. @C");
        teamblue.model.OneToManyModels.Book bookM1 =
                new teamblue.model.OneToManyModels.Book("Book about one to many", LocalDate.now());
        bookM1.setPublisher(publisher1M);
        teamblue.model.OneToManyModels.Book bookM1v2 =
                new teamblue.model.OneToManyModels.Book("Another book about one to many", LocalDate.now());
        bookM1v2.setPublisher(publisher1M);

        orManager.register(teamblue.model.OneToManyModels.Book.class, Publisher.class);
        orManager.save(publisher1M);
        orManager.save(bookM1);
        orManager.save(bookM1v2);
    }

        public static void findAllCheck (ORManager orManager) throws
        SQLException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
            List<Book> all = orManager.findAll(Book.class);
            System.out.println("Found elements number: " + all.size());
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
