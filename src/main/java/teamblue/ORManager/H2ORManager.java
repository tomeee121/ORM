package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.annotations.Column;
import teamblue.annotations.Entity;
import teamblue.annotations.Id;
import teamblue.annotations.Table;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static teamblue.constants.h2.ConstantsH2.*;

@Slf4j
public class H2ORManager extends ORManager {

    H2ORManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    Connection getConnectionWithDB() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    void register(Class... entityClasses) throws SQLException {
        for (Class<? extends Class> entityClass : entityClasses) {

            if (entityClass.isAnnotationPresent(Entity.class)) {
                registerClass(entityClass);
            } else {
                log.error("Error creating table of name {}", entityClass.getSimpleName());
                throw new RuntimeException("Annotate POJO with @Entity to add it to DB as a table!");
            }
        }
    }

    void registerClass(Class<?> entityClass) throws SQLException {
        String tableName = "";

        if (entityClass.isAnnotationPresent(Table.class)) {
            tableName = entityClass.getDeclaredAnnotation(Table.class)
                    .value();
        } else {
            tableName = entityClass.getSimpleName();
        }


        List<Field> primaryKeyFields = new ArrayList<>();


        Field[] declaredFields = entityClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(Id.class)) ;
            primaryKeyFields.add(declaredField);
        }


        StringBuilder baseSql = new StringBuilder(CREATE_TABLE_IF_NOT_EXISTS + tableName + LEFT_PARENTHESIS);


        for (int i = 0; i < primaryKeyFields.size(); i++) {
            getCastedTypeToH2(primaryKeyFields, i, baseSql);

            if (i != primaryKeyFields.size() - 1) {
                baseSql.append(COLON);
            }
        }
        baseSql.append(RIGHT_PARENTHESIS);

        PreparedStatement addTableStatement = getConnectionWithDB().prepareStatement(String.valueOf(baseSql));
        addTableStatement.executeUpdate();

        List<Field> columnFields = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Column.class))
                .toList();

        columnRename(tableName, columnFields);

        log.info("Created table of name {}", entityClass.getSimpleName());
    }


    void columnRename(String tableName, List<Field> columnFields) throws SQLException {
        for (Field field : columnFields) {
            getConnectionWithDB().prepareStatement(ALTER_TABLE + tableName
                            + ALTER_COLUMN + field.getName()
                            .toUpperCase() + RENAME_TO +
                            field.getAnnotation(Column.class)
                                    .value())
                    .executeUpdate();
        }
    }


    void getCastedTypeToH2(List<Field> fields, int i, StringBuilder baseSql) {
/**
 primary keys need auto_increment and primary key  syntaxes
 */

        if (java.util.UUID.class == fields.get(i).getType() && (fields.get(i).isAnnotationPresent(Id.class))) {
            baseSql.append(fields.get(i).getName() + UUID + AUTO_INCREMENT + PRIMARY_KEY);
        } else if (long.class == fields.get(i).getType() && (fields.get(i).isAnnotationPresent(Id.class))
                || Long.class == fields.get(i).getType() && (fields.get(i).isAnnotationPresent(Id.class))) {
            baseSql.append(fields.get(i).getName() + BIGINT + AUTO_INCREMENT + PRIMARY_KEY);

/**
 now not annoted with @Id POJO fields casted to H2 equivalennt type
 */

        } else if (java.util.UUID.class == fields.get(i).getType()) {baseSql.append(fields.get(i).getName() + UUID);
        } else if (String.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + VARCHAR_255);
        } else if (LocalDate.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + DATE);
        } else if (int.class == fields.get(i).getType()
                || Integer.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + INTEGER);
        } else if (long.class == fields.get(i).getType()
                || Long.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + BIGINT);
        } else if (double.class == fields.get(i).getType()
                || Double.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + DOUBLE_PRECISION);
        } else if (boolean.class == fields.get(i).getType()
                || Boolean.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + BOOLEAN);
        }
    }

    @Override
    Object save(Object o) {
        return null;
    }

    @Override
    void persist(Object o) {

    }

    @Override
    <T> Optional<T> findById(Serializable id, Class<T> cls) {
        return Optional.empty();
    }

    @Override
    <T> List<T> findAll(Class<T> cls) {

        String tableName = "";

        if (cls.isAnnotationPresent(Table.class)) {
            tableName = cls.getAnnotation(Table.class)
                    .value();
        } else {
            tableName = cls.getSimpleName();
        }

        String baseSql = SELECT_ALL_FROM + tableName;

        PreparedStatement findAllStmt = null;
        List<T> foundAll = new ArrayList<>();
        try {
            findAllStmt = getConnectionWithDB().prepareStatement(baseSql);
            ResultSet resultSet = null;
            resultSet = findAllStmt.executeQuery();
            int nrOfColumns = resultSet.getMetaData().getColumnCount();
            Constructor<T> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            T newObject = constructor.newInstance();

            MetaInfo metaInfo = new MetaInfo();
            while (resultSet.next()) {
                MetaInfo metaInfoInstanceObjects = metaInfo.of(cls);
                for (var fieldInfo : metaInfoInstanceObjects.getFieldInfos()) {
                    var value = fieldInfo.getRSgetter(resultSet);
                    var field = fieldInfo.getField();
                    field.set(newObject, value);
                }
                foundAll.add(newObject);

            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Exception of reflecive operation");
        } catch (SQLException e) {
            throw new RuntimeException("SQL Exception");
        }

        return foundAll;
//        while (resultSet.next()) {
//            int cursor = 1;
//            Field[] declaredFields = cls.getDeclaredFields();
//            for (Field declaredField : declaredFields) {
//                declaredField.set(objectToFill, resultSet.getObject(cursor++));
//            }
//            cursor = 1;
//
//        }

//        List<Object> foundTypeNames = new ArrayList<>();
//        List<T> allFoundObjects = new ArrayList<>();
//        ResultSetMetaData metaData = resultSet.getMetaData();
//        int nrOfColumns = metaData.getColumnCount();
//
//        for (int i = 1; i <= nrOfColumns; i++) {
//            String columnNames = metaData.getColumnName(i);
//            foundTypeNames.add(columnNames);
//        }
//
//        Constructor<T> contrOfObjectToFill = cls.getDeclaredConstructor();
//        Object objectToFill = contrOfObjectToFill.newInstance();
//        while (resultSet.next()) {
//            int cursor = 1;
//            Field[] declaredFields = cls.getDeclaredFields();
//            for (Field declaredField : declaredFields) {
//                declaredField.set(objectToFill, resultSet.getObject(cursor++));
//            }
//            cursor = 1;
//
//        }
    }

    class MetaInfo {
        //        Map<Class, MetaInfo> cache = new HashMap<>();
        List<FieldInfo> fields = new ArrayList<>();

        MetaInfo() {
        }

        MetaInfo(List<FieldInfo> fields) {
            this.fields = fields;
        }

        String tableName;
//        Class<T> cls;

        MetaInfo of(Class cls) {
            Arrays.stream(cls.getDeclaredFields()).forEach(field -> fields.add(new FieldInfo(field.getName(), field, cls)));
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
                    return rs.getObject(columnName);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    <T> Iterable<T> findAllAsIterable(Class<T> cls) {
        return null;
    }

    @Override
    <T> Stream<T> findAllAsStream(Class<T> cls) {
        return null;
    }

    @Override
    <T> T merge(T o) {
        return null;
    }

    @Override
    <T> T refresh(T o) {
        return null;
    }

    @Override
    boolean delete(Object o) {
        return false;
    }
}
