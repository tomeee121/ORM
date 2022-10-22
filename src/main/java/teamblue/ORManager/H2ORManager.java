package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.annotations.Column;
import teamblue.annotations.Entity;
import teamblue.annotations.Id;
import teamblue.annotations.Table;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDate;

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
                                        + ALTER_COLUMN + field.getName().toUpperCase() + RENAME_TO +
                                         field.getAnnotation(Column.class).value())
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

        } else if (java.util.UUID.class == fields.get(i).getType()) {
            baseSql.append(fields.get(i).getName() + UUID);
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
    public Object save(Object object) {

        String oClassName = object.getClass().getName();
        Class<?> clazz;
        try {
            clazz = Class.forName(oClassName);
        } catch (ClassNotFoundException e) {
            log.debug("Class was not found!");
            return object;
        }
        saveObject(object, clazz);

        return object;
    }

    protected void saveObject(Object object, Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            log.info("Class missing @Entity annotation!");
        } else {
            List<Field> declaredFields = Arrays.stream(clazz.getDeclaredFields())
                                               .toList();

            StringBuilder saveSql = new StringBuilder();

            if (declaredFields.stream()
                              .findAny()
                              .isEmpty()) {
                log.info("No fields present to save to database.");
                return;
            }

            List<String> listOfFieldsName = getFieldsName(declaredFields);
            List<String> listOfFieldValues = getFieldValuesForSaving(object, declaredFields);
            String sqlFieldName = listOfFieldsName.stream()
                                                  .collect(Collectors.joining(", ", LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
            String sqlFieldValues = String.join(", ", listOfFieldValues);

            saveSql.append(INSERT_INTO)
                   .append(getTableName(Objects.requireNonNull(clazz)))
                   .append(sqlFieldName)
                   .append(VALUES)
                   .append(LEFT_PARENTHESIS)
                   .append(sqlFieldValues)
                   .append(RIGHT_PARENTHESIS);

            Long generatedKey;
            try {
                generatedKey = runSQLAndGetId(saveSql);
            } catch (SQLException e) {
                log.info("Unable to get correct generated ID.");
                log.debug("{}", e.getMessage());
                return;
            }
            Long finalGeneratedKey = generatedKey;
            Arrays.stream(clazz.getDeclaredFields())
                  .filter(field -> field.isAnnotationPresent(Id.class))
                  .forEach(field -> {
                      field.setAccessible(true);
                      try {
                          field.set(object, finalGeneratedKey);
                      } catch (IllegalAccessException e) {
                          log.debug("{}", e.getMessage());
                      }
                  });
            log.info("Object of {} saved successfully", object.getClass()
                                                              .getSimpleName());
        }
    }

    private Long runSQLAndGetId(StringBuilder saveSql) throws SQLException {
        Long generatedKey = null;
        try (PreparedStatement ps = getConnectionWithDB().prepareStatement(saveSql.toString(),
                                                                           Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                generatedKey = rs.getLong(1);
            }
        } catch (SQLException e) {
            log.debug("Unable to save object to database, some fields might be null. %n {}", e.getSQLState());
            throw new SQLException("Unable to save object.");
        }
        return generatedKey;
    }

    private List<String> getFieldsName(List<Field> declaredFields) {
        return declaredFields.stream()
                     .map(field -> {
                         if (field.isAnnotationPresent(Column.class)) {
                             return field.getAnnotation(Column.class)
                                         .value();
                         } else {
                             return field.getName();
                         }
                     })
                     .toList();
    }

    private List<String> getFieldValuesForSaving(Object object, List<Field> declaredFields) {
        return declaredFields.stream()
                             .map(field -> {
                                 try {
                                     if (field.isAnnotationPresent(Id.class)) {
                                         if (field.getType().getSimpleName().equals("Long")) {
                                             return "default";
                                         } else if (field.getType().getSimpleName().equals("String")) {
                                             return String.valueOf(field.get(object));
                                         } else {
                                             return "";
                                         }
                                     } else {
                                         field.setAccessible(true);
                                         return String.valueOf(field.get(object));
                                     }
                                 } catch (IllegalAccessException e) {
                                     log.debug("Unable to access field: {}", field.getName());
                                     return "";
                                 }
                             })
                             .map(field -> field.equals("default")
                                     || field.equals("null") ? field : "'" + field + "'")
                             .toList();
    }

    private static String getTableName(Class<?> clazz) {
        return clazz.isAnnotationPresent(Table.class) ? clazz.getAnnotation(Table.class)
                                                             .value() : clazz.getSimpleName();
    }




    private List<String> getDeclaredFieldsName(Stream<Field> fieldStream) {
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
        return null;
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
