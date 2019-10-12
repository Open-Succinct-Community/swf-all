package com.venky.swf.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

public class BeforeValidateUser extends BeforeModelValidateExtension<User>{
	static {
		registerExtension(new BeforeValidateUser());
	}
	
	@Override
	public void beforeValidate(User model) {
		if (!ObjectUtil.isVoid(model.getName())) {
			String name = model.getName();
			if (ObjectUtil.isVoid(model.getLongName())) {
				int at_position = name.indexOf('@');
				if (at_position >= 0) {
					name = name.substring(0, at_position);  //Avoid Special Characters of email id
				}
				model.setLongName(name);
			}
			if (model.getRawRecord().isNewRecord()){
				List<User> userList = new Select().from(User.class).where(new Expression(ModelReflector.instance(User.class).getPool(),"lower(NAME)", Operator.EQ, model.getName().toLowerCase())).execute();
				if (!userList.isEmpty()){
					if (name.indexOf('@') > 0){
						throw new RuntimeException("Email already registered!");
					}else {
						throw new RuntimeException("Login Id already registered!");
					}
				}
			}
		}


		if (Config.instance().shouldPasswordsBeEncrypted()){
			if (model.getReflector().isVoid(model.getCreatedAt())){
				model.setCreatedAt(model.getReflector().getNow());
			}
			if (model.getRawRecord().isFieldDirty("PASSWORD") && !model.isPasswordEncrypted()){
				model.setPassword(model.getEncryptedPassword(model.getPassword()));
				model.setPasswordEncrypted(true);
			}
		}
	}

}
