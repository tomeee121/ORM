package teamblue.ORManager;

import lombok.*;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public abstract class ORManager {
    DataSource dataSource;

    abstract void register(Class... entityClasses);

    abstract Object save(Object o);

    abstract void persist(Object o);

    abstract <T> Optional<T> findById(Serializable id, Class<T> cls);

    abstract <T> List<T> findAll(Class<T> cls);

    abstract <T> Iterable<T> findAllAsIterable(Class<T> cls); // (MEDIUM)
    abstract <T> Stream<T> findAllAsStream(Class<T> cls);     // (OPTIONAL)


    abstract <T> T merge(T o);   // send o -> DB row (to table)
    abstract <T> T refresh(T o); // send o <- DB row (from table)

    abstract boolean delete(Object o);
}
