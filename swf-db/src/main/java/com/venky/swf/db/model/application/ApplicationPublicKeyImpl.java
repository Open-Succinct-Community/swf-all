package com.venky.swf.db.model.application;

import com.venky.swf.db.table.ModelImpl;

public class ApplicationPublicKeyImpl extends ModelImpl<ApplicationPublicKey> {
    public ApplicationPublicKeyImpl() {
    }

    public ApplicationPublicKeyImpl(ApplicationPublicKey proxy) {
        super(proxy);
    }
    public boolean isExpired() {
        ApplicationPublicKey key = getProxy();
        if (key.getRawRecord().isNewRecord()){
            return true;
        }
        long now = System.currentTimeMillis();
        boolean valid = (key.getValidFrom() == null || key.getValidFrom().getTime() < now )
                && (key.getValidUntil() == null  || key.getValidUntil().getTime() > now);
        return !valid;
    }
}
