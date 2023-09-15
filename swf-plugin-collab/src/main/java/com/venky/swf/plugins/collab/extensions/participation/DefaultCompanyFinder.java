package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import org.apache.commons.net.whois.WhoisClient;

import java.util.LinkedList;
import java.util.StringTokenizer;

public class DefaultCompanyFinder implements Extension {
    static {
        Registry.instance().registerExtension("swf.company.finder",new DefaultCompanyFinder());
    }

    @Override
    public void invoke(Object... context) {
        String subdomainId = (String)context[0];
        ObjectHolder holder = (ObjectHolder)context[1];

        String domainName = getFQDomainName(subdomainId);

        Company company = Database.getTable(Company.class).newRecord();
        company.setDomainName(domainName);
        company = Database.getTable(Company.class).getRefreshed(company);
        if (company.getRawRecord().isNewRecord()){
            company.setName(company.getDomainName());
        }

        holder.set(company);
    }



    public static String getFQDomainName(String domainName) {
        WhoisClient whoisClient = new WhoisClient();

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

        if (trials.size() == 1){
            return trials.getFirst().toString();
        }
        for (StringBuilder trial : trials){
            try {
                whoisClient.connect("whois.godaddy.com");
                String result = whoisClient.query(trial.toString());
                if (!result.startsWith("No match for ")) {
                    return trial.toString() ;
                }
            }catch(Exception ignored){

            } finally {
                try {
                    whoisClient.disconnect();
                }catch (Exception ignored){

                }
            }

        }

        return null;
    }
}
