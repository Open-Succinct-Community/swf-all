package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationPublicKey;

import java.util.HashSet;
import java.util.Set;

public class ApplicationPublicKeyExtension extends ModelOperationExtension<ApplicationPublicKey> {
    static {
        registerExtension(new ApplicationPublicKeyExtension());
    }

    @Override
    protected void beforeValidate(ApplicationPublicKey instance) {
        if (instance.getRawRecord().isNewRecord()){
            if (instance.isVerified() &&
                    !instance.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().
                            valueOf(instance.getTxnProperty("being.verified"))) {
                    throw new AccessDeniedException();
            }
            return;
        }
        if (instance.isVerified() ){
            Set<String> fieldsAllowedToBeChanged = new HashSet<String>() {{
                add("VALID_FROM");
                add("VALID_UNTIL");
                add("UPDATED_AT");
                add("UPDATER_ID");
            }};
            if (instance.getRawRecord().isFieldDirty("VERIFIED")){
                if (!instance.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(instance.getTxnProperty("being.verified"))){
                    throw new AccessDeniedException();
                }
                fieldsAllowedToBeChanged.add("VERIFIED");
            }
            Set<String> fieldsChanged = new HashSet<>(instance.getRawRecord().getDirtyFields());
            fieldsChanged.removeAll(fieldsAllowedToBeChanged);
            if (!fieldsChanged.isEmpty()) {
                throw new RuntimeException("Cannot change " + fieldsChanged + " once the key is verified. Please create new one.");
            }
        }else {
            if (instance.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(
                    instance.getRawRecord().getOldValue("VERIFIED"))){
                throw new RuntimeException("Cannot change verification status once verified");
            }
        }
    }


}
