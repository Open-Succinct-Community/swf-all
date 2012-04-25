/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.DDL;
import com.venky.swf.sql.DDL.AlterTable;
import com.venky.swf.sql.DDL.CreateTable;
import com.venky.swf.sql.DDL.DropTable;
import com.venky.swf.sql.DataManupulationStatement;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Update;




/**
 *
 * @author venky
 */
public class Table<M extends Model> {
    private final String tableName ;
    private final Class<M> modelClass;
    private final ModelReflector reflector;
    public ModelReflector getReflector() {
		return reflector;
	}
    public boolean isReal(){
    	if (reflector != null ){
        	IS_VIRTUAL isVirtual = reflector.getAnnotation(modelClass,IS_VIRTUAL.class);
        	if (isVirtual != null && isVirtual.value()) {
        		return false;
        	}
        	return true;
    	}else {
    		return StringUtil.equals(getRealTableName(),getTableName());
    	}
    }
    
    public boolean isVirtual(){
    	return !isReal();
    }

	private boolean existingInDatabase = false;

    public boolean isExistingInDatabase() {
        return existingInDatabase;
    }

    public void setExistingInDatabase(boolean existingInDatabase) {
        this.existingInDatabase = existingInDatabase;
    }
    
    @SuppressWarnings("unchecked")
	public Table(String tableName){
        this(tableName, (Class<M>)modelClass(tableName));
    }
    public Table(Class<M> modelClass){
        this(tableName(modelClass),modelClass);
    }
    
    private Table(String tableName, Class<M> modelClass){
        this.tableName = tableName; 
        this.modelClass = modelClass;
        if (modelClass != null){
        	this.reflector = ModelReflector.instance(modelClass);
        	this.realTableName = reflector.getTableName();
        }else {
        	this.reflector = null;
        	this.realTableName = this.tableName;
        } 
    }
    
    public static <M extends Model> String tableName(Class<M> modelClass){
    	if (modelClass == null){
    		return null;
    	}else {
    		return tableName(modelClass.getSimpleName());
    	}
    }
    
    public static String tableName(String modelClassSimpleName){
        return StringUtil.underscorize(StringUtil.pluralize(modelClassSimpleName));
    }
    public static String getSimpleModelClassName(String tableName){
    	return StringUtil.camelize(StringUtil.singularize(tableName));
    }
    public static Class<?> modelClass(String tableName){
        for (String root : Config.instance().getModelPackageRoots()){
            String className = root ; 
            if (!root.endsWith(".")){
                className += "."; 
            }
            className += getSimpleModelClassName(tableName);
            
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                //
            }
            
        }
        return null;
    }

    private final String realTableName;
    public String getRealTableName(){ 
    	return realTableName;
    }
    public String getTableName() {
        return tableName;
    }

    public Class<M> getModelClass() {
        return modelClass;
    }
    
    public void dropTable(){
        try {
            Transaction txn = Database.getInstance().getCurrentTransaction();
            DDL.DropTable q = new DDL.DropTable(getRealTableName());
            q.executeUpdate();
            txn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    public void createTable() {
        try {
            Transaction txn = Database.getInstance().getCurrentTransaction();
            CreateTable q = createTableQuery();
            q.executeUpdate();
            txn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    private CreateTable createTableQuery(){
        CreateTable q = new CreateTable(getRealTableName());
        createFields(q);
        if (getReflector().getRealFields().contains("id")){
            q.addPrimaryKeyColumn(getReflector().getColumnDescriptor("id").getName());
        }
        return q;
    }
    
    private void createFields(CreateTable q){
        List<String> fields = reflector.getRealFields();
        Iterator<String> fieldIterator = fields.iterator();
        while( fieldIterator.hasNext() ){
            String fieldName = fieldIterator.next();
            ColumnDescriptor d = reflector.getColumnDescriptor(fieldName);
            q.addColumn(d.toString());
        }
    }
    
    public static final String FIELDS_ADDED = "ADD";
    public static final String COLUMNS_DROPPED = "DROP";
    public static final String FIELDS_MODIFIED = "ALTER";
    
    public Map<String,Set<String>> getFieldsAltered(){
        Map<String,Set<String>> fieldsAltered = new IgnoreCaseMap<Set<String>>();
        fieldsAltered.put(FIELDS_ADDED, new IgnoreCaseSet());
        fieldsAltered.put(COLUMNS_DROPPED, new IgnoreCaseSet());
        fieldsAltered.put(FIELDS_MODIFIED, new IgnoreCaseSet());
        List<String> fields = reflector.getRealFields();
        List<String> columns = reflector.getRealColumns();
        Iterator<String> fieldIterator = fields.iterator();
        while( fieldIterator.hasNext() ){
            String fieldName = fieldIterator.next();
            ColumnDescriptor modelColumn = reflector.getColumnDescriptor(fieldName);
            ColumnDescriptor tableColumn = getColumnDescriptor(modelColumn.getName());
            if (tableColumn == null){
                fieldsAltered.get(FIELDS_ADDED).add(fieldName);
            }else if (!modelColumn.equals(tableColumn)){
            	fieldsAltered.get(FIELDS_MODIFIED).add(fieldName);
            }
        }
        for (ColumnDescriptor tableColumn : getColumnDescriptors()){ 
            if (!columns.contains(tableColumn.getName())){
                fieldsAltered.get(COLUMNS_DROPPED).add(tableColumn.getName());
            }
        }
        return fieldsAltered;
    }
    public boolean sync(){
        try {
            Map<String,Set<String>> fields = getFieldsAltered();
            Set<String> addedFields = fields.get(FIELDS_ADDED);
            Set<String> droppedColumns = fields.get(COLUMNS_DROPPED);
            Set<String> alteredFields = fields.get(FIELDS_MODIFIED);
            if (addedFields.isEmpty() && droppedColumns.isEmpty() && alteredFields.isEmpty()){
                return false;
            }
            
            Transaction txn = Database.getInstance().getCurrentTransaction();
            for (String columnName:droppedColumns){
                AlterTable q = new AlterTable(getRealTableName());
                q.dropColumn(columnName);
                q.executeUpdate();
            }

            for (String fieldName:addedFields){
                AlterTable q = new AlterTable(getRealTableName());
                q.addColumn(reflector.getColumnDescriptor(fieldName).toString());
                q.executeUpdate();
            }
            
            boolean idTypeChanged = false;
            for (String fieldName:alteredFields){
            	if (fieldName.equalsIgnoreCase("ID")){
            		idTypeChanged = true;
            		continue;
            	}
            	ColumnDescriptor cd = reflector.getColumnDescriptor(fieldName);
            	String columnName = cd.getName();
            	AlterTable q = new AlterTable(getRealTableName());
            	q.addColumn("NEW_"+cd.toString());
                q.executeUpdate();
                
            	Update u = new Update(getRealTableName());
            	u.setUnBounded("NEW_"+columnName, columnName);
            	u.executeUpdate();
            	
            	q = new AlterTable(getRealTableName());
            	q.dropColumn(columnName);
                q.executeUpdate();
                
                q = new AlterTable(getRealTableName());
                q.addColumn(cd.toString());
                q.executeUpdate();
                
                u = new Update(getRealTableName());
            	u.setUnBounded(columnName,"NEW_" + columnName);
            	u.executeUpdate();
                
                q = new AlterTable(getRealTableName());
            	q.dropColumn("NEW_" + columnName);
                q.executeUpdate();
            }

            if (idTypeChanged){
            	// Rare event. Drop and recreate table.
            	CreateTable create = new CreateTable("temp_"+getRealTableName()).as("select * from "+ getRealTableName());
            	create.executeUpdate();
            	
                DropTable drop = new DropTable(getRealTableName());
                drop.executeUpdate();
                
                create = createTableQuery();
                create.executeUpdate();
                
                DataManupulationStatement insert = new DataManupulationStatement();
                insert.add("insert into ").add(getRealTableName()).add("(");
                Iterator<String> columnIterator = reflector.getRealColumns().iterator();
                while (columnIterator.hasNext()){
                	insert.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
                }
                insert.add(") select ");
                columnIterator = reflector.getRealColumns().iterator();
                while (columnIterator.hasNext()){
                	insert.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
                }
                insert.add(" from temp_" + getRealTableName());
                insert.executeUpdate();
                
                drop = new DropTable("temp_"+getRealTableName());
                drop.executeUpdate();
            }
            txn.commit();
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public M get(int id) {
        Select q = new Select().from(getModelClass());
        String idColumn = getReflector().getColumnDescriptor("id").getName();
        q.where(new Expression(idColumn,Operator.EQ,new BindVariable(id)));
        List<M> result = q.execute(getModelClass());
        if (result.isEmpty()){
            return null;
        }else {
            return result.get(0);
        }
    }
    
    public M newRecord(){
        return ModelInvocationHandler.getProxy(modelClass,new Record());
    }

    private Map<String,ColumnDescriptor> columnDescriptors = new IgnoreCaseMap<ColumnDescriptor>();
    
    public  Map<String,ColumnDescriptor> columnDescriptors(){
    	if (isReal()){
    		return columnDescriptors; 
    	}else {
			return Database.getTable(getRealTableName()).columnDescriptors();
    	}
    }
    
    public Set<String> getColumnNames(){ 
        return columnDescriptors().keySet();
    }
    public Collection<ColumnDescriptor> getColumnDescriptors(){ 
        return columnDescriptors().values();
    }
    public ColumnDescriptor getColumnDescriptor(String columnName){
        return getColumnDescriptor(columnName, false);
    }
    public ColumnDescriptor getColumnDescriptor(String columnName,boolean createIfRequired){
		Map<String,ColumnDescriptor> cds = columnDescriptors();
        ColumnDescriptor c = cds.get(columnName);
        if (c == null && createIfRequired){
            c = new ColumnDescriptor();
            cds.put(columnName, c);
        }
        return c;
    }

    public Set<String> getAutoIncrementColumns(){ 
        Set<String> columns = new IgnoreCaseSet();
        for (String name :getColumnNames()){ 
            if (getColumnDescriptor(name).isAutoIncrement()){ 
                columns.add(name);
            }
        }
        return columns;
    }
    public static class ColumnDescriptor extends Record{
        public ColumnDescriptor(){
            
        }
        
        public int getOrdinalPosition(){
            Integer pos = (Integer)get("ORDINAL_POSITION");
            return (pos == null? 0 : pos);
        }
        
        public String getName(){
            return ((String)get("COLUMN_NAME"));
        }
        
        public int getJDBCType(){ 
            return (Integer)get("DATA_TYPE");
        }
        
        public void setName(String name){
            put("COLUMN_NAME",name);
        }
        
        public void setJDBCType(int sqlType){
            put("DATA_TYPE",sqlType);
        }

        public int getSize(){
            Integer ret = (Integer)get("COLUMN_SIZE");
            if (ret == null){
                return 0;
            }else {
                return ret;
            }
        }
        
        public void setSize(int size){
            put("COLUMN_SIZE",size);
        }
        
        public void setPrecision(int size){
            setSize(size);
        }
        
        public int getPrecision(){
            return getSize();
        }
        
        public void setScale(int scale){
            put("DECIMAL_DIGITS",scale);
        }

        public int getScale(){
            Integer retval = (Integer)get("DECIMAL_DIGITS");
            if (retval == null){
                return 0;
            }else {
                return retval;
            }
        }
        
        public boolean isNullable(){ 
            return "YES".equals(get("IS_NULLABLE"));
        }
        
        public void setNullable(boolean nullable){
            put("IS_NULLABLE",nullable ? "YES" : "NO");
        }
        
        public boolean isAutoIncrement(){ 
            return "YES".equals(get("IS_AUTOINCREMENT"));
        }
        
        public void setAutoIncrement(boolean autoincrement){
            put("IS_AUTOINCREMENT",autoincrement ? "YES": "NO");
        }
        
        private boolean virtual = false;
        
        
        public boolean isVirtual() {
			return virtual;
		}

		public void setVirtual(boolean virtual) {
			this.virtual = virtual;
		}

		@Override
        public String toString(){
            StringBuilder buff = new StringBuilder();
			TypeRef<?> ref = Database.getJdbcTypeHelper().getTypeRef(getJDBCType());
            
            buff.append(getName()).append(" ");
            if (isAutoIncrement()){
                buff.append(Database.getJdbcTypeHelper().getAutoIncrementInstruction());
            }else {
                buff.append(ref.getSqlType());
                if (ref.getSize() > 0 && getSize() > 0){ 
                    buff.append("(").append(getSize()); 
                    if (ref.getScale() > 0 && getScale() > 0){
                        buff.append(",").append(getScale());
                    }    
                    buff.append(")");
                }
                if (!isNullable()){
                    buff.append(" NOT NULL ");
                }
            }
            
            return buff.toString();
        }

        @Override
        public boolean equals(Object other){
            if (other == null || !(other instanceof ColumnDescriptor)){
                return false;
            }
            ColumnDescriptor othercd = (ColumnDescriptor)other;
            return  toString().equalsIgnoreCase(othercd.toString()) ;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
    
}
