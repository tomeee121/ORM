package teamblue.ORManager;

import lombok.Builder;
import lombok.Getter;

import javax.sql.DataSource;

@Builder
@Getter
public class ORManager {
    DataSource dataSource;
    DBType dbType;
}
