package com.venky.swf.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.User;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.routing.Config;

public class UserAuthenticateExtension implements Extension {
	static {
		Registry.instance().registerExtension(User.USER_AUTHENTICATE,new UserAuthenticateExtension());
	}
	@Override
	public void invoke(Object... context) {
		User user = (User) context[0];
		String password = (String) context[1];
		if (user.isPasswordEncrypted()){
			password = user.getEncryptedPassword(password);
		}

		if (!ObjectUtil.equals(user.getPassword(),password)){
			throw new AccessDeniedException();
		}else if (user.getNumMinutesToKeyExpiration() < 0 ) {
			user.generateApiKey();
		}
	}
}
