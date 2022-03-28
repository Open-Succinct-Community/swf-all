package com.venky.swf.plugins.bugs.extensions;

import com.venky.core.io.StringReader;
import com.venky.core.util.ExceptionUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.AsyncTaskWorker;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoIssueCreator implements Extension {
    static {
        //Causes Issues Registry.instance().registerExtension("after.rollback",new AutoIssueCreator());
    }
    @Override
    public void invoke(Object... context) {
        Transaction transaction = context.length <=  0? null : (Transaction)context[0];
        com.venky.swf.db.model.User u = Database.getInstance().getCurrentUser();
        User sessionUser = u == null ? null : u.getRawRecord().getAsProxy(User.class);
        if (sessionUser == null || sessionUser.getCompanyId() == null){
            return;
        }
        Throwable throwable = context.length <= 1 ? null : ExceptionUtil.getRootCause((Throwable)context[1]);
        if (throwable != null){
            AsyncTaskManagerFactory.getInstance().addAll(Arrays.asList(new Task() {
                @Override
                public void execute() {
                    String title =  "Exception found " + throwable.getMessage() + " " + getLocation(throwable);
                    List<Issue> issues = new Select().from(Issue.class).where(new Expression(ModelReflector.instance(Issue.class).getPool(),"TITLE", Operator.LK,title+"%")).execute(1);
                    if (issues.isEmpty()){
                        Issue issue = Database.getTable(Issue.class).newRecord();
                        StringWriter w = new StringWriter(); throwable.printStackTrace(new PrintWriter(w));
                        issue.setDescription(new StringReader(w.toString()));
                        issue.setTitle(title.substring(0,Math.min(issue.getReflector().getColumnDescriptor("TITLE").getSize(),title.length())));
                        issue.setCompanyId(sessionUser.getCompanyId());
                        issue.save();
                    }
                }

                private String getLocation(Throwable throwable) {
                    Set<String> interestingPackages = new HashSet<>();
                    Config.instance().getModelPackageRoots().forEach(r->interestingPackages.add(r.replace(".db.model","")));

                    StackTraceElement[] st = throwable.getStackTrace();
                    for (int i = 0 ; i < st.length ; i ++){
                        for (String interestingPackage: interestingPackages){
                            if (st[i].getClassName().startsWith(interestingPackage)){
                                return st[i].toString();
                            }
                        }
                    }
                    return  "Unknown:" + System.currentTimeMillis();
                }
            }));
        }

    }
}
