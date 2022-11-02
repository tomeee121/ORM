package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.model.Book;
import teamblue.model.OneToManyModels.Publisher;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        harry.setPublishedAt(LocalDate.of(2020, 12, 12));
        Book saved = orManager.save(harry);
        Optional<Book> byId = orManager.findById(saved.getId(), Book.class);
        Book bookFound = byId.get();
        bookFound.setTitle("");
        bookFound.setPublishedAt(LocalDate.of(1111, 11, 11));
        orManager.merge(bookFound);
        System.out.println(orManager.findById(bookFound.getId(), Book.class).get());

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


        Publisher publisher1Mv2 = new Publisher("IT libr");
        orManager.persist(publisher1Mv2);
        bookM1v2.setPublisher(publisher1Mv2);
        orManager.merge(bookM1v2);
    }

    }
