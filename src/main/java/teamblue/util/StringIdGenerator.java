package teamblue.util;

import teamblue.annotations.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StringIdGenerator {

    public static String generate(Class<?> clazz, Connection conn){
        long sizeOfTable = 0L;
        try (Connection connection = conn) {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + getTableName(clazz));
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

    private static String getTableName(Class<?> clazz) {
        return clazz.isAnnotationPresent(Table.class) ? clazz.getAnnotation(Table.class)
                .value() : clazz.getSimpleName();
    }

}
