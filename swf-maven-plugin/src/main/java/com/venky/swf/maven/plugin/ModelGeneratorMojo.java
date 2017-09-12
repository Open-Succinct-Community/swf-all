package com.venky.swf.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.DECIMAL_DIGITS;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.DBPOOL;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.routing.Config;

/**
 * Goal which generates models from database a.k.a reverse engineering.
 * 
 */
@Mojo(defaultPhase=LifecyclePhase.COMPILE,name="generate-model")

public class ModelGeneratorMojo extends AbstractMojo {
	
	@Parameter(property="srcDir",defaultValue="src/main/java")
	File srcDir;
	
	@Parameter(property="pool",defaultValue="")
	String pool;
	
	@Parameter(property="tableName",defaultValue="")
	String tableName;
	
	@Parameter(property="project.build.sourceEncoding",defaultValue="UTF-8")
	String encoding;
	
	
	public void execute() throws MojoExecutionException {
		getLog().info("Generating models...");
		generateModels(srcDir);
		getLog().info("Done");
	}
	
    public void generateModels(File directory) throws MojoExecutionException{
        Database.loadTables(true);
        if (!ObjectUtil.isVoid(this.pool)){
        	generateModelClass(directory, pool);
        }else {
	        for (String pool : ConnectionManager.instance().getPools()){
	        	generateModelClass(directory, pool);
	        }
        }
        Database.getInstance().getCurrentTransaction().rollback(null);
        Database.getInstance().close();
    }
    private void generateModelClass(File directory, String pool) throws MojoExecutionException {
	    for (Table<?> table: Database.getTables(pool).values()){
            generateModelClass(table, directory,pool);
        }
    }
    
    private void generateModelClass(Table<?> table,File directory,String pool) throws MojoExecutionException{
    	if (!ObjectUtil.isVoid(this.tableName) && !ObjectUtil.equals(table.getTableName(), this.tableName)) {
    		return;
    	}
    	String simpleModelClassName = Table.getSimpleModelClassName(table.getTableName());
    	String fQModelClassName = null; 
    	StringBuilder packageName = new StringBuilder(Config.instance().getModelPackageRoots().get(0));
    	if (packageName.charAt(packageName.length()-1) == '.'){
    		packageName.setLength(packageName.length() -1 );
    	}
    	
    	fQModelClassName =  packageName.toString();
		if (!fQModelClassName.endsWith(".")){
			fQModelClassName += ".";
		}
		fQModelClassName += simpleModelClassName;

		String srcFileName = directory.getPath() + "/" + fQModelClassName.replace('.', '/')+".java";
    	File srcFile = new File(srcFileName);
		srcFile.getParentFile().mkdirs(); //Create all directories in the path.
		if (srcFile.exists()){
			if (!table.isExistingInDatabase() && !table.isVirtual()){
				getLog().info("Manually remove " + srcFileName + " to drop model");
			}
		}else if (table.getReflector() == null){
			OutputStreamWriter wr = null;
			try {
				wr = new OutputStreamWriter(new FileOutputStream(srcFile),encoding);
				writeFile(wr,table, packageName.toString(),pool);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage(),e);
			}finally { 
				if (wr!=null){
					try {
						wr.close();
					} catch (IOException e) {
						//
					}
				}
			}
		}
    }
    private void writeFile(OutputStreamWriter osw,Table<?> table,String packageName, String pool){
    	JdbcTypeHelper helper = Database.getJdbcTypeHelper(pool);
		Set<String> columnsPresentInFrameworkModel = new IgnoreCaseSet();
		columnsPresentInFrameworkModel.addAll(ModelReflector.instance(Model.class).getRealColumns());
		
    	Set<String> imports = new HashSet<String>();
    	List<String> code = new ArrayList<String>();
    	String extendingClass = null;
    	if (table.getModelClass() == null){
    		imports.add("com.venky.swf.db.model.Model");
    		extendingClass = "Model";
    	}else {
    		extendingClass = table.getModelClass().getName();
    		if (extendingClass.equals(packageName + "." +Table.getSimpleModelClassName(table.getTableName()))){
    			imports.add("com.venky.swf.db.model.Model");
    			extendingClass = "Model";
    		}
    		if (!table.getModelClass().getName().startsWith(packageName)){
        		columnsPresentInFrameworkModel.addAll(table.getReflector().getRealColumns());
    		}
    	}
    	if (!ObjectUtil.isVoid(pool)){
    		imports.add(DBPOOL.class.getName());
    		code.add("@DBPOOL(\"" + pool + "\")");
    	}
    	code.add("public interface " + Table.getSimpleModelClassName(table.getTableName()) + " extends " + extendingClass + "{");
		
    	for (ColumnDescriptor cd:table.getColumnDescriptors()){
    		List<TypeRef<?>> refs = helper.getTypeRefs(cd.getJDBCType());
    		TypeRef<?> ref = null; 
    		if (refs == null){
    			getLog().error("cannot find jdbc type with helper for pool" + pool + " with helper " + helper.getClass().getName()); 
    			throw new NullPointerException("cannot find jdbc type for column " + cd );
    		}
    		for (TypeRef<?>r :refs){
				ref = r;
    			if (cd.isNullable() ){
    				if (!r.getJavaClass().isPrimitive()){
    					break;
    				}
    			}else{
    				if (r.getJavaClass().isPrimitive()){
    					break;
    				}
    			}
    		}
    		String columnName = cd.getName();
			if (columnsPresentInFrameworkModel.contains(columnName)){
				continue; //Framework definition must stay.
			}
    		
			String camelfieldName = StringUtil.camelize(columnName);
			
    		if (ref == null){
    			code.add("\tpublic Unknown get"+camelfieldName+"();" );
    			code.add("\tpublic void set"+camelfieldName+"(Unknown " + StringUtil.camelize(columnName,false) + ");" );
    		}else {
    			String getterPrefix = "get";
    			if (boolean.class.isAssignableFrom(ref.getJavaClass()) || Boolean.class.isAssignableFrom(ref.getJavaClass()) ){
					getterPrefix = "is";
    			}
    			if (!ref.getJavaClass().isPrimitive() && !ref.getJavaClass().getPackage().getName().startsWith("java.lang")){
        			imports.add(ref.getJavaClass().getName());
    			}
    			code.add("\t");
    			if (!cd.isNullable() && !ref.getJavaClass().isPrimitive()){
    				imports.add(IS_NULLABLE.class.getName());
    				code.add("\t@IS_NULLABLE(false)");
    			}
    			if (cd.getSize() > 0 && ref.getSize() > 0 && ref.getSize() != cd.getSize() ){
    				imports.add(COLUMN_SIZE.class.getName());
    				code.add("\t@COLUMN_SIZE("+cd.getSize()+")");
    			}
    			if (cd.getScale() > 0 && ref.getScale() != cd.getScale()){
    				imports.add(DECIMAL_DIGITS.class.getName());
    				code.add("\t@DECIMAL_DIGITS("+cd.getScale() +")");
    			}
    			
                if (!cd.isNullable() && !ObjectUtil.isVoid(cd.getColumnDefault())){
    				imports.add(COLUMN_DEF.class.getName());
    				imports.add(StandardDefault.class.getName());
    				code.add("\t"+toAppDefaultStr(helper, ref, cd.getColumnDefault()));	
                }
                if (!StringUtil.underscorize(camelfieldName).equals(columnName.toUpperCase())){
        			imports.add(COLUMN_NAME.class.getName());
        			code.add("\t@COLUMN_NAME(\""+ cd.getName().toUpperCase() + "\")");
                }
                
          
    			code.add("\tpublic "+ ref.getJavaClass().getSimpleName() + " " + getterPrefix +  camelfieldName + "();");
    			code.add("\tpublic void set" + camelfieldName + "("+ ref.getJavaClass().getSimpleName() + " " + StringUtil.camelize(columnName,false) + ");");
    			if (camelfieldName.endsWith("Id")) {
    				String possibleReferredModelName = camelfieldName.substring(0, camelfieldName.length() - "Id".length());
    				String possibleReferredTableName = StringUtil.underscorize(StringUtil.pluralize(possibleReferredModelName));
    				Table<? extends Model> referredTable = Database.getTable(possibleReferredTableName);
    				if (referredTable != null){
    					code.add("\tpublic " + possibleReferredModelName + " get" + possibleReferredModelName + "();");
    				}
    			}
    			
    		}
    	}
    	code.add("}");
    	
    	PrintWriter w =  new PrintWriter(osw);
    	w.println("package " + packageName + ";");
    	for (String imp:imports){
    		w.println("import " + imp + ";");
		}
    	for (String line:code){
    		w.println(line);
		}
    }
    
    private String toAppDefaultStr(JdbcTypeHelper helper , TypeRef<?> ref , String dbDefault){
		if (ObjectUtil.equals(dbDefault,helper.getCurrentTimeStampKW())){
			return "@COLUMN_DEF(StandardDefault.CURRENT_TIMESTAMP)";
		}else if (ObjectUtil.equals(dbDefault, helper.getCurrentDateKW())){
			return "@COLUMN_DEF(StandardDefault.CURRENT_DATE)";
		}else if (dbDefault != null ){
			Class<?> refClass = ref.getJavaClass();
			if (refClass == boolean.class || refClass == Boolean.class){
				if (helper.getDefaultKW(ref,Boolean.valueOf(true)).equals(dbDefault)){
					return "@COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)";
				}else {
					return "@COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)";
				}
			}else if (ref.isNumeric()){
				if (helper.getDefaultKW(ref,0).equals(dbDefault)){
					return "@COLUMN_DEF(StandardDefault.ZERO)";
				}
				if (helper.getDefaultKW(ref,1).equals(dbDefault)){
					return "@COLUMN_DEF(StandardDefault.ONE)";
				}
			}
			if (ref.isColumnDefaultQuoted()) {
				StringTokenizer tok = new StringTokenizer(dbDefault, "'",false);
				return "@COLUMN_DEF(value=StandardDefault.SOME_VALUE,args=\""+tok.nextToken() +"\")";
			}else {
				return "@COLUMN_DEF(value=StandardDefault.SOME_VALUE,args=\""+dbDefault+"\")";
			}
		}else {
			return "@COLUMN_DEF(value=StandardDefault.NULL)";
		}
	}
    

}
