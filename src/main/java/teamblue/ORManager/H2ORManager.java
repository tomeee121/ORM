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

                String tableName = "";

                if (entityClass.isAnnotationPresent(Table.class)) {
                    tableName = entityClass.getDeclaredAnnotation(Table.class).value();
                } else {
                    tableName = entityClass.getSimpleName();
                }


                List<Field> primaryKeyFields = new ArrayList<>();


                Field[] declaredFields = entityClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(Id.class)) ;
                    primaryKeyFields.add(declaredField);
                }


                String dropStatementQuery = DROP + entityClass.getSimpleName();
                PreparedStatement dropPriorTableStmt = getConnectionWithDB().prepareStatement(dropStatementQuery);
                dropPriorTableStmt.executeUpdate();


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
            } else {
                log.error("Error creating table of name {}", entityClass.getSimpleName());
                throw new RuntimeException("Annotate POJO with @Entity to add it to DB as a table!");
            }
        }
    }


    void columnRename(String tableName, List<Field> columnFields) throws SQLException {
        for (Field field : columnFields) {
            getConnectionWithDB()
                    .prepareStatement(ALTER_TABLE + tableName
                            + ALTER_COLUMN + field.getName().toUpperCase() + RENAME_TO + field.getAnnotation(Column.class).value())
                    .executeUpdate();
        }
    }


    void getCastedTypeToH2(List<Field> fields, int i, StringBuilder baseSql) {
/**
        primary keys need auto_increment and primary key  syntaxes
*/

        if("UUID".equals(fields.get(i).getType().getSimpleName()) && (fields.get(i).isAnnotationPresent(Id.class))) {
            baseSql.append(fields.get(i).getName() + UUID + AUTO_INCREMENT + PRIMARY_KEY);

        } else if ("long".equals(fields.get(i).getType().getSimpleName()) && (fields.get(i).isAnnotationPresent(Id.class))
                    || "Long".equals(fields.get(i).getType().getSimpleName()) && (fields.get(i).isAnnotationPresent(Id.class))) {
            baseSql.append(fields.get(i).getName() + BIGINT + AUTO_INCREMENT + PRIMARY_KEY);

/**
        now not annoted with @Id POJO fields casted to H2 equivalennt type
*/

        } else if ("UUID".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + UUID);
        } else if ("String".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + VARCHAR_255);
        } else if ("LocalDate".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + DATE);
        } else if ("int".equals(fields.get(i).getType().getSimpleName()) || "Integer".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + INTEGER);
        } else if ("long".equals(fields.get(i).getType().getSimpleName()) || "Long".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + BIGINT);
        } else if ("double".equals(fields.get(i).getType().getSimpleName()) || "Double".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + DOUBLE_PRECISION);
        } else if ("boolean".equals(fields.get(i).getType().getSimpleName()) || "Boolean".equals(fields.get(i).getType().getSimpleName())) {
            baseSql.append(fields.get(i).getName() + BOOLEAN);
        }
    }

    @Override
    Object save(Object object) {
        StringBuilder saveSql = new StringBuilder();
        String oClassName = object.getClass()
                             .getName();
        Class<?> clazz = null;
        try {
            clazz = Class.forName(oClassName);
        } catch (ClassNotFoundException e) {
            log.debug("Class was not found!");
        }

        if (clazz.isAnnotationPresent(Entity.class)) {
            Field[] declaredFields = clazz.getDeclaredFields();

            if(Arrays.stream(declaredFields).findAny().isEmpty()){
                log.debug("No fields present to save to database.");
                throw new RuntimeException("No fields present to save to database");
            }

            List<String> listOfFieldsName = getFieldsNameWithoutId(declaredFields);
            List<String> listOfFieldValues = getFieldValuesWithoutId(object, declaredFields);

            String sqlFieldName = listOfFieldsName.stream()
                                                  .collect(Collectors.joining(", "
                                                           , LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
            String sqlFieldValues = listOfFieldValues.stream()
                                                     .map(field -> "'" + field + "'")
                                                     .collect(Collectors.joining(", "
                                                           , LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
            saveSql.append(INSERT_INTO)
                   .append(getTableName(Objects.requireNonNull(clazz)))
                   .append(sqlFieldName)
                   .append(VALUES)
                   .append(sqlFieldValues);

            Long generatedKey = null;
            generatedKey = runSQLAndGetId(saveSql);

            Long finalGeneratedKey = generatedKey;
            Arrays.stream(declaredFields).filter(field -> field.isAnnotationPresent(Id.class))
                  .forEach(field -> {
                      field.setAccessible(true);
                      try {
                          field.set(object, finalGeneratedKey);
                      } catch (IllegalAccessException e) {
                          log.debug("{}", e.getMessage());
                          throw new RuntimeException("Unable to set id of the object.");
                      }
                  });
        }
        return object;
    }

    private Long runSQLAndGetId(StringBuilder saveSql) {
        Long generatedKey = null;
        try (PreparedStatement ps = getConnectionWithDB().prepareStatement(saveSql.toString()
                , Statement.RETURN_GENERATED_KEYS)){
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()){
                generatedKey = rs.getLong(1);
            }
        } catch (SQLException e) {
            log.debug("{}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return generatedKey;
    }

    private List<String> getFieldsNameWithoutId(Field[] declaredFields) {
        return Arrays.stream(declaredFields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .map(field -> {
                    if(field.isAnnotationPresent(Column.class)){
                        return field.getAnnotation(Column.class).value();
                    } else {
                        return field.getName();
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> getFieldValuesWithoutId(Object object, Field[] declaredFields) {
        return Arrays.stream(declaredFields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                     .map(field -> {
                         try {
                             field.setAccessible(true);
                             return String.valueOf(field.get(object));
                         } catch (IllegalAccessException e) {
                             throw new RuntimeException(e);
                         }
                     })
                     .collect(Collectors.toList());
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
