package teamblue.util;

import teamblue.annotations.Column;
import teamblue.annotations.ManyToOne;
import teamblue.annotations.Table;

import java.lang.reflect.Field;

public class NameConverter {

    private NameConverter() {
        throw new IllegalCallerException();
    }

    public static String getTableName(Class<?> clazz) {
        return clazz.isAnnotationPresent(Table.class) ? clazz.getAnnotation(Table.class)
                .value() : clazz.getSimpleName();
    }

    public static String getFieldName(Field field) {
        if(field.isAnnotationPresent(Column.class)){
            return field.getAnnotation(Column.class).value();
        } else if (field.isAnnotationPresent(ManyToOne.class)){
            return field.getName().toLowerCase() + "_id";
        } else {
            return field.getName();
        }
    }
}
