package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.plugins.collab.db.model.user.Email;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class EmailTest {
    @Test
    public void testEmail(){
        String email = "venky@wiggles.in";
        Email.validate(email);
        Assert.assertEquals("wiggles.in", getDomain(email));
    }

    @Test
    public void testDomain(){
        for (StringBuilder t: trials("wiggles.co.in")){
            System.out.println(t);
        }
    }
    public String getDomain(String email){
        String[] parts = email.split("@");
        if (parts.length != 2){
            return null;
        }
        return parts[1];

    }

    public List<StringBuilder> trials(String domainName){
        LinkedList<StringBuilder> trials = new LinkedList<>();
        StringTokenizer tokenizer = new StringTokenizer(domainName,".");
        while ( tokenizer.hasMoreTokens() ){
            String tok = tokenizer.nextToken();
            for (StringBuilder trial : trials){
                trial.append(".");
                trial.append(tok);
            }
            trials.add(new StringBuilder(tok));
        }
        while(trials.size() > 3) { //Root domain can at most have 3 parts.
            trials.removeFirst();
        }
        trials.removeLast(); //Remove registry tld


        return trials;
    }
}
