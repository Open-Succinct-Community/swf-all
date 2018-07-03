package com.venky.swf.plugins.mail.controller;

import com.venky.core.string.StringUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.mail.core.MailerTask;
import com.venky.swf.plugins.mail.db.model.Mail;
import com.venky.swf.views.View;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;


public class MailsController extends ModelController<Mail> {
    public MailsController(Path path) {
        super(path);
    }

    public View send(){

        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot sendMail in any other method other than POST");
        }
        List<Mail> mails = getIntegrationAdaptor().readRequest(this.getPath());
        List<MailerTask> tasks = new LinkedList<>();
        for (Mail mail : mails){
            MailerTask task = new MailerTask(mail.getUser(),mail.getSubject(),StringUtil.read(mail.getBody()));
            tasks.add(task);
        }
        TaskManager.instance().executeAsync(tasks);


        if (getIntegrationAdaptor() != null){
            return getIntegrationAdaptor().createStatusResponse(getPath(),null);
        }else {
            getPath().addInfoMessage("Job Submitted");
            return back();
        }

    }

}
