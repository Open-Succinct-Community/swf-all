package com.venky.swf.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.UserLogin;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.sql.Timestamp;
import java.util.List;

public class LoginLogger implements Extension {
    static {
        Registry.instance().registerExtension(_IPath.USER_LOGIN_SUCCESS_EXTENSION,new LoginLogger());
    }
    @Override
    public void invoke(Object... objects) {
        Path path = (Path)objects[0];
        User user = ((Model)objects[1]).getRawRecord().getAsProxy(User.class);
        TaskManager.instance().executeAsync(new UserLoginLogger(path,user),false);
    }

    public static class UserLoginLogger implements Task {
        User user;
        String remoteHost;
        String userAgent;
        public UserLoginLogger(Path path, User user){
            this.user = user;
            remoteHost = path.getHeader("Real-IP") ;
            if (remoteHost == null){
                remoteHost = path.getRequest().getRemoteHost();
            }
            userAgent = path.getHeader("User-Agent");
            int size = ModelReflector.instance(UserLogin.class).getColumnDescriptor("USER_AGENT").getSize();
            if (!ObjectUtil.isVoid(userAgent) && userAgent.length() > size){
                userAgent = userAgent.substring(0,size);
            }

        }

        @Override
        public void execute() {
            if (user == null){
                return;
            }
            String guestUserName = Config.instance().getProperty("swf.guest.user");
            if (!ObjectUtil.isVoid(guestUserName) && ObjectUtil.equals(guestUserName,user.getName())){
                return;
            }

            Select select = new Select(true).from(UserLogin.class);
            Expression where = new Expression(select.getPool(), Conjunction.AND);
            where.add(new Expression(select.getPool(),"FROM_IP", Operator.EQ, remoteHost));
            where.add(new Expression(select.getPool(),"USER_ID", Operator.EQ, user.getId()));
            List<UserLogin> logins = select.where(where).orderBy("LOGIN_TIME DESC").execute();
            UserLogin login = null;
            for (UserLogin userLogin : logins) {
                if (login == null) {
                    login = userLogin;
                } else {
                    userLogin.destroy();
                }
            }

            if (login == null){
                login = Database.getTable(UserLogin.class).newRecord();
            }

            login.setUserId(user.getId());
            login.setFromIp(remoteHost);
            login.setUserAgent(userAgent);
            login.setLoginTime(new Timestamp(System.currentTimeMillis()));
            if (login.getRawRecord().isNewRecord()){
                login = Database.getTable(UserLogin.class).getRefreshed(login);
            }
            login.setLat(user.getCurrentLat());
            login.setLng(user.getCurrentLng());
            login.save();
        }
    }
}
