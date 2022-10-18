package teamblue.ORManager;


import javax.sql.DataSource;

public interface ORManagerFactory {

    static ORManager withPropertiesFrom(String filename) {
        DataSource dataSourceFromFile = dataSourceFactory.createDataSourceFromFile(filename);
        DBType dbType = dataSourceFactory.getDbTypeFromDataSource(dataSourceFromFile);

        ORManager orManager = ORManager.builder().dataSource(dataSourceFromFile).dbType(dbType).build();
        return orManager;
    }

    static ORManager withDataSource(DataSource dataSource) {
        DBType dbType = dataSourceFactory.getDbTypeFromDataSource(dataSource);

        ORManager orManager = ORManager.builder().dataSource(dataSource).dbType(dbType).build();
        return orManager;
    }
}
