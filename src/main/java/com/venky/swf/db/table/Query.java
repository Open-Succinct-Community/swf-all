/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;

/**
 *
 * @author venky
 */
public class Query {
    StringBuilder query = new StringBuilder();
    Class modelClass = null;
    public Query(Class modelClass){
        this.modelClass = modelClass;
    }
    public Query(){
        this(null);
    }
    
    List<BindVariable> parameters = new ArrayList<BindVariable>();
    
    public Query select(){
        return select(modelClass);
    }
    
    public Query select(Class modelClass){
        return select(Table.tableName(modelClass));
    }
    
    public Query select(String tableName){
        query.append("select * from ").append(tableName).append( " ");
        return this;
    }
    
    int maxRows = 0;
    public Query setMaxRows(int limit){
        this.maxRows = limit;
        return this;
    }
    
    public Query where(String fragment, BindVariable... bindVariables){
        return add(" where ").add(fragment, bindVariables);
    }

    public Query and(String fragment, BindVariable... bindVariables){
        return add(" and ").add(fragment, bindVariables);
    }
    
    public Query add(String fragment, BindVariable... bindVariables){
        query.append(" ").append(fragment).append(" ");
        return add(bindVariables);
    }
    
    public Query add(BindVariable... bindVariables){
        if (bindVariables != null && bindVariables.length > 0){
            parameters.addAll(Arrays.asList(bindVariables));
        }
        return this;
    }
    
    public Query orderBy(String... columnNames){
        for (int i = 0; i < columnNames.length ; i ++){
            if (i > 0){
                query.append(",");
            }
            query.append(" ").append(columnNames[i]); 
        }
        return this;
    }
 
    public <M extends Model> List<M> execute(){
        return execute(modelClass);
    }
    public <M extends Model> List<M> execute(Class<M> modelInterface) {
        PreparedStatement st = null;
        try {
            st = prepare();
            List<M> result = new ArrayList<M>();
            if (st.execute()){
                ResultSet rs = st.getResultSet();
                while (rs.next()){
                    Record r = new Record();
                    r.load(rs);
                    M m = ModelImpl.getProxy(modelInterface, r);
                    result.add(m);
                }
                rs.close();
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (st != null){
                try {
                    if (!st.isClosed()){
                        st.close();
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    
    private PreparedStatement prepare(String... columnNames) throws SQLException{
        PreparedStatement st = null;
        if (columnNames == null || columnNames.length == 0){
            st = Database.getInstance().getCurrentTransaction().createStatement(query.toString());
        }else {
            st = Database.getInstance().getCurrentTransaction().createStatement(query.toString(),columnNames);
        }
        for (int i = 0; i < parameters.size() ; i ++ ) {
            BindVariable value = parameters.get(i);
            if (value.getJdbcType() == Types.BLOB || value.getJdbcType() == Types.LONGVARBINARY){
            	st.setBinaryStream(i+1, value.getBinaryInputStream());
            }else if (value.getJdbcType() == Types.CLOB || value.getJdbcType() == Types.LONGVARCHAR){
            	st.setCharacterStream(i+1, value.getCharacterInputStream());
            }else {
            	st.setObject(i+1,value.getValue(), value.getJdbcType());
            }
        }
        return st;
    }
    public int executeUpdate(){
        return executeUpdate(null);
    }
    public int executeUpdate(Record generatedKeyValues,String... generatedKeyColumns){ 
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Executing {0}", query.toString());
        PreparedStatement st = null;
        try {
            st = prepare(generatedKeyColumns);
            int ret = st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys(); 
            if (generatedKeyValues != null && rs != null && rs.next()){
                generatedKeyValues.load(rs);
            }
            return ret;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (st != null){
                try {
                    if (!st.isClosed()){
                        st.close();
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        
    }
}
