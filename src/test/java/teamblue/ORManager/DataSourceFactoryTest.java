package teamblue.ORManager;

import org.junit.Test;
import teamblue.db.type.DBType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

public class DataSourceFactoryTest {

    @Test
    public void smokeTestWithDataSource() {

        //given
        //when
        Connection connection;
        try {
            connection = HikariDataSourceSample.getH2DataSource()
                                               .getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //then
        assertThat(connection)
                  .isNotNull();
    }

    @Test
    public void smokeTestWithFile() {

        //given
        //when
        Connection connection;
        ORManager orManager = ORManagerFactory.withPropertiesFrom("db.file");
        try {connection = orManager.getDataSource()
                                              .getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //then
        assertThat(connection)
                  .isNotNull();
    }

    @Test
    public void shouldThrowException_whenNotExistingPropsFilePassed() {

        //given
        //when
        //then
        assertThatThrownBy(() -> ORManagerFactory.withPropertiesFrom("nothing"))
                  .isInstanceOf(RuntimeException.class)
                  .hasMessage("Could not find given properties file!");
    }

    @Test
    public void shouldThrowException_whenNotExistingDriverPassedToDataSource() {

        //given
        //when
        //then
        assertThatThrownBy(() -> ORManagerFactory.withDataSource(HikariDataSourceSample.getH2DataSourceWithFakeDriver()))
                  .isInstanceOf(RuntimeException.class)
                  .hasMessage("Failed to load driver class wrong in either of HikariConfig class loader or Thread context classloader");
    }

    @Test
    public void shouldDecoratorORManagerObjectFromH2DataSourceCreated_whenHaveCorrectInfoAboutTypeOfDB() {

        //given
        DataSource h2DataSource = HikariDataSourceSample.getH2DataSource();

        //when
        DBType dbTypeFromDataSource = DataSourceFactory.getDbTypeFromDataSource(h2DataSource);
        //then
        assertThat(dbTypeFromDataSource.name()).isEqualTo("H2");
    }

}