package com.venky.swf.plugins.collab.util;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Registry;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public class CompanyFinder {
    private static volatile CompanyFinder sSoleInstance;

    //private constructor.
    private CompanyFinder() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static CompanyFinder getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (CompanyFinder.class) {
                if (sSoleInstance == null) sSoleInstance = new CompanyFinder();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected CompanyFinder readResolve() {
        return getInstance();
    }

    public Company find(String subdomain){
        ObjectHolder<Company> companyObjectHolder = new ObjectHolder<>(null);
        Registry.instance().callExtensions("swf.company.finder",subdomain,companyObjectHolder);
        Company company= companyObjectHolder.get();
        return company;
    }
}
