#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ${package}.db.model;

import java.sql.Date;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.model.Model;

public interface Sample extends Model {

	/*
		Every model interface must extends Model. 
		That would also automatically add an id column to your table/model

		add getter, setter methods for each field you need. Note. Both getter and setter are required. 

		e.g.
		public int getSomeThing();
		public void setSomeThing(int someThing); 

		Based on the return value of the getter, an appropriate db type would be automatically chosen. 
	
		The following return types are currently supported. 
		boolean,byte,short,int,long,float,double,BigDecimal, java.sql.Date, java.sql.Timestamp, java.sql.Time and String 


		Annotations Supported on getter methods:
		======================================
		FIELD Descriptors:
		1. @COLUMN_DEF(StandardDefault.*)  to set column defaults, Allowed values for such defaults are enumerated in enum StandardDefault
		2. @COLUMN_NAME,@COLUMN_SIZE,@COLUMN_TYPE,DECIMAL_DIGITS to override default values that would be used by framework. Mostly these should not be required. 
		3. @IS_AUTOINCREMENT to indicate if the column is auto generated. Typically used for id column. 
		4. @IS_NULLABLE to indicate if the column is nullable.
		5. @IS_VIRTUAL to indicate if the column must not be persisted in DB and is only a computed column. 
				For such computed columns, you would need to put in an Impl Class 
				e.g SampleImpl that extends ModelImpl class and have the logic put in there. 
				You may also need to declare any local variables to store such values if so desired. 

		6. @PASSWORD to indicate if the column is a password field and must be shown in ui as *****.
		
		Field Validations:
		1. @Enumeration("comma separated values") 
		2. @ExactLength(integer) 
		3. @MaxLength(integer)
		4. @Mandatory
		5. @RegEx("regex pattern") e.g @RegEx("[A-z|0-9]+@[A-z|0-9]+[.][A-z]+") may be used to validate  an email id simplistically.


		Keeping references to other models.
		==================================
		E.g Say Customer reference is to be kept in an order model. you would have typically done:

		public long getXXXCustomerId();
		public void setXXXCustomerId(long customerId);

		
		In addition, you could define 
		public Customer getXXXCustomer(); 

		IF you define this, in the order edit screen, framework could automatically provide a lookup from customer table. This lookup would automatically come if the column that hold customer name is "NAME" if however, you need to show a different column name in the lookup, a class level annotation HAS_DESCRIPTION_COLUMN("COLUMN NAME") may be used to tell the framework what column should be shown in the auto complete for customer in other screens. 

	*/


	public int getSomeInt();
	public void setSomeInt(int someInt);


	@COLUMN_DEF(StandardDefault.CURRENT_DATE)
	public Date getSomeDate();
	public void setSomeDate(Date someDate);


	@COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
	public boolean getSomeBoolean();
	public void setSomeBoolean (boolean someBoolean);
}
