package teamblue.ORManager;


import teamblue.ORManager.H2ORManager;
import teamblue.ORManager.ORManager;
import teamblue.ORManager.ORManagerFactory;
import teamblue.model.Book;

import java.sql.SQLException;

public class App
{
    public static void main( String[] args ) throws SQLException {
        ORManager orManager = ORManagerFactory.withPropertiesFrom("db.file");
        orManager.register(Book.class);


    }
}
