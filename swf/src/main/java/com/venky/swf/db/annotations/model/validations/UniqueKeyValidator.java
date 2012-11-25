package com.venky.swf.db.annotations.model.validations;

import com.venky.swf.db.model.Counts;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;


public class UniqueKeyValidator extends ModelValidator{

	@Override
	protected <M extends Model> boolean isValid(ModelReflector<M> reflector, M m,MultiException modelValidationException) {
		for (Expression where : reflector.getUniqueKeyConditions(m)){
			Expression countWhere = new Expression(Conjunction.AND);
			countWhere.add(where);
			if (m.getId() > 0){
				countWhere.add(new Expression("ID",Operator.NE,m.getId()));
			}
			Counts count = new Select("COUNT(1) AS COUNT").from(reflector.getModelClass()).where(countWhere).execute(Counts.class).get(0);
			if (count.getCount() > 0){
				modelValidationException.add(new UniqueConstraintViolatedException(where.getRealSQL()));
				return false;
			}
		}
		return true;
	}
	
	public class UniqueConstraintViolatedException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6457033240437360324L;
		
		public UniqueConstraintViolatedException(){
			super();
		}
		public UniqueConstraintViolatedException(String message){
			super(message);
		}
	}

}
