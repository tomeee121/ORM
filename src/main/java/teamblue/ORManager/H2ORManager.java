package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.annotations.*;
import teamblue.util.NameConverter;
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

            Class<? extends Class> manyToOneSideTable =
                    Arrays.stream(entityClasses).filter(classToCheck -> classToCheck.isAnnotationPresent(Entity.class))
                            .filter(cls -> Arrays.stream(cls.getDeclaredFields()).anyMatch(field -> field.isAnnotationPresent(ManyToOne.class)))
                            .findFirst().orElseThrow(() -> new RuntimeException("Incorrect mapping"));

            for (Class entityClassForOneToMany : entityClasses) {
                String id1MFieldName = null;
                Class oneToManyIdType = null;
                for (Field field1M : entityClassForOneToMany.getDeclaredFields()) {
                    if (field1M.isAnnotationPresent(Id.class)) {
/**
                        Need info about name and type of Id by parent entity because it reflects foreign key attributes by child entity side
*/
                        id1MFieldName = NameConverter.getFieldName(field1M);
                        oneToManyIdType = field1M.getType();
                    }
                    if (field1M.isAnnotationPresent(OneToMany.class)) {

/**
                        If relationships on entities are mapped we can also extract table names and start writing some DDL
*/

                        field1M.setAccessible(true);
                        String manyToOneTableName = NameConverter.getTableName(manyToOneSideTable);
                        String oneToManyTableName = NameConverter.getTableName(entityClassForOneToMany);
                        try (Connection conn = getConnectionWithDB()) {

                            String addForeignKeyColumnSql = ALTER_TABLE + manyToOneTableName + ADD
                                    + oneToManyTableName.toLowerCase() + _ID + oneToManyIdType.getSimpleName(); // publisher_id - the name of FK (as example)

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

        tableName = NameConverter.getTableName(entityClass);

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
    public <T> T save(T object) {
        String oClassName = object.getClass().getName();
        Class<?> clazz;
        try {
            clazz = Class.forName(oClassName);
        } catch (ClassNotFoundException e) {
            log.debug("Class was not found!");
            return object;
        }
        if (clazz.isAnnotationPresent(Entity.class)) {
            String valueId = getValueIdFromObject(object, clazz).orElse("");
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

        Map<ElementOfSaveMap, List<?>> mapOfFieldAttributes = generateMapOfFieldAttributes(
                object, declaredFields);

        String sqlFieldName = mapOfFieldAttributes.get(ElementOfSaveMap.NAMES).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "
                        , LEFT_PARENTHESIS, RIGHT_PARENTHESIS));

        String sqlFieldValues = mapOfFieldAttributes.get(ElementOfSaveMap.VALUES).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        StringBuilder saveSql = new StringBuilder();
        saveSql.append(INSERT_INTO)
                .append(NameConverter.getTableName(clazz))
                .append(sqlFieldName)
                .append(VALUES)
                .append(LEFT_PARENTHESIS)
                .append(sqlFieldValues)
                .append(RIGHT_PARENTHESIS);

        Object generatedId = null;
        try {
            generatedId = runSQLAndGetGeneratedId(saveSql);
        } catch (SQLException e) {
            log.debug("Unable to get correct generated ID.");
            log.debug("{}", e.getMessage());
            throw new RuntimeException(e.getSQLState());
        }

        boolean idFieldPresent = declaredFields.stream().anyMatch(field -> field.isAnnotationPresent(Id.class));

        if(idFieldPresent){
            if (generatedId == null) {
                setFieldValueWithAnnotation(object, mapOfFieldAttributes.get(ElementOfSaveMap.GENERATED_STRING_ID).get(0), Id.class);
            } else {
                setFieldValueWithAnnotation(object, generatedId, Id.class);
            }
        }
    }

    /*
    * Method only work for saving objects to DB
    * Return list of names, values, fields and values annotated with @Id
    */
    private Map<ElementOfSaveMap, List<?>> generateMapOfFieldAttributes(Object object, List<Field> declaredFields) {
        Map<ElementOfSaveMap, List<?>> map = new EnumMap<>(ElementOfSaveMap.class);
        List<String> listOfFieldsName = declaredFields.stream().map(NameConverter::getFieldName).toList();

        map.put(ElementOfSaveMap.NAMES, listOfFieldsName);

        List<String> fieldValues = getFieldValues(object, declaredFields)
                .stream().map(String::valueOf).toList();

        List<String> fieldValuesForSaving = new ArrayList<>();
        List<String> fieldsIdValue = new ArrayList<>();
        int i = 0;

        for (Field field : declaredFields) {
            String typeName = field.getType().getSimpleName();
            if (field.isAnnotationPresent(Id.class)) {
                if (typeName.equals("UUID") || typeName.equalsIgnoreCase("Long")) {
                    fieldValuesForSaving.add("default");
                } else {
                    String generateId = StringIdGenerator.generate(object.getClass(), findAll(object.getClass()).size());
                    fieldValuesForSaving.add("'" + generateId + "'");
                    fieldsIdValue.add(generateId);
                }
            } else {
                String value = fieldValues.get(i);
                fieldValuesForSaving.add(value.equals("null") ? value : "'" + value + "'");
            }
            i++;
        }

        map.put(ElementOfSaveMap.VALUES, fieldValuesForSaving);
        map.put(ElementOfSaveMap.GENERATED_STRING_ID, fieldsIdValue);

        return map;
    }

    private static void setFieldValueWithAnnotation(Object object, Object valueToInsert, Class<? extends Annotation> classAnnotation) {
        Arrays.stream(object.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(classAnnotation))
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        field.set(object, valueToInsert);
                    } catch (IllegalAccessException e) {
                        log.debug("{}", e.getMessage());
                        throw new RuntimeException(e.getMessage());
                    }
                });
    }

    private Object runSQLAndGetGeneratedId(StringBuilder saveSql) throws SQLException {
        Object generatedValue = null;
        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(saveSql.toString(),
                    Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                generatedValue = rs.getObject(1);
            }
        } catch (SQLException e) {
            log.debug("Unable to save object to database, some fields might be null. %n {}", e.getSQLState());
        }
        return generatedValue;
    }


    private List<Object> getFieldValues(Object object, List<Field> declaredFields) {
        return declaredFields.stream()
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .map(field -> {
                    field.setAccessible(true);
                    try {
                        if (field.isAnnotationPresent(ManyToOne.class)) {
                            if (field.get(object) != null) {
                                Field innerField = Arrays.stream(field.get(object).getClass().getDeclaredFields())
                                        .filter(fieldO -> fieldO.isAnnotationPresent(Id.class))
                                        .findAny().orElseThrow();
                                innerField.setAccessible(true);
                                return innerField.get(field.get(object));
                            }
                            return null;
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
            String result = getValueIdFromObject(object, clazz).orElse("");
            if (result.equals("")) {
                saveObject(object, clazz);
            } else {
                throw new RuntimeException("Class should not have ID!");
            }
        } else {
            log.info("Class missing @Entity annotation!");
        }
    }

    private Optional<String> getValueIdFromObject(Object object, Class<?> clazz) {
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

    @Override
    <T> Optional<T> findById(Serializable id, Class<T> cls) {

        String tableName = NameConverter.getTableName(cls);

        Field fieldId = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst().get();

        String fieldName = NameConverter.getFieldName(fieldId);

        Optional<T> result = Optional.empty();

        if (Arrays.stream(cls.getDeclaredFields()).noneMatch(f -> f.isAnnotationPresent(ManyToOne.class))) {
            result = regularFindById(id, cls, tableName, fieldName);
        } else {
            result = manyToOneFindById(id, cls, tableName, fieldName);
        }

        if (result.isEmpty()) {
            log.info("There is no such object with given ID: {} in {}", id, cls.getSimpleName());
            throw new NoSuchElementException();
        }
        return result;
    }

    private <T> Optional<T> manyToOneFindById(Serializable id,
                                              Class<T> cls,
                                              String tableName,
                                              String fieldName
    ) {

        Field fieldManyToOne = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(OneToMany.class))
                .filter(f -> f.isAnnotationPresent(ManyToOne.class))
                .findFirst().orElseThrow();

        String innerTableName = getTableName(fieldManyToOne.getType());

        String manyToOneFieldName = fieldManyToOne.getName();

        String innerIdFieldName = Arrays.stream(fieldManyToOne.getType().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst().get().getName();


        Optional<T> result = Optional.empty();

        String sqlStatement =
                SELECT_ALL_FROM + tableName +
                        " INNER JOIN " + innerTableName +
                        " ON " + manyToOneFieldName + _ID + "=" + innerTableName + "." + innerIdFieldName +
                        WHERE + tableName + "." + fieldName + EQUAL_QUESTION_MARK;

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(sqlStatement);
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            Constructor<T> declaredConstructor = cls.getDeclaredConstructor();
            Constructor<T> declaredManyToOneConstructor = (Constructor<T>) fieldManyToOne.getType().getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            declaredManyToOneConstructor.setAccessible(true);
            MetaInfo metaInfo = new MetaInfo();
            MetaInfo manyToOneMetaInfo = new MetaInfo();

            while (rs.next()) {
                T newObject = declaredConstructor.newInstance();
                T manyToOne = declaredManyToOneConstructor.newInstance();
                MetaInfo metaInfoOfClass = metaInfo.of(cls);
                MetaInfo metaInfoOfManyToOne = manyToOneMetaInfo.of(fieldManyToOne.getType());
                for (FieldInfo field : metaInfoOfClass.getFieldInfos()) {

                    if (field.getField().getType().equals(fieldManyToOne.getType())) {

                        for (FieldInfo fieldMany : metaInfoOfManyToOne.getFieldInfos()) {
                            if (!fieldMany.getField().isAnnotationPresent(OneToMany.class)) {

                                Object value = fieldMany.getRSgetter(rs);
                                Field thisField = fieldMany.getField();
                                thisField.setAccessible(true);
                                thisField.set(manyToOne, value);
                            } else {
                                //TODO - show all books author has
                            }
                        }
                        Field thisField = field.getField();
                        thisField.setAccessible(true);
                        thisField.set(newObject, manyToOne);
                    } else {
                        Object value = field.getRSgetter(rs);
                        Field thisField = field.getField();
                        thisField.setAccessible(true);
                        thisField.set(newObject, value);
                    }
                }

                result = Optional.of(newObject);


                log.info("Result from finding by id {} is {}", id, newObject);
            }


        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private <T> Optional<T> regularFindById(Serializable id, Class<T> cls, String tableName, String fieldName) {

        Optional<T> result = Optional.empty();

        String sqlStatement = SELECT_ALL_FROM + tableName + WHERE + fieldName + EQUAL_QUESTION_MARK;

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(sqlStatement);
            ps.setObject(1, id);
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

        String tableName = NameConverter.getTableName(cls);

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

        String valueOfField = getValueIdFromObject(o, cls).orElse("");
        if (valueOfField.equals("")) {
            throw new NoSuchElementException();
        }


        String fieldIdName = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .map(Field::getName)
                .findFirst().orElseThrow(() -> new RuntimeException("No field annotated as Id"));


        String tableName = NameConverter.getTableName(cls);

        MetaInfo metaInfo = new MetaInfo();
        MetaInfo of = metaInfo.of(cls);

        List<FieldInfo> fieldInfos = of.getFieldInfos();
        List<String> fieldValuesForSaving = getFieldValues(o, Arrays.stream(cls.getDeclaredFields()).toList())
                .stream()
                .map(String::valueOf)
                .toList();

        int i = 0;
        String updateSql = "";

        for (FieldInfo fieldInfo : fieldInfos) {

            if (!fieldInfo.columnName.equals(fieldIdName)) {
                updateSql =
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

        String valueOfField = getValueIdFromObject(o, cls).orElse("");
        if (valueOfField.equals("")) {
            throw new NoSuchElementException();
        }

        String fieldIdName = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .map(Field::getName)
                .findFirst().get();

        String tableName = NameConverter.getTableName(cls);

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
        String valueOfField = getValueIdFromObject(o, classOfObject).orElse("");

        if (!valueOfField.equals("")) {
            return deleteWithId(o, classOfObject, valueOfField);
        } else {
            return deleteWithoutId(o, classOfObject);
        }
    }

    private boolean deleteWithoutId(Object o, Class<?> classOfObject) {
        List<Field> declaredFields = Arrays.stream(classOfObject.getDeclaredFields()).filter(f -> !f.isAnnotationPresent(OneToMany.class)).toList();
        Map<ElementOfSaveMap, List<?>> elementOfSaveMapListMap = generateMapOfFieldAttributes(o, declaredFields);
        List<?> sqlNames = elementOfSaveMapListMap.get(ElementOfSaveMap.NAMES);
        List<?> sqlValues = elementOfSaveMapListMap.get(ElementOfSaveMap.VALUES);

        StringBuilder deleteSQL = new StringBuilder();
        deleteSQL.append(DELETE_FROM)
                 .append(NameConverter.getTableName(classOfObject))
                 .append(WHERE);
        for (int i = 0; i < sqlNames.size(); i++){
            deleteSQL.append(sqlNames.get(i))
                    .append(" = ")
                    .append(sqlValues.get(i))
                    .append(" AND ");
        }
        deleteSQL.delete(deleteSQL.length() - 5, deleteSQL.length());
        deleteSQL.append(" LIMIT 1");

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(deleteSQL.toString());
            ps.execute();
            log.debug("Object deleted from DB successfully.");
        } catch (SQLException e) {
            log.debug("Unable to delete object from DB. Message: {}",e.getSQLState());
            return false;
        }
        return true;
    }

    private boolean deleteWithId(Object o, Class<?> classOfObject, String valueOfField) {
        Field[] declaredFields = classOfObject.getDeclaredFields();
        String fieldIdName = Arrays.stream(declaredFields)
                                 .filter(field -> field.isAnnotationPresent(Id.class))
                                 .map(NameConverter::getFieldName)
                                 .findAny().orElseThrow();

        String deleteSQL = DELETE_FROM + NameConverter.getTableName(classOfObject) +
                           WHERE + fieldIdName + EQUAL_QUESTION_MARK;

        try (Connection conn = getConnectionWithDB()) {
            PreparedStatement ps = conn.prepareStatement(deleteSQL);
            ps.setObject(1, valueOfField);
            ps.execute();
            log.debug("Object deleted from DB successfully.");
        } catch (SQLException e) {
            log.debug("Unable to delete object from DB. Message: {}",e.getSQLState());
            return false;
        }
        Arrays.stream(declaredFields).filter(field -> field.isAnnotationPresent(Id.class))
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        if (field.getType().isPrimitive()) {
                            field.set(o, 0L);
                        } else {
                            field.set(o, null);
                        }
                    } catch (IllegalAccessException e) {
                        log.debug("Unable to set object a null/0 value");
                    }
                });
        return true;
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
            String tableName = NameConverter.getTableName(clazz);
            try (PreparedStatement ps = getConnectionWithDB().prepareStatement("SELECT COUNT(*) FROM " + tableName)) {
                ps.execute();
                ResultSet resultSet = ps.getResultSet();
                while (resultSet.next()) {
                    this.sizeOfTable = resultSet.getLong(1);
                }
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
            String tableName = NameConverter.getTableName(clazz);
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
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        continue;
                    } else if (field.isAnnotationPresent(ManyToOne.class) && value instanceof Serializable val) {
                        Optional<?> byId = findById(val, field.getType());
                        if (byId.isPresent()){
                            field.set(newObject,byId.get());
                        }
                    } else {
                        field.set(newObject, value);
                    }
                }
                cursor++;
                return newObject;
            } catch (SQLException | ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}