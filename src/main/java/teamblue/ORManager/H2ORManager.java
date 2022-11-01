package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.annotations.*;
import teamblue.util.StringIdGenerator;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static teamblue.ORManager.MetaInfo.FieldInfo;
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
    void register(Class... entityClasses) {
        for (Class<? extends Class> entityClass : entityClasses) {

            if (entityClass.isAnnotationPresent(Entity.class)) {
                registerClass(entityClass);
            } else {
                log.error("Error creating table of name {}", entityClass.getSimpleName());
                throw new RuntimeException("Annotate POJO with @Entity to add it to DB as a table!");
            }
        }

/**
 ManyToOne feature
 */
        if (entityClasses.length > 1) {
            Field[] manyToOneAnnotatedFields =
                    Arrays.stream(entityClasses)
                            .filter(classToCheck -> classToCheck.isAnnotationPresent(Entity.class))
                            .map(cls -> cls.getDeclaredFields())
                            .flatMap(array -> Stream.of(array).filter(field -> field.isAnnotationPresent(ManyToOne.class)))
                            .toArray(Field[]::new);

            Class<? extends Class> manyToOneSideTable =
                    Arrays.stream(entityClasses).filter(classToCheck -> classToCheck.isAnnotationPresent(Entity.class))
                            .filter(cls -> Arrays.stream(cls.getDeclaredFields()).anyMatch(field -> field.isAnnotationPresent(ManyToOne.class)))
                            .findFirst().orElseThrow(() -> new RuntimeException("Incorrect mapping"));

            for (Class entityClassForOneToMany : entityClasses) {
                String id1MFieldName = null;
                Class oneToManyIdType = null;
                for (Field field1M : entityClassForOneToMany.getDeclaredFields()) {
                    if (field1M.isAnnotationPresent(Id.class)) {
                        if (field1M.isAnnotationPresent(Column.class)) {
                            id1MFieldName = field1M.getAnnotation(Column.class).value();
                        } else {
                            id1MFieldName = field1M.getName();
                        }
                        oneToManyIdType = field1M.getType();
                    }
                    if (field1M.isAnnotationPresent(OneToMany.class)) {
                        field1M.setAccessible(true);
                        String manyToOneTableName = getTableName(manyToOneSideTable);
                        String oneToManyTableName = getTableName(entityClassForOneToMany);
                        try (Connection conn = getConnectionWithDB()) {

                            String addForeignKeyColumnSql = ALTER_TABLE + manyToOneTableName + ADD
                                    + oneToManyTableName.toLowerCase() + _ID + oneToManyIdType.getSimpleName();

                            String foreignKeyConstraintSql = ALTER_TABLE + manyToOneTableName + ADD_FOREIGN_KEY +
                                    LEFT_PARENTHESIS + oneToManyTableName.toLowerCase() + _ID + RIGHT_PARENTHESIS + REFERENCES + oneToManyTableName
                                    + LEFT_PARENTHESIS + id1MFieldName + RIGHT_PARENTHESIS;

                            executeUpdate(addForeignKeyColumnSql, conn);
                            executeUpdate(foreignKeyConstraintSql, conn);

                        } catch (SQLException e) {
                            log.error("Error creating connection to DB");
                            throw new RuntimeException("Error creating connection to DB");
                        }
                    }
                }
            }
        }
    }

    void registerClass(Class<?> entityClass) {
        String tableName = "";

        tableName = getTableName(entityClass);

        List<Field> fields = new ArrayList<>();

        Field[] declaredFields = entityClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
/**
 ManyToOnefeature
 */
            if (!declaredField.isAnnotationPresent(OneToMany.class) && !declaredField.isAnnotationPresent(ManyToOne.class)) {
                fields.add(declaredField);
            }
        }

        StringBuilder baseSql =
                new StringBuilder(CREATE_TABLE_IF_NOT_EXISTS + tableName + LEFT_PARENTHESIS);

        for (int i = 0; i < fields.size(); i++) {
/**
 ManyToOne feature
 */
            getCastedTypeToH2(fields, i, baseSql);

            if (i != fields.size() - 1) {
                baseSql.append(COLON);
            }
        }
        baseSql.append(RIGHT_PARENTHESIS);

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement addTableStatement =
                    conn.prepareStatement(String.valueOf(baseSql));
            addTableStatement.executeUpdate();

            List<Field> columnFields = Arrays.stream(declaredFields)
                    .filter(f -> f.isAnnotationPresent(Column.class))
                    .toList();

            columnRename(tableName, columnFields);
        } catch (SQLException e) {
            log.error("Error of {} occured during creating table", e.getMessage());
        }

        log.info("Created table of name {}", entityClass.getSimpleName());
    }


    void columnRename(String tableName, List<Field> columnFields) throws SQLException {
        for (Field field : columnFields) {
            getConnectionWithDB().prepareStatement(ALTER_TABLE + tableName
                            + ALTER_COLUMN + field.getName()
                            .toUpperCase() + RENAME_TO +
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
 now not annoted with @Id POJO fields cast to H2 equivalennt type
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

    private static void executeUpdate(String sql, Connection conn) {
        PreparedStatement preparedStmt = null;
        try {
            preparedStmt = conn.prepareStatement(sql);
        } catch (SQLException e) {
            log.error("Error preparing update statement: {}", e.getMessage());
            throw new RuntimeException("Prepared statement syntax error");
        }
        try {
            preparedStmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error executing update: {}", e.getMessage());
            throw new RuntimeException("Executing update error");
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
        if (clazz.isAnnotationPresent(Entity.class)) {
            String valueId = getStringOfIdIfExist(object, clazz).orElse("");
            Object byId = null;

            try {
                byId = findById(valueId, clazz).orElse(null);
            } catch (Exception e) {
                log.debug(e.getMessage());
            }

            if (byId == null) {
                saveObject(object, clazz);
            } else {
                merge(object);
            }
        } else {
            log.info("Class missing @Entity annotation!");
        }
        return object;
    }

    protected void saveObject(Object object, Class<?> clazz) {
        List<Field> declaredFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(OneToMany.class)).toList();

        if (declaredFields.stream().findAny().isEmpty()) {
            log.info("No fields present to save to database.");
            return;
        }

        String generatedKeyString = null;
        try {
            generatedKeyString = StringIdGenerator.generate(clazz, getConnectionWithDB());
        } catch (SQLException e) {
            throw new RuntimeException(e.getSQLState());
        }

        Map<String, List<?>> listOfNamesValuesFieldsId = generateMapOfNamesAndValues(
                object, declaredFields, clazz, generatedKeyString);


        String sqlFieldName = listOfNamesValuesFieldsId.get("names").stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "
                        , LEFT_PARENTHESIS, RIGHT_PARENTHESIS));

        String sqlFieldValues = listOfNamesValuesFieldsId.get("values").stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        StringBuilder saveSql = new StringBuilder();
        saveSql.append(INSERT_INTO)
                .append(getTableName(Objects.requireNonNull(clazz)))
                .append(sqlFieldName)
                .append(VALUES)
                .append(LEFT_PARENTHESIS)
                .append(sqlFieldValues)
                .append(RIGHT_PARENTHESIS);


        List<?> idFields = listOfNamesValuesFieldsId.get("fieldId");
        Field idField = idFields.isEmpty() ? null : (Field) idFields.get(0);

        Object generatedKey = null;
        try {
            generatedKey = runSQLAndGetId(saveSql, idField);
        } catch (SQLException e) {
            log.debug("Unable to get correct generated ID.");
            log.debug("{}", e.getMessage());
            return;
        }

        if (!(generatedKey instanceof String) && idField != null) {
            if (!idField.getType().getSimpleName().equalsIgnoreCase("String")) {
                setFieldValueWithAnnotation(object, clazz, generatedKey, Id.class);
                log.info("Object of {} saved successfully with Id: {}", object.getClass()
                        .getSimpleName(), generatedKey);
            } else {
                setFieldValueWithAnnotation(object, clazz, generatedKeyString, Id.class);
                log.info("Object of {} saved successfully without Id", object.getClass()
                        .getSimpleName());
            }
        }
    }

    /*
     * Method only work for saving objects to DB
     * Return list of names, values and fields annotated with @Id
     */
    private Map<String, List<?>> generateMapOfNamesAndValues(Object object, List<Field> declaredFields, Class<?> clazz, String generatedKeyString) {
        Map<String, List<?>> map = new HashMap<>();
        List<String> listOfFieldsName = declaredFields.stream().map(this::getFieldName).toList();

        map.put("names", listOfFieldsName);

        List<String> fieldValues = getFieldValueForSaving(object, declaredFields)
                .stream().map(String::valueOf).toList();
        List<String> fieldValuesForSaving = new ArrayList<>();

        List<Field> fieldsId = new ArrayList<>();

        int i = 0;
        for (Field field : declaredFields) {
            String typeName = field.getType().getSimpleName();
            if (field.isAnnotationPresent(Id.class)) {
                if (typeName.equals("UUID") || typeName.equalsIgnoreCase("Long")) {
                    fieldValuesForSaving.add("default");
                } else {
                    fieldValuesForSaving.add("'" + generatedKeyString + "'");
                }
                fieldsId.add(field);
            } else {
                String value = fieldValues.get(i);
                fieldValuesForSaving.add(value.equals("null") ? value : "'" + value + "'");
            }
            i++;
        }
        map.put("values", fieldValuesForSaving);
        map.put("fieldId", fieldsId);

        return map;
    }

    private static void setFieldValueWithAnnotation(Object object, Class<?> clazz, Object valueToInsert, Class<? extends Annotation> classAnnotation) {
        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(classAnnotation))
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        field.set(object, valueToInsert);
                    } catch (IllegalAccessException e) {
                        log.debug("{}", e.getMessage());
                    }
                });
    }

    private Object runSQLAndGetId(StringBuilder saveSql, Field fieldId) throws SQLException {
        Object generatedKey = null;
        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(saveSql.toString(),
                    Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                if (fieldId == null) {
                    break;
                }
                String idObjectType = fieldId.getType().getSimpleName();
                if (idObjectType.equalsIgnoreCase("Long")) {
                    generatedKey = rs.getLong(1);
                } else if (idObjectType.equalsIgnoreCase("UUID")) {
                    generatedKey = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            log.debug("Unable to save object to database, some fields might be null. %n {}", e.getSQLState());
            throw new SQLException("Unable to save object.");
        }
        return generatedKey;
    }


    private List<Object> getFieldValueForSaving(Object object, List<Field> declaredFields) {
        return declaredFields.stream()
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .map(field -> {
                    field.setAccessible(true);
                    try {
                        if (field.isAnnotationPresent(ManyToOne.class)) {
                            Field innerField = Arrays.stream(field.get(object).getClass().getDeclaredFields())
                                    .filter(fieldO -> fieldO.isAnnotationPresent(Id.class))
                                    .findAny().orElseThrow();
                            innerField.setAccessible(true);
                            return innerField.get(field.get(object));
                        }
                        return field.get(object);
                    } catch (IllegalAccessException e) {
                        log.debug("Unable to access field: {}", field.getName());
                        throw new RuntimeException(e);
                    }
                })
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
    void persist(Object object) {
        String oClassName = object.getClass().getName();
        Class<?> clazz;
        try {
            clazz = Class.forName(oClassName);
        } catch (ClassNotFoundException e) {
            log.debug("Class was not found!");
            return;
        }
        if (clazz.isAnnotationPresent(Entity.class)) {
            String result = getStringOfIdIfExist(object, clazz).orElse("");
            if (result.equals("")) {
                saveObject(object, clazz);
            } else {
                throw new RuntimeException("Class should not have ID!");
            }
        } else {
            log.info("Class missing @Entity annotation!");
        }
    }

    private Optional<String> getStringOfIdIfExist(Object object, Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .map(field -> {
                    field.setAccessible(true);
                    try {
                        return field.get(object);
                    } catch (IllegalAccessException e) {
                        return "error";
                    }
                })
                .filter(Objects::nonNull)
                .map(Object::toString)
                .findAny();
    }


    private String getFieldName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            return field.getAnnotation(Column.class).value();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            return field.getName().toLowerCase() + "_id";
        } else {
            return field.getName();
        }
    }

    @Override
    <T> Optional<T> findById(Serializable id, Class<T> cls) {

        String tableName = getTableName(cls);

        Field fieldId = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst().get();

        String fieldName = getFieldName(fieldId);


        String sqlStatement = SELECT_ALL_FROM + tableName + WHERE + fieldName + EQUAL_QUESTION_MARK;
        Optional<T> result = Optional.empty();

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(sqlStatement);
            ps.setInt(1, (int) id);
            ResultSet rs = ps.executeQuery();
            Constructor<T> declaredConstructor = cls.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            MetaInfo metaInfo = new MetaInfo();

            while (rs.next()) {
                T newObject = declaredConstructor.newInstance();
                MetaInfo metaInfoOfClass = metaInfo.of(cls);
                for (FieldInfo field : metaInfoOfClass.getFieldInfos()) {
                    Object value = field.getRSgetter(rs);
                    Field thisField = field.getField();
                    thisField.setAccessible(true);
                    thisField.set(newObject, value);
                }

                result = Optional.of(newObject);


                log.info("Result from finding by id {} is {}", id, newObject);
            }


        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        if (result.isEmpty()) {
            log.info("There is no such object with given ID: {} in {}", id, cls.getSimpleName());
            throw new NoSuchElementException();
        }
        return result;
    }


    @Override
    <T> List<T> findAll(Class<T> cls) {

        String tableName = getTableName(cls);

        String findAllSql = SELECT_ALL_FROM + tableName;

        PreparedStatement findAllStmt = null;
        List<T> foundAll = new ArrayList<>();
        try (Connection conn = getConnectionWithDB()) {
            findAllStmt = conn.prepareStatement(findAllSql);
            ResultSet resultSet = null;
            resultSet = findAllStmt.executeQuery();
            Constructor<T> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);

            MetaInfo metaInfo = new MetaInfo();
            while (resultSet.next()) {
                T newObject = constructor.newInstance();
                MetaInfo metaInfoInstanceObjects = metaInfo.of(cls);
                for (var fieldInfo : metaInfoInstanceObjects.getFieldInfos()) {
                    var value = fieldInfo.getRSgetter(resultSet);
                    var field = fieldInfo.getField();
                    field.setAccessible(true);
                    field.set(newObject, value);
                }
                foundAll.add(newObject);
            }

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Exception of reflective operation");
        } catch (SQLException e) {
            throw new RuntimeException("SQL Exception");
        }

        return foundAll;
    }


    @Override
    <T> Iterable<T> findAllAsIterable(Class<T> cls) {
        return new FindAllIterator<>(cls);
    }

    @Override
    <T> Stream<T> findAllAsStream(Class<T> cls) {
        return null;
    }

    @Override
    <T> T merge(T o) {
        Class<?> cls = o.getClass();

        String valueOfField = getStringOfIdIfExist(o, cls).orElse("");
        if (valueOfField.equals("")) {
            throw new NoSuchElementException();
        }


        String fieldIdName = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .map(Field::getName)
                .findFirst().orElseThrow(() -> new RuntimeException("No field annotated as Id"));


        String tableName = getTableName(cls);

        MetaInfo metaInfo = new MetaInfo();
        MetaInfo of = metaInfo.of(cls);

        List<FieldInfo> fieldInfos = of.getFieldInfos();
        List<String> fieldValuesForSaving = getFieldValueForSaving(o, Arrays.stream(cls.getDeclaredFields()).toList())
                .stream()
                .map(String::valueOf)
                .toList();

        int i = 0;

        for (FieldInfo fieldInfo : fieldInfos) {

            if (!fieldInfo.columnName.equals(fieldIdName)) {
                String updateSql =
                        UPDATE + tableName + SET + fieldInfo.columnName + EQUAL_QUESTION_MARK + WHERE + fieldIdName + EQUAL_QUESTION_MARK;
                try (Connection conn = getConnectionWithDB()) {
                    PreparedStatement ps = conn.prepareStatement(updateSql);
                    ps.setObject(1, fieldValuesForSaving.get(i));
                    ps.setString(2, valueOfField);
                    ps.execute();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            i++;
        }

        return o;

    }

    @Override
    <T> T refresh(T o) {
        Class<?> cls = o.getClass();

        String valueOfField = getStringOfIdIfExist(o, cls).orElse("");
        if (valueOfField.equals("")) {
            throw new NoSuchElementException();
        }

        String fieldIdName = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .map(Field::getName)
                .findFirst().get();

        String tableName = getTableName(cls);

        MetaInfo metaInfo = new MetaInfo();
        MetaInfo of = metaInfo.of(cls);

        List<FieldInfo> fieldInfos = of.getFieldInfos();

        String retrieveSql = SELECT_ALL_FROM + tableName + WHERE + fieldIdName + EQUAL_QUESTION_MARK;

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(retrieveSql);
            ps.setString(1, valueOfField);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                for (FieldInfo fieldInfo : fieldInfos) {
                    if (!fieldInfo.columnName.equals(fieldIdName)) {
                        Object rSgetter = fieldInfo.getRSgetter(resultSet);
                        Field field = fieldInfo.getField();
                        field.set(o, rSgetter);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return o;
    }

    @Override
    boolean delete(Object o) {
        Class<?> classOfObject = o.getClass();
        String valueOfField = getStringOfIdIfExist(o, classOfObject).orElse("");

        if (!valueOfField.equals("")) {
            Field[] declaredFields = classOfObject.getDeclaredFields();
            String fieldName = Arrays.stream(declaredFields)
                    .filter(field -> field.isAnnotationPresent(Id.class))
                    .map(this::getFieldName)
                    .findAny().orElseThrow();

            String deleteSQL = DELETE_FROM + getTableName(classOfObject) +
                    WHERE + fieldName + EQUAL_QUESTION_MARK;

            try (Connection conn = getConnectionWithDB()) {
                PreparedStatement ps = conn.prepareStatement(deleteSQL);
                ps.setObject(1, valueOfField);
                ps.execute();
                log.debug("Object deleted from DB successfully.");
            } catch (SQLException e) {
                log.debug("Unable to delete object from DB. Message: {}", e.getSQLState());
            }
            Arrays.stream(declaredFields).filter(field -> field.isAnnotationPresent(Id.class))
                    .forEach(field -> {
                        try {
                            field.setAccessible(true);
                            if (field.getType().getSimpleName().equals("long")) {
                                field.set(o, 0L);
                            } else if (field.getType().getSimpleName().equals("Long")) {
                                field.set(o, null);
                            }
                        } catch (IllegalAccessException e) {
                            log.debug("Unable to set object a null/0 value");
                        }
                    });
            return true;
        }
        log.info("Unable to delete the object.");
        return false;
    }

    private class FindAllIterator<T> implements Iterable<T> {
        Class<T> clazz;

        public FindAllIterator(Class<T> cls) {
            this.clazz = cls;
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            Iterable.super.forEach(action);
        }

        public Iterator<T> iterator() {
            return new SimpleIterator<>(clazz);
        }
    }

    private class SimpleIterator<T> implements Iterator<T> {
        long cursor;
        long sizeOfTable;
        Class<T> clazz;

        public SimpleIterator(Class<T> clazz) {
            this.clazz = clazz;
            setSizeOfTable();
        }

        private void setSizeOfTable() {
            String tableName = getTableName(clazz);
            try (PreparedStatement ps = getConnectionWithDB().prepareStatement("SELECT COUNT(*) FROM " + tableName)) {
                ps.execute();
                ResultSet resultSet = ps.getResultSet();
                while (resultSet.next())
                    this.sizeOfTable = resultSet.getLong(1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            return cursor < sizeOfTable;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String tableName = getTableName(clazz);
            try (PreparedStatement ps = getConnectionWithDB().prepareStatement(SELECT_ALL_FROM + tableName + " LIMIT 1 " + "OFFSET " + cursor)) {
                ResultSet rs = ps.executeQuery();
                rs.next();
                Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                T newObject = declaredConstructor.newInstance();
                MetaInfo metaInfo = new MetaInfo();
                MetaInfo metaInfoInstanceObjects = metaInfo.of(clazz);
                for (var fieldInfo : metaInfoInstanceObjects.getFieldInfos()) {
                    var value = fieldInfo.getRSgetter(rs);
                    var field = fieldInfo.getField();
                    field.setAccessible(true);
                    field.set(newObject, value);
                }
                cursor++;
                return newObject;
            } catch (SQLException | ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


