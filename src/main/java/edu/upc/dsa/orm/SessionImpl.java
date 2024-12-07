package edu.upc.dsa.orm;

import edu.upc.dsa.DB.SQLNotInsert;
import edu.upc.dsa.DB.SQLNotSelect;
import edu.upc.dsa.util.QueryHelper;
import edu.upc.dsa.util.ObjectHelper;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

public class SessionImpl implements Session {
    private final Connection conn;

    public SessionImpl(Connection conn) {
        this.conn = conn;
    }

    public void save(Object entity) throws SQLException{

        String InsertQuery = QueryHelper.CreateQueryINSERT(entity);
        PreparedStatement pstm = conn.prepareStatement(InsertQuery);

        int i = 1;
        for (String field: ObjectHelper.getFields(entity)) {
            try{
                if(!entity.getClass().getDeclaredField(field).isAnnotationPresent(SQLNotInsert.class)){
                    pstm.setObject(i++, ObjectHelper.getter(entity, field));
                }
            }catch(NoSuchFieldException ignored){}

        }

        pstm.executeQuery();

    }

    public void close(){
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public <T> T get(Class<T> theClass, int ID) throws SQLException{

        String sql = QueryHelper.CreateQuerySELECTbyId(theClass);

        T object;
        try {
            object = theClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }

        PreparedStatement pstm = conn.prepareStatement(sql);
        pstm.setObject(1, ID);
        pstm.executeQuery();

        ResultSet res = pstm.getResultSet();
        if(!res.next()) return null;
        ResultSetMetaData rsmd = res.getMetaData();



        int numColumns = rsmd.getColumnCount();
        for (int i = 0; i < numColumns; i++) {
            try {
                String key = rsmd.getColumnName(i);
                if(theClass.getDeclaredField(key).isAnnotationPresent(SQLNotSelect.class)) continue;
                Object value = res.getObject(i);
                ObjectHelper.setter(object, key, value);
            } catch (Exception ignored) {}
        }

        return object;
    }

    public <T> void update(Class<T> theClass, Map<String, Object> changes, Map<String, Object> selectors) throws SQLException{
        String sql = QueryHelper.CreateUpdate(theClass, changes, selectors);
        PreparedStatement pstm = conn.prepareStatement(sql);
        int i = 1;
        for(Object val : changes.values()){
            pstm.setObject(i++, val);
        }
        for(Object val : selectors.values()){
            pstm.setObject(i++, val);
        }
        pstm.executeQuery();
    }

    public <T> void update(Class<T> theClass, Map<String, Object> changes, int id) throws SQLException{
        update(theClass, changes, Map.of("ID", id));
    }

    public <T> void delete(Class<T> theClass, Map<String, Object> params) throws SQLException {
        String sql = QueryHelper.CreateQueryDELETEbyParams(theClass, params);
        PreparedStatement pstm = conn.prepareStatement(sql);
        {
            int i = 1;
            for(Object o : params.values()){
                pstm.setObject(i++, o);
            }
        }
        pstm.executeQuery();
    }

    public <T> void delete(Class<T> theClass, int id) throws SQLException {
        delete(theClass, Map.of("id", id));
    }

    public <T> List<T> findAll(Class<T> theClass) throws SQLException{
        return findAll(theClass, new HashMap<>());
    }

    public <T> List<T> findAll(Class<T> theClass, Map<String, Object> params) throws SQLException{
        String sql = QueryHelper.CreateQuerySELECTbyParams(theClass, params);
        PreparedStatement pstm = conn.prepareStatement(sql);
        List<T> results = new ArrayList<>();
        {
            int i = 1;
            for(Object o : params.values()){
                pstm.setObject(i++, o);
            }
        }
        ResultSet res = pstm.executeQuery();
        ResultSetMetaData rsmd = res.getMetaData();

        while (res.next()) {
            int numColumns = rsmd.getColumnCount();
            T object;
            try{
                object = theClass.getDeclaredConstructor().newInstance();
            }catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                continue;
            }
            for (int i = 0; i < numColumns; i++) {
                try {
                    String key = rsmd.getColumnName(i);
                    if(theClass.getDeclaredField(key).isAnnotationPresent(SQLNotSelect.class)) continue;
                    Object value = res.getObject(i);
                    ObjectHelper.setter(object, key, value);
                } catch (Exception ignored) {}
            }
            results.add(object);
        }

        return results;
    }

}