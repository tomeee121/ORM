package teamblue.ORManager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public abstract class ORManager {
    DataSource dataSource;

    abstract Connection getConnectionWithDB() throws SQLException;

    abstract void register(Class... entityClasses) throws SQLException;

    abstract <T> T save(T o);

    abstract void persist(Object o);

    abstract <T> Optional<T> findById(Serializable id, Class<T> cls);

    abstract <T> List<T> findAll(Class<T> cls);

    abstract <T> Iterable<T> findAllAsIterable(Class<T> cls); // (MEDIUM)
    abstract <T> Stream<T> findAllAsStream(Class<T> cls);     // (OPTIONAL)


    abstract <T> T merge(T o);   // send o -> DB row (to table)
    abstract <T> T refresh(T o); // send o <- DB row (from table)

    abstract boolean delete(Object o);
}
