package teamblue.ORManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

public class dataSourceFactory {

    static DataSource createDataSourceFromFile(String file) {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find given properties file!");
        }
        try {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Properties object could not load from given file!");
        }


        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(props.getProperty("DB_DRIVER_CLASS"));
        hikariConfig.setJdbcUrl(props.getProperty("DB_URL"));
        hikariConfig.setUsername(props.getProperty("DB_USERNAME"));
        hikariConfig.setPassword(props.getProperty("DB_PASSWORD"));

        if (props.getProperty("POOL_SIZE") != null) {
            hikariConfig.setMaximumPoolSize(Integer.parseInt(props.getProperty("POOL_SIZE")));
        } else hikariConfig.setMaximumPoolSize(5);

        hikariConfig.setPoolName("ORM_POOL");
        hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", "true");

        if (props.getProperty("PREP_STATEMENT_CACHE_SIZE") != null) {
            hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", props.getProperty("PREP_STATEMENT_CACHE_SIZE"));
        } else hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", "250");

        hikariConfig.setPoolName("ORM_POOL");
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts", "true");

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        return dataSource;
    }

    static DBType getDbTypeFromDataSource(DataSource dataSource) {
        String dbName = null;
        try {
            dbName = dataSource.getConnection().getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException("Could not get the db type name!");
        }

        for (DBType enumName : DBType.values()){
            if(dbName.toUpperCase().contains(enumName.name())) {
                return enumName;
            }
        }

        throw new RuntimeException("Incorrect DB type chosen!");
    }
}
