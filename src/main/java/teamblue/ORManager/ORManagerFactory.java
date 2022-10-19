package teamblue.ORManager;


import teamblue.db.type.DBType;

import javax.sql.DataSource;

public interface ORManagerFactory {

    static ORManager withPropertiesFrom(String filename) {
        DataSource dataSourceFromFile = DataSourceFactory.createDataSourceFromFile(filename);
        DBType dbType = DataSourceFactory.getDbTypeFromDataSource(dataSourceFromFile);

        ORManager ORManager = chooseInstanceOfORManager(dbType, dataSourceFromFile);

        return ORManager;
    }

    static ORManager withDataSource(DataSource dataSource) {
        DBType dbType = DataSourceFactory.getDbTypeFromDataSource(dataSource);

        ORManager ORManager = chooseInstanceOfORManager(dbType, dataSource);

        return ORManager;
    }


    static ORManager chooseInstanceOfORManager(DBType dbType, DataSource dataSource){
        switch (dbType.name()) {
            case "H2" : return new H2ORManager(dataSource);
            case "MYSQL" : return new MySQLORManager(dataSource);
            case "POSTGRESQL" : return new PostgreSQLORManager(dataSource);
            case "ORACLE" : return new OracleORManager(dataSource);
            default:
                throw new RuntimeException("No support for this type of database! Please migrate.");
        }
    }
}
