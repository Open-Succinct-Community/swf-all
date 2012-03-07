package com.venky.swf.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.DECIMAL_DIGITS;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
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
        Database db =Database.getInstance(false);
        db.loadTables(true);
        for (Table<?> table: db.getTables().values()){
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
    	JdbcTypeHelper helper = Database.getInstance().getJdbcTypeHelper();
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
    		if (table.getModelClass().getName().startsWith("com.venky.swf.db.model")){
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
    			if (!cd.isNullable()){
    				imports.add(IS_NULLABLE.class.getName());
    				code.add("\t@IS_NULLABLE(false)");
    			}
    			if (cd.getSize() > 0){
    				imports.add(COLUMN_SIZE.class.getName());
    				code.add("\t@COLUMN_SIZE("+cd.getSize()+")");
    			}
    			if (cd.getScale() > 0){
    				imports.add(DECIMAL_DIGITS.class.getName());
    				code.add("\t@DECIMAL_DIGITS("+cd.getScale() +")");
    			}
    			imports.add(COLUMN_NAME.class.getName());
    			code.add("\t@COLUMN_NAME(\""+ cd.getName() + "\")");
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
}
