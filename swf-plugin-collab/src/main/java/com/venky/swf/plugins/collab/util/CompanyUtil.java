package com.venky.swf.plugins.collab.util;

import com.venky.swf.db.Database;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.security.db.model.Role;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import org.eclipse.jetty.server.Request;


import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class CompanyUtil {

    public static Company getCompany(){
        _IPath path = Database.getInstance().getContext(_IPath.class.getName());
        String domainName = null;
        if (path == null){
            domainName = Config.instance().getProperty("swf.host","");
        }else {
            domainName = Request.getServerName(path.getRequest());
        }
        return getCompany(domainName);
    }
    public static Company getCompany(String domainName){
        List<String> domainParts = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(domainName,".");
        while (tok.hasMoreTokens()){
            domainParts.add(tok.nextToken());
        }

        List<String> possibleDomainNames = new ArrayList<>();
        while (domainParts.size() >= 2){
            possibleDomainNames.add(getFQDomainName(domainParts));
            domainParts.remove(0);
        }
        Select select =new Select().from(Company.class);
        select.where(new Expression(select.getPool(),"DOMAIN_NAME" , Operator.IN, possibleDomainNames.toArray()));
        List<Company> companies = select.execute();
        if (companies.isEmpty()){
            Company company =Database.getTable(Company.class).newRecord();
            company.setDomainName(possibleDomainNames.get(possibleDomainNames.size()-1));
            company.setName(company.getDomainName());
            company.setDateOfIncorporation(new Date(System.currentTimeMillis()));
            return company;
        }else if (companies.size() == 1) {
            return companies.get(0);
        }else {
            throw new RuntimeException("Multiple companies are registered. Not able to identify your company");
        }
    }
    private static String getFQDomainName(List<String> domainParts){
        StringBuilder FQDomainName = new StringBuilder();
        for (String part: domainParts){
            if(!FQDomainName.isEmpty()){
                FQDomainName.append(".");
            }
            FQDomainName.append(part);
        }
        return FQDomainName.toString();
    }




}
