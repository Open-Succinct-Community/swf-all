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

import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.DECIMAL_DIGITS;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.routing.Config;

/**
 * Goal which generates models from database a.k.a reverse engineering.
 * 
 * @goal generate-model
 * 
 * @phase generate-sources
 */
public class ModelGeneratorMojo extends AbstractMojo {
	
	/**
	 * Source directory. 
	 * @parameter expression="${generate-model.srcdir}" default-value="src/main/java"
	 */
	File srcDir;
	
	/**
	 * @parameter expression="${project.build.sourceEncoding}"
	 */
	String encoding;
	
	
	public void execute() throws MojoExecutionException {
		getLog().info("Generating models...");
		generateModels(srcDir);
		getLog().info("Done");
	}
	
    public void generateModels(File directory) throws MojoExecutionException{
        Database.loadTables(true);
        for (Table<?> table: Database.getTables().values()){
            generateModelClass(table, directory);
        }
    }
    
    private void generateModelClass(Table<?> table,File directory) throws MojoExecutionException{
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
		if (!table.isExistingInDatabase()) {
			if (srcFile.exists()) {
				getLog().info("Manually remove " + srcFileName + " to drop model");
			}
		}else if (!srcFile.exists()){
			OutputStreamWriter wr = null;
			try {
				wr = new OutputStreamWriter(new FileOutputStream(srcFile),encoding);
				writeFile(wr,table, packageName.toString());
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
		}else {
			getLog().info("Manually remove " + srcFileName + " to regenerate");
		}
    }
    private void writeFile(OutputStreamWriter osw,Table<?> table,String packageName){
    	JdbcTypeHelper helper = Database.getJdbcTypeHelper();
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
    	code.add("public interface " + Table.getSimpleModelClassName(table.getTableName()) + " extends " + extendingClass + "{");
		
		
    	for (ColumnDescriptor cd:table.getColumnDescriptors()){
    		List<TypeRef<?>> refs = helper.getTypeRefs(cd.getJDBCType());
    		TypeRef<?> ref = null; 
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
			if (columnsPresentInFrameworkModel.contains(cd.getName())){
				continue; //Framework definition must stay.
			}
    		
			String camelfieldName = StringUtil.camelize(cd.getName());
			
    		if (ref == null){
    			code.add("\tpublic Unknown get"+camelfieldName+"();" );
    			code.add("\tpublic void set"+camelfieldName+"(Unknown " + StringUtil.camelize(cd.getName(),false) + ");" );
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
                	if (!cd.getColumnDefault().equals(helper.getDefaultKW(ref,ref.getTypeConverter().valueOf(null)))){
        				imports.add(COLUMN_DEF.class.getName());
        				imports.add(StandardDefault.class.getName());
        				code.add("\t"+toAppDefaultStr(helper, ref, cd.getColumnDefault()));	
                	}
                }
                /*
    			imports.add(COLUMN_NAME.class.getName());
    			code.add("\t@COLUMN_NAME(\""+ cd.getName() + "\")");
    			Column name not required as name derived from getter we are putting is going to be the same any way.
    			*/
    			code.add("\tpublic "+ ref.getJavaClass().getSimpleName() + " " + getterPrefix +  camelfieldName + "();");
    			code.add("\tpublic void set" + camelfieldName + "("+ ref.getJavaClass().getSimpleName() + " " + StringUtil.camelize(cd.getName(),false) + ");");
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
			Class refClass = ref.getJavaClass();
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
				return "@COLUMN_DEF(value=StandardDefault.SOME_VALUE,someValue=\""+tok.nextToken() +"\")";
			}else {
				return "@COLUMN_DEF(value=StandardDefault.SOME_VALUE,someValue=\""+dbDefault+"\")";
			}
		}else {
			return "@COLUMN_DEF(value=StandardDefault.NULL)";
		}
	}
    

}
