package teamblue.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StringIdGenerator {
    private StringIdGenerator(){
        throw new IllegalCallerException();
    }

    public static String generate(Class<?> clazz, int sizeOfTable){
        return clazz.getSimpleName().toLowerCase() + sizeOfTable + 1L;
    }
}
