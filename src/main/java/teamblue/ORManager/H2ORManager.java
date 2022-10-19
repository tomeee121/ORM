package teamblue.ORManager;

import lombok.extern.slf4j.Slf4j;
import teamblue.annotations.Entity;
import teamblue.annotations.PrimaryKey;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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
    void register(Class... entityClasses) throws SQLException {
        for (Class entityClass : entityClasses) {
            if (entityClass.isAnnotationPresent(Entity.class)) {
                List<Field> fields = new ArrayList<>();
                String primaryKeyName = null;

                Field[] declaredFields = entityClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(PrimaryKey.class)) primaryKeyName = declaredField.getName();
                    fields.add(declaredField);
                }

                String dropStatementQuery = DROP + entityClass.getSimpleName();
                PreparedStatement dropPriorTableStmt = dataSource.getConnection().prepareStatement(dropStatementQuery);
                dropPriorTableStmt.executeUpdate();

                StringBuilder baseSql =
                        new StringBuilder(CREATE_TABLE_IF_NOT_EXISTS + entityClass.getSimpleName() + LEFT_PARENTHESIS);

                for (int i = 0; i < fields.size(); i++) {
                    if (fields.get(i).isAnnotationPresent(PrimaryKey.class)) {
                        baseSql.append(fields.get(i).getName() + UUID);
                    } else if ("String".equals(fields.get(i).getType().getSimpleName())) {
                        baseSql.append(fields.get(i).getName() + VARCHAR_255);
                    } else if ("LocalDate".equals(fields.get(i).getType().getSimpleName())) {
                        baseSql.append(fields.get(i).getName() + DATE);
                    }
                    if(i != fields.size() - 1) {
                        baseSql.append(COLON);
                    }
                }
                baseSql.append(RIGHT_PARENTHESIS);

                PreparedStatement addTableStatement = dataSource.getConnection()
                                                                .prepareStatement(String.valueOf(baseSql));
                addTableStatement.executeUpdate();
            } else {
                throw new RuntimeException("Annotate POJO with @Entity to add it to DB as a table!");
            }
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
