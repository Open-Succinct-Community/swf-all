package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.swf.plugins.collab.db.model.participants.Application;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApplicationParticipantExtension extends CompanyNonSpecificParticipantExtension<Application> {
    static {
        registerExtension(new ApplicationParticipantExtension());
    }

    @Override
    protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, Application partiallyFilledModel, String fieldName) {
        User u = (User) user;
        if (partiallyFilledModel != null) {
            if ("CREATOR_USER_ID".equalsIgnoreCase(fieldName)) {
                if (u.isStaff()) {
                    return null;
                } else {
                    return Arrays.asList(u.getId());
                }
            }
        }
        return new ArrayList<>();
    }
}
