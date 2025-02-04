/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.venky.cache.Cache;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;

/**
 *
 * @author venky
 */
@HAS_DESCRIPTION_FIELD("LONG_NAME")
@MENU("Admin")
public interface User extends Model {
	
	@IS_NULLABLE(false)
	@UNIQUE_KEY("K1,K2,K3") //All keys needed for uniqueness within company etc context and independently  also.
	@Index
    public String getName();
    public void setName(String name);
    
    @Index
    @UNIQUE_KEY("K2")
    public String getLongName();
    public void setLongName(String name);

    @IS_NULLABLE
    @UNIQUE_KEY("API")
    @HIDDEN
    @PROTECTION
    @COLUMN_DEF(StandardDefault.NULL)
    @EXPORTABLE(false)
    public String getApiKey();
    public void setApiKey(String key);

    @IS_NULLABLE
    @HIDDEN
    @EXPORTABLE(false)
    @COLUMN_DEF(StandardDefault.NULL)
    public Timestamp getApiKeyGeneratedTs();
    public void setApiKeyGeneratedTs(Timestamp ts);

    @IS_VIRTUAL
    @COLUMN_SIZE(60)
    @PASSWORD
    public String getChangePassword();
    @IS_VIRTUAL
    public void setChangePassword(String password);
    
    
    @PASSWORD
    @HIDDEN
    @PROTECTION
    public String getPassword();
    public void setPassword(String password);

    @HIDDEN
    @PROTECTION
    @IS_VIRTUAL
    public String getPassword2();
    public void setPassword2(String password2); // Used for signup. !!

    public boolean authenticate(String password);
    public static final String USER_AUTHENTICATE = "user.authenticate" ;
   
    public <M extends Model> Cache<String,Map<String,List<Long>>> getParticipationOptions(Class<M> modelClass);
    public Cache<String,Map<String,List<Long>>> getParticipationOptions(Class<? extends Model> modelClass,Model model);
    public static final String GET_PARTICIPATION_OPTION = "get.participation.option";//++ModelClass.SimpleName
    
    public <M extends Model> Expression getDataSecurityWhereClause(Class<M> modelClass);
    public Expression getDataSecurityWhereClause(Class<? extends Model> modelClass,Model model);
    public Expression getDataSecurityWhereClause(ModelReflector<? extends Model> ref, Cache<String,Map<String,List<Long>>> participatingRoleGroupOptions);
    
    @IS_VIRTUAL
    public boolean isAdmin();
    

	@CONNECTED_VIA("USER_ID")
	public List<UserEmail> getUserEmails();
	
	public void generateApiKey();
    public void generateApiKey(boolean save);

    @IS_VIRTUAL
    public String getEncryptedPassword(String unencryptedPassword);


    @IS_VIRTUAL
    public int getNumMinutesToKeyExpiration();


    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION(Kind.DISABLED)
    public boolean isPasswordEncrypted();
    public void setPasswordEncrypted(boolean encrypted);


    @IS_VIRTUAL
    public String getFirstName();


    @IS_VIRTUAL
    public String  getLastName();


    @IS_VIRTUAL
    public BigDecimal getCurrentLat();
    public void setCurrentLat(BigDecimal lat);

    @IS_VIRTUAL
    public BigDecimal getCurrentLng();
    public void setCurrentLng(BigDecimal lng);



    @IS_NULLABLE(false)
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION
    public boolean isAccountClosureInitiated();
    public void setAccountClosureInitiated(boolean accountClosureInitiated);

    @IS_NULLABLE(false)
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION
    public boolean isAccountClosed();
    public void setAccountClosed(boolean accountClosed);


    @IS_VIRTUAL
    public boolean isPasswordSet();

}
