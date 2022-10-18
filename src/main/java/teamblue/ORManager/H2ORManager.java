package teamblue.ORManager;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class H2ORManager extends ORManager {

    H2ORManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    void register(Class... entityClasses) {

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
