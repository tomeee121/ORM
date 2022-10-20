package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.annotations.Column;
import teamblue.annotations.Entity;
import teamblue.annotations.PrimaryKey;
import teamblue.annotations.Table;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static teamblue.constants.h2.ConstantsH2.*;

@Slf4j
public class H2ORManager extends ORManager {

    H2ORManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    void register(Class... entityClasses) throws SQLException {
        for (Class entityClass : entityClasses) {
            if (entityClass.isAnnotationPresent(Entity.class)) {


                List<Field> primaryKeyFields = new ArrayList<>();


                Field[] declaredFields = entityClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(PrimaryKey.class)) ;
                    primaryKeyFields.add(declaredField);
                }

                StringBuilder baseSql;
                if (entityClass.isAnnotationPresent(Table.class)) {

                }
                String dropStatementQuery = DROP + entityClass.getSimpleName();
                PreparedStatement dropPriorTableStmt = dataSource.getConnection().prepareStatement(dropStatementQuery);
                dropPriorTableStmt.executeUpdate();





                baseSql = new StringBuilder(CREATE_TABLE_IF_NOT_EXISTS + entityClass.getSimpleName() + LEFT_PARENTHESIS);



                for (int i = 0; i < primaryKeyFields.size(); i++) {
                    getCastedTypeToH2(primaryKeyFields, i, baseSql);

                    if (i != primaryKeyFields.size() - 1) {
                        baseSql.append(COLON);
                    }
                }
                baseSql.append(RIGHT_PARENTHESIS);

                PreparedStatement addTableStatement = dataSource.getConnection()
                        .prepareStatement(String.valueOf(baseSql));
                addTableStatement.executeUpdate();

                List<Field> columnFields = Arrays.stream(declaredFields)
                        .filter(f -> f.isAnnotationPresent(Column.class))
                        .toList();

                columnRename(entityClass, columnFields);

            } else {
                throw new RuntimeException("Annotate POJO with @Entity to add it to DB as a table!");
            }
        }
    }

    void columnRename(Class entityClass, List<Field> columnFields) throws SQLException {
        for(Field field : columnFields){
            dataSource.getConnection()
                    .prepareStatement(ALTER_TABLE + entityClass.getSimpleName()
                    + ALTER_COLUMN + field.getName() + RENAME_TO + field.getAnnotation(Column.class).value())
                    .executeUpdate();
        }
    }


    void getCastedTypeToH2(List<Field> fields, int i, StringBuilder baseSql) {
        if (fields.get(i).isAnnotationPresent(PrimaryKey.class)) {
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
    Object save(Object o) {
        StringBuilder saveSql = new StringBuilder();
        String oClassName = o.getClass()
                             .getName();
        Class<?> clazz = null;
        try {
            clazz = Class.forName(oClassName);
        } catch (ClassNotFoundException e) {
            log.info("{}", e.getMessage());
            throw new RuntimeException("Class was not found");
        }

        if (clazz.isAnnotationPresent(Entity.class)) {
            Field[] declaredFields = clazz.getDeclaredFields();
            saveSql.append(INSERT_INTO)
                   .append(clazz.getSimpleName())
                   .append(VALUES);
            if(Arrays.stream(declaredFields).findAny().isEmpty()){
                log.debug("No fields present to save to database.");
                throw new RuntimeException("No fields present to save to database");
            }
            String fieldNames = Arrays.stream(declaredFields)
                    .map(field -> getFieldNameOfObject(o, field))
                    .collect(Collectors.joining(", ", LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
            saveSql.append(fieldNames);


            Long currentId = Arrays.stream(clazz.getDeclaredFields())
                                   .filter(field -> field.isAnnotationPresent(Id.class))
                                   .map(f -> {
                                       f.setAccessible(true);
                                       try {
                                           return f.getLong(o);
                                       } catch (IllegalAccessException e) {
                                           log.debug(e.getMessage());
                                           throw new RuntimeException("Unable to get Id from object");
                                       }
                                   })
                                   .findFirst()
                                   .orElse(0L);


            String sqlFieldsValue = "";
            if (currentId == 0L
                    && Arrays.stream(declaredFields).count() > 2) {
                sqlFieldsValue = getSqlValuesOfFields(declaredFields,
                                                   () -> Predicate.not(f -> f.isAnnotationPresent(Id.class)));
                saveSql.append(sqlFieldsValue);
            } else {
                sqlFieldsValue = getSqlValuesOfFields(declaredFields,() -> f -> true);
                saveSql.append(sqlFieldsValue);
            }
            if(sqlFieldsValue.equals("")){
                log.debug("Problem with getting names from fields");
                throw new RuntimeException("Problem with getting names from fields");
            }

            saveSql.append(VALUES)
                   .append(sqlFieldsValue);

            Long generatedKey = null;
            try (PreparedStatement ps = dataSource.getConnection().prepareStatement(saveSql.toString()
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
            if (currentId == 0L) {
                Long finalGeneratedKey = generatedKey;
                Arrays.stream(declaredFields).filter(field -> field.isAnnotationPresent(Id.class))
                              .forEach(field -> {
                                  field.setAccessible(true);
                                  try {
                                      field.set(o, finalGeneratedKey);
                                  } catch (IllegalAccessException e) {
                                      log.debug("{}", e.getMessage());
                                      throw new RuntimeException("Unable to set id of the object.");
                                  }
                              });
            }
        }
        return o;
    }

    private static String getSqlValuesOfFields(Field[] declaredFields, Supplier<Predicate<Field>> annotationPresent) {
        return Arrays.stream(declaredFields)
                     .filter(annotationPresent.get())
                     .map(f -> {
                         if (f.isAnnotationPresent(Column.class)) {
                             return f.getAnnotation(Column.class)
                                     .value();
                         } else {
                             return f.getName();
                         }
                     })
                     .collect(Collectors.joining(", ", LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
    }

    private static String getFieldNameOfObject(Object o, Field field) {
        if(field.isAnnotationPresent(Id.class)){
            field.setAccessible(true);
            try {
                return String.valueOf(field.getLong(o));
            } catch (IllegalAccessException e) {
                log.debug("Unable to get Id value of object.");
            }
        } else{
            if (field.isAnnotationPresent(Column.class)) {
                return field.getAnnotation(Column.class)
                            .value();
            } else {
                field.setAccessible(true);
                try {
                    return String.valueOf(field.get(o));
                } catch (IllegalAccessException e) {
                    log.debug("Unable to get value of field: {}",field.getName());
                    throw new RuntimeException("Unable to get value of field: " + field.getName());
                }
            }
        }
        return "";
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
