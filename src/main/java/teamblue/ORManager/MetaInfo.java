package teamblue.ORManager;

import teamblue.util.NameConverter;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

class MetaInfo {
    List<FieldInfo> fields = new ArrayList<>();

    MetaInfo() {
    }

    MetaInfo(List<FieldInfo> fields) {
        this.fields = fields;
    }


    MetaInfo of(Class cls) {
        Arrays.stream(cls.getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .forEach(field -> fields.add(
                        (new FieldInfo(NameConverter.getFieldName(field), field, cls))));
        return new MetaInfo(fields);
    }

    List<FieldInfo> getFieldInfos() {
        return fields;
    }

    public static class FieldInfo {
        String columnName;
        Field field;
        Class type;

        public FieldInfo(String columnName, Field field, Class type) {
            this.columnName = columnName;
            this.field = field;
            this.type = type;
        }

        public Field getField() {
            return field;
        }

        public Object getRSgetter(ResultSet rs) {
            try {
                if (rs.getObject(columnName) instanceof Date date) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    String format1 = formatter.format(date);
                    LocalDate dateFormatted = LocalDate.parse(format1);
                    return dateFormatted;
                }
                return rs.getObject(columnName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
