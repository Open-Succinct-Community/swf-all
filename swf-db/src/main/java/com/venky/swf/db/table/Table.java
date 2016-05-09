/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.BooleanConverter;
import com.venky.swf.db.JdbcTypeHelper.IntegerConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Counts;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.exceptions.AccessDeniedException;
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
    private final ModelReflector<M> reflector;
    public ModelReflector<M> getReflector() {
		return reflector;
	}
    public boolean isReal(){
    	if (reflector != null ){
        	IS_VIRTUAL isVirtual = reflector.getAnnotation(IS_VIRTUAL.class);
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
	        return modelClassTableNameCache.get(modelClass);
    	}
    }
    public static <M extends Model> String tableName(String modelClassSimpleName){
    	return modelNameTableNameCache.get(modelClassSimpleName);
    }
    
    private static Cache<Class<? extends Model>,String> modelClassTableNameCache = new Cache<Class<? extends Model>,String>(){
		private static final long serialVersionUID = 468418078793388786L;

		@Override
		protected String getValue(Class<? extends Model> modelClass) {
			String modelClassSimpleName = modelClass.getSimpleName();
			return modelNameTableNameCache.get(modelClassSimpleName);
		}
    	
    };
    
    private static Cache<String,String> modelNameTableNameCache = new Cache<String,String>(){
		private static final long serialVersionUID = 468418078793388786L;

		@Override
		protected String getValue(String modelClassSimpleName) {
			return StringUtil.underscorize(StringUtil.pluralize(modelClassSimpleName));
		}
    	
    };

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
        Transaction txn = Database.getInstance().getCurrentTransaction();
        DDL.DropTable q = new DDL.DropTable(getRealTableName());
        q.executeUpdate();
        txn.commit();
    }
    public void createTable() {
        Transaction txn = Database.getInstance().getCurrentTransaction();
        CreateTable q = createTableQuery();
        q.executeUpdate();
        txn.commit();
    }
    private CreateTable createTableQuery(){
    	return createTableQuery(getRealTableName());
    }
    private CreateTable createTableQuery(String tableName){
    	CreateTable q = new CreateTable(tableName);
        createFields(q);
        if (getReflector().getRealFields().contains("id")){
            q.addPrimaryKeyColumn(getReflector().getColumnDescriptor("id").getName());
        }
        return q;
    }
    
    private void createFields(CreateTable q){
        List<String> fields = reflector.getRealFields();
        SequenceSet<String> columnSpecs = new SequenceSet<String>();
        
        Iterator<String> fieldIterator = fields.iterator();
        while( fieldIterator.hasNext() ){
            String fieldName = fieldIterator.next();
            ColumnDescriptor d = reflector.getColumnDescriptor(fieldName);
            columnSpecs.add(d.toString());
        }
        for (String columnSpec:columnSpecs){
            q.addColumn(columnSpec);
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
            	Config.instance().getLogger(Table.class.getName()).info("Model: " + modelColumn.toString());
            	Config.instance().getLogger(Table.class.getName()).info("Table: " + tableColumn.toString());
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

        boolean dropAndReCreateTable = false;
        for (String fieldName:addedFields){
            AlterTable q = new AlterTable(getRealTableName());
        	ColumnDescriptor cd = reflector.getColumnDescriptor(fieldName);
            q.addColumn(cd.toString());
            q.executeUpdate();
        }
        
        for (String fieldName:alteredFields){
        	if (fieldName.equalsIgnoreCase("ID")){
        		dropAndReCreateTable = true;
        		continue;
        	}
        	ColumnDescriptor cd = reflector.getColumnDescriptor(fieldName);
        	if (!cd.isNullable() && Database.getJdbcTypeHelper().isVoid(cd.getColumnDefault())){
        		dropAndReCreateTable = true;
        		continue;
        	}
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

        if (dropAndReCreateTable){
        	// Rare event. Drop and recreate table.
        	String tmpTable = "temp_"+getRealTableName();
        	CreateTable create = createTableQuery(tmpTable);
        	create.executeUpdate();
        	
        	SequenceSet<String> columns = new SequenceSet<String>(); 
        	columns.addAll(reflector.getRealColumns());
        	
        	DataManupulationStatement insert = new DataManupulationStatement();
            insert.add("insert into ").add(tmpTable).add("(");
            Iterator<String> columnIterator = columns.iterator();
            while (columnIterator.hasNext()){
            	insert.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
            }
            insert.add(") select ");
            columnIterator = columns.iterator();
            while (columnIterator.hasNext()){
            	insert.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
            }
            insert.add(" from " + getRealTableName());
            insert.executeUpdate();
            
        	
            DropTable drop = new DropTable(getRealTableName());
            drop.executeUpdate();
            
            create = createTableQuery();
            create.executeUpdate();
            
            insert = new DataManupulationStatement();
            insert.add("insert into ").add(getRealTableName()).add("(");
            columnIterator = columns.iterator();
            while (columnIterator.hasNext()){
            	insert.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
            }
            insert.add(") select ");
            columnIterator = columns.iterator();
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
    }

    public int recordCount(){
    	Select sel = new Select("COUNT(1) AS COUNT").from(getModelClass());
    	Counts count = sel.execute(Counts.class).get(0); // number of records would be one.!!
    	return count.getCount();
    }
    
    
    public M lock(int id){
    	return lock(id,true);
    }
    public M lock(int id,boolean wait){
    	return get(id,true,wait);
    }
    public M get(int id) {
    	return get(id,false,false);
    }
    public M get(int id,boolean locked,boolean wait) {
    	Timer timer = Timer.startTimer(null, Config.instance().isTimerAdditive());
    	try {
	    	Select q = new Select(locked,wait);
	    	q.from(getModelClass());

	    	String idColumn = getReflector().getColumnDescriptor("id").getName();
	    	Timer createWhereTimer = Timer.startTimer(null, Config.instance().isTimerAdditive());
	        q.where(new Expression(idColumn,Operator.EQ,new BindVariable(id)));
	        createWhereTimer.stop();
	        
	        List<M> result = q.execute(getModelClass());
	        if (result.isEmpty()){
	            return null;
	        }else {
	            return result.get(0);
	        }
    	}finally{
    		timer.stop();
    	}
    }
    
    public int truncate(){
    	Select sel = new Select().from(getModelClass());
    	int numRecords = 0;
    	for (M m : sel.execute(getModelClass(),new Select.AccessibilityFilter<M>())){
    		m.destroy();
    		numRecords ++;
    	}
    	return numRecords;
    }
    
    public M newRecord(){
        return ModelInvocationHandler.getProxy(modelClass,new Record());
    }

    private Map<String,ColumnDescriptor> columnDescriptors = new IgnoreCaseMap<ColumnDescriptor>();
    
    public  Map<String,ColumnDescriptor> columnDescriptors(){
    	if (isReal() || ObjectUtil.equals(getRealTableName(), getTableName())){
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
    /*
    public Set<String> getAutoIncrementColumns(){ 
        Set<String> columns = new IgnoreCaseSet();
        for (String name :getColumnNames()){ 
            if (getColumnDescriptor(name).isAutoIncrement()){ 
                columns.add(name);
            }
        }
        return columns;
    }*/
    
    public static class ColumnDescriptor extends Record{
        public ColumnDescriptor(){
            
        }
        
        public int getOrdinalPosition(){
            Integer pos = ic.valueOf(get("ORDINAL_POSITION"));
            return (pos == null? 0 : pos);
        }
        
        public String getName(){
            return ((String)get("COLUMN_NAME"));
        }
        
        public int getJDBCType(){ 
        	Object dataType = get("DATA_TYPE");
        	return ic.valueOf(dataType);
        }
        
        public void setName(String name){
            put("COLUMN_NAME",name);
        }
        
        public void setJDBCType(int sqlType){
            put("DATA_TYPE",sqlType);
        }

        public int getSize(){
            Integer ret = ic.valueOf(get("COLUMN_SIZE"));
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
            Integer retval = ic.valueOf(get("DECIMAL_DIGITS"));
            if (retval == null){
                return 0;
            }else {
                return retval;
            }
        }
        
    	private BooleanConverter bc = (BooleanConverter) Database.getJdbcTypeHelper().getTypeRef(Boolean.class).getTypeConverter();
    	private IntegerConverter ic = (IntegerConverter) Database.getJdbcTypeHelper().getTypeRef(Integer.class).getTypeConverter();
        public boolean isNullable(){
        	return bc.valueOf(get("IS_NULLABLE"));
        }
        
        public void setNullable(boolean nullable){
            put("IS_NULLABLE",nullable ? "YES" : "NO");
        }
        
        public boolean isAutoIncrement(){ 
            return bc.valueOf(get("IS_AUTOINCREMENT"));
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
		
		public void setColumnDefault(String defaultValue){
			put("COLUMN_DEF",defaultValue);
		}
		
		public String getColumnDefault(){
			return (String)get("COLUMN_DEF");
		}

		@Override
        public String toString(){
            StringBuilder buff = new StringBuilder();
            JdbcTypeHelper helper = Database.getJdbcTypeHelper();
			TypeRef<?> ref = helper.getTypeRef(getJDBCType());
			if (ref == null){
				throw new RuntimeException("Unknown JDBCType:" + getJDBCType() + " for column " + getName());
			}
            if (helper.isColumnNameAutoLowerCasedInDB()){
            	buff.append(LowerCaseStringCache.instance().get(getName()));
            }else {
            	buff.append(getName());
            }
            if (isAutoIncrement()){
                buff.append(helper.getAutoIncrementInstruction());
            }else {
            	buff.append(" ");
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
                    String def = getColumnDefault();
                    if (def != null){
                    	buff.append(" DEFAULT " );
                    	if (ref.isColumnDefaultQuoted()){
                			if (def.startsWith("'")){
                				buff.append(def);
                			}else {
                				buff.append("'").append(def).append("'");
                			}
                    	}else {
                    		buff.append(def);
                    	}
                    }
                }
            }
            
            return buff.toString().trim();
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
	
	public M getRefreshed(M partiallyFilledModel){
		M fullModel = null;
		if (partiallyFilledModel.getId() > 0) {
			fullModel = Database.getTable(getModelClass()).get(partiallyFilledModel.getId());
		}else {
			for (Expression where : getReflector().getUniqueKeyConditions(partiallyFilledModel)){
				List<M> recordsMatchingUK = new Select().from(getModelClass()).where(where).execute();
				if (recordsMatchingUK.size() == 1){
					fullModel = recordsMatchingUK.get(0);
					break;
				}
			}
		}

		if (fullModel == null){
			fullModel = partiallyFilledModel;
		}else {
			if (!fullModel.isAccessibleBy(Database.getInstance().getCurrentUser())){
				throw new AccessDeniedException("Existing Record in " + getModelClass().getSimpleName() + " identified by "+ getReflector().get(fullModel,getReflector().getDescriptionField()) + " cannot be  modified.");
			}
			Record rawPartiallyFilledRecord = partiallyFilledModel.getRawRecord();
			Record rawFullRecord = fullModel.getRawRecord();
			for (String field: rawPartiallyFilledRecord.getDirtyFields()){
				rawFullRecord.put(field, partiallyFilledModel.getRawRecord().get(field));
			}
		}
		return fullModel;
	}

}
