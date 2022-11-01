package teamblue.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StringIdGenerator {
    private StringIdGenerator(){
        throw new IllegalCallerException();
    }

    public static String generate(Class<?> clazz, Connection conn){
        long sizeOfTable = 0L;
        try (Connection connection = conn) {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + NameConverter.getTableName(clazz));
            ResultSet resultSet = ps.executeQuery();
            while(resultSet.next()){
                sizeOfTable = resultSet.getLong(1);
            }
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return clazz.getSimpleName().toLowerCase() + sizeOfTable + 1L;
    }
}
