package teamblue.ORManager;

import teamblue.annotations.Column;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MetaInfo {
    List<FieldInfo> fields = new ArrayList<>();
    static Map<Class, Set<Object>> cache = new HashMap<>();
    static boolean isCacheUpToDate = false;

    MetaInfo() {
    }

    MetaInfo(List<FieldInfo> fields) {
        this.fields = fields;
    }


    MetaInfo of(Class cls) {
        Arrays.stream(cls.getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .forEach(field -> fields.add(
                        (new FieldInfo(field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).value() : field.getName(), field, cls))));
        return new MetaInfo(fields);
    }

    static Set<Object> newCache = new HashSet<>();
    static void establishNewCache(List<Object> newlyFoundInDBObjects, Class cls) {
        Set<Object> metaInfoFromCache = getCache().get(cls);
        for (var latelyFound : newlyFoundInDBObjects) {
            if (!metaInfoFromCache.contains(latelyFound)) {
                newCache.add(latelyFound);
            }
        }

        Set<Object> oldCache = getCache().get(cls);
        Set<Object> wholeCache = Stream.of(oldCache, newCache)
                .flatMap(list -> list.stream()).collect(Collectors.toSet());
        addToCache(cls, wholeCache);
    }

    static void addToCache(Class cls, Set objects) {
        objects.stream().forEach(upgradeCacheWithEl -> cache.get(cls).add(upgradeCacheWithEl));
    }

    static void clearCache() {
        newCache.clear();
        cache.values().clear();
    }

    public static Map<Class, Set<Object>> getCache() {
        return cache;
    }

    public static void setCache(Map<Class, Set<Object>> cache) {
        MetaInfo.cache = cache;
    }

    public static boolean isIsCacheUpToDate() {
        return isCacheUpToDate;
    }

    public static void setIsCacheUpToDate(boolean isCacheUpToDate) {
        MetaInfo.isCacheUpToDate = isCacheUpToDate;
    }

    List<FieldInfo> getFieldInfos() {
        return fields;
    }

    public static class FieldInfo {
        String columnName;
        Field field;
        Class type;

        public FieldInfo(String columnName, Field field, Class type) {
            this.columnName = columnName;
            this.field = field;
            this.type = type;
        }

        public Field getField() {
            return field;
        }

        public Object getRSgetter(ResultSet rs) {
            try {
                if (rs.getObject(columnName) instanceof Date date) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    String format1 = formatter.format(date);
                    LocalDate dateFormatted = LocalDate.parse(format1);
                    return dateFormatted;
                }
                return rs.getObject(columnName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
