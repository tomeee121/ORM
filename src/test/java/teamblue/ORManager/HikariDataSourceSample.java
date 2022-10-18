package teamblue.ORManager;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;


public interface HikariDataSourceSample {
    static DataSource getH2DataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.h2.Driver");
        hikariConfig.setJdbcUrl("jdbc:h2:./src/main/H2DBfile");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");

        HikariDataSource hikariDataSourceSample = new HikariDataSource(hikariConfig);
        return hikariDataSourceSample;
    }

    static DataSource getH2DataSourceWithFakeDriver() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("wrong");
        hikariConfig.setJdbcUrl("jdbc:h2:./src/main/H2DBfile");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");

        HikariDataSource hikariDataSourceSample = new HikariDataSource(hikariConfig);
        return hikariDataSourceSample;
    }

}
