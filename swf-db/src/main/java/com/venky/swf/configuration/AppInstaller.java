package com.venky.swf.configuration;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.encryption.EncryptedField;
import com.venky.swf.db.model.encryption.EncryptedModel;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Update;
import com.venky.swf.util.SharedKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AppInstaller implements Installer {
	public void install(){
		installUsers();
		fixUserName();
		fixUserPasswords();
		migrateEncryptedModels();
	}
	@SuppressWarnings("unchecked")
	protected  void migrateEncryptedModels() {
		List<String> modelClasses = Config.instance().getModelClasses();
		Map<String,EncryptedModel> map = new Cache<>(0,0) {
			@Override
			protected EncryptedModel getValue(String s) {
				return null;
			}
		};
		List<EncryptedModel> statuses = new Select().from(EncryptedModel.class).execute();
		statuses.forEach(s->{
			String correctName = s.getName().replaceAll("^.*\\.","");
			EncryptedModel encryptedModel = map.get(correctName);
			if (encryptedModel != null){
				s.destroy();
			}else {
				if (!ObjectUtil.equals(correctName,s.getName())) {
					s.setName(correctName);
					s.save();
					map.put(correctName,s);
				}
			}
		});


		for (String modelClassName : modelClasses){
			try {
				Class<? extends Model> modelClass = (Class<? extends Model>) Class.forName(modelClassName);
				if (!modelClassName.equals(Model.class.getName()) && modelClass.isInterface() && Model.class.isAssignableFrom(modelClass)) {
					ModelReflector<? extends Model> ref = ModelReflector.instance((Class<? extends Model>) modelClass);
					encrypt(ref, map.get(ref.getModelClass().getSimpleName()));
				}
			}catch (Exception ex){
				//
			}
		}
	}
	public <M extends Model > void encrypt(ModelReflector<M> ref,EncryptedModel existing) {
		EncryptedModel status = existing;
		boolean isEncrypted = !ref.getEncryptedFields().isEmpty();
		boolean encryptionSupported = (Config.instance().getBooleanProperty("swf.encryption.support",true));
		boolean requiresEncryptionFinally = encryptionSupported &&  isEncrypted;

		List<String> fieldsFinallyToBeEncrypted = requiresEncryptionFinally? ref.getEncryptedFields() : new IgnoreCaseList(false);

		List<String> fieldsAlreadyEncrypted = new IgnoreCaseList(false);
		if (status != null){
			fieldsAlreadyEncrypted = status.getEncryptedFields().stream().map(EncryptedField::getFieldName).collect(Collectors.toList());
			if (fieldsAlreadyEncrypted.isEmpty()){
				fieldsAlreadyEncrypted = ref.getEncryptedFields(); //Backwards compatibility.!
				for (String f : fieldsAlreadyEncrypted){
					EncryptedField field = Database.getTable(EncryptedField.class).newRecord();
					field.setFieldName(f);
					field.setEncryptedModelId(status.getId());
					field.save();
				}
			}
		}

		List<String> fieldsToBeEncryptedNow = new IgnoreCaseList(false);
		fieldsToBeEncryptedNow.addAll(fieldsFinallyToBeEncrypted);
		fieldsToBeEncryptedNow.removeAll(fieldsAlreadyEncrypted);

		List<String> fieldsToBeDecryptedNow = new IgnoreCaseList(false);
		fieldsToBeDecryptedNow.addAll(fieldsAlreadyEncrypted);
		fieldsToBeDecryptedNow.removeAll(fieldsFinallyToBeEncrypted);

		if (fieldsToBeEncryptedNow.isEmpty() && fieldsToBeDecryptedNow.isEmpty()){
			return;
		}else if (fieldsFinallyToBeEncrypted.isEmpty()) {
			if (status != null){
				status.destroy();
				status = null;
			}
		}else {
			if (status == null) {
				EncryptedModel encryptedModel = Database.getTable(EncryptedModel.class).newRecord();
				encryptedModel.setName(ref.getModelClass().getSimpleName());
				encryptedModel.save();
				status = encryptedModel;
			}
		}
		List<String> encryptedFields = ref.getEncryptedFields();
		try {


			SharedKeys.getInstance().setEnableEncryption(!fieldsToBeDecryptedNow.isEmpty());
			ref.setEncryptedFields(fieldsToBeDecryptedNow);
			List<M> models = new Select().from(ref.getModelClass()).execute(ref.getModelClass());
			SharedKeys.getInstance().setEnableEncryption(!fieldsToBeEncryptedNow.isEmpty());
			ref.setEncryptedFields(fieldsToBeEncryptedNow);

			models.forEach(a -> {
				for (String f : fieldsToBeEncryptedNow) {
					a.getRawRecord().markDirty(ref.getColumnDescriptor(f).getName());
				}
				for (String f : fieldsToBeDecryptedNow) {
					a.getRawRecord().markDirty(ref.getColumnDescriptor(f).getName());
				}
				//Make  all Fields as dirty to do any compute/avoid compute as may be decided by domain model.
				a.save(false);
			});
			if (status != null){
				if (!fieldsToBeDecryptedNow.isEmpty()){
					for (EncryptedField encryptedField : status.getEncryptedFields()) {
						if (fieldsToBeDecryptedNow.contains(encryptedField.getFieldName())){
							encryptedField.destroy();
						}
					}
				}
				if (!fieldsToBeEncryptedNow.isEmpty()){
					for (String f : fieldsToBeEncryptedNow){
						EncryptedField field  = Database.getTable(EncryptedField.class).newRecord();
						field.setEncryptedModelId(status.getId());
						field.setFieldName(f);
						field.save();
					}
				}
			}

		}finally {
			ref.setEncryptedFields(encryptedFields);
			SharedKeys.getInstance().setEnableEncryption(encryptionSupported);
		}
	}

	protected void fixUserPasswords(){
		if (Config.instance().shouldPasswordsBeEncrypted()){
			Expression where = new Expression(ModelReflector.instance(User.class).getPool(), Conjunction.AND);
			where.add(new Expression(ModelReflector.instance(User.class).getPool(),
					"PASSWORD_ENCRYPTED", Operator.EQ,false));
			where.add(new Expression(ModelReflector.instance(User.class).getPool(),
					"PASSWORD", Operator.NE));

			List<User> users = new Select().from(User.class).
					where(where).execute();
			for (User user : users) {
				user.setChangePassword(user.getPassword());
				user.save();
			}
		}
	}
	protected void fixUserName(){
		Select q = new Select().from(User.class);
		ModelReflector<User> ref = ModelReflector.instance(User.class);
		String nameColumn = ref.getColumnDescriptor("long_name").getName();

		List<User> users = q.where(new Expression(ref.getPool(),nameColumn,Operator.EQ)).execute();
		for (User user: users){
			user.setLongName(user.getName());
		}
	}
	
	protected void installUsers(){
		Table<User> USER = Database.getTable(User.class);
		
		Select q = new Select().from(User.class);
		ModelReflector<User> ref = ModelReflector.instance(User.class);
		String nameColumn = ref.getColumnDescriptor("name").getName();
		
		//This Encryption is the symmetic encryption using sharedkeys
		List<User> users = q.where(new Expression(ref.getPool(),nameColumn,Operator.EQ,new BindVariable(ref.getPool(),"root"))).execute(User.class,false);
		
		if (users.isEmpty()){
			User u = USER.newRecord();
			u.setName("root");
			u.setLongName("Application Adminstrator");
			u.setPassword("root");
			u.setPasswordEncrypted(false); // This is hashed password.
			u.save();
			if (u.getId() != 1){
				new Update(ref).set("ID", new BindVariable(ref.getPool(),1L)).where(new Expression(ref.getPool(),"ID",Operator.EQ,u.getId())).executeUpdate();
				u.setId(1L); //Coorect memory object.
			}
		}
	}
}
