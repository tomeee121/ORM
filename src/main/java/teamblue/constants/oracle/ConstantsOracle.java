package teamblue.constants.oracle;

public class ConstantsOracle {
    public static final String SELECT_ALL_FROM = "SELECT * FROM ";
    public static final String ALTER = " ALTER ";
    public static final String RENAME_TO = " RENAME TO ";

    public static final String EQUAL_QUESTION_MARK = " =? ";
    public static final String COLON = ", ";
    public static final String UUID = " UUID";
    public static final String AUTO_INCREMENT = " AUTO_INCREMENT ";
    public static final String PRIMARY_KEY = "PRIMARY KEY";
    // Oracle doesn't provide IF EXISTS clause in the DROP TABLE statement
    public static final String DROP_IF_EXISTS_BOOKS = "";

    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String VALUES = " VALUES ";
    public static final String LEFT_PARENTHESIS = " (";
    public static final String RIGHT_PARENTHESIS = ")";
    public static final String ALTER_TABLE = "ALTER TABLE ";
    public static final String MODIFY = " MODIFY ";
    // Oracle doesn't provide this statement
    public static final String CREATE_TABLE_IF_NOT_EXISTS = "";
    // Needs to be implemented
    public static final String PRIMARY_KEY_AUTO_INCREMENT = "";
    public static final String FOREIGN_KEY = "FOREIGN KEY";
    public static final String REFERENCES = " REFERENCES ";
    public static final String VARCHAR_255 = " VARCHAR(255)";
    public static final String DATE = " DATE";
    public static final String INTEGER = " INTEGER";
    public static final String BIGINT = " BIGINT";
    public static final String DOUBLE_PRECISION = " DOUBLE PRECISION";
    // Does not provide boolean data type
    public static final String BOOLEAN = "";

    public static final String DROP = "DROP TABLE ";
}
