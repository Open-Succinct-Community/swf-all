package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

@HAS_DESCRIPTION_FIELD
@CONFIGURATION
@MENU("Admin")
public interface Role extends Model{
	@IS_NULLABLE(false)
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);


	static Role getRole(String name) {
		List<Role> roles = new Select().from(Role.class).where(new Expression(ModelReflector.instance(Role.class).getPool(),"NAME", Operator.EQ, name)).execute(1);
		if (roles.isEmpty()){
			return null;
		}else {
			return roles.get(0);
		}
	}
}
