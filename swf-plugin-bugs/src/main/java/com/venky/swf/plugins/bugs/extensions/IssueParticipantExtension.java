package com.venky.swf.plugins.bugs.extensions;

import com.venky.swf.plugins.collab.db.model.user.User ;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.plugins.collab.extensions.participation.CompanySpecificParticipantExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IssueParticipantExtension extends CompanySpecificParticipantExtension<Issue> {
    static {
        registerExtension(new IssueParticipantExtension());
    }
    @Override
    public List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, Issue partiallyFilledModel, String fieldName) {
        User u = user.getRawRecord().getAsProxy(User.class);

        if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
            return super.getAllowedFieldValues(user,partiallyFilledModel,fieldName);
        }else if ("ASSIGNED_TO_ID".equalsIgnoreCase(fieldName)){
            List<Long> ids = new ArrayList<>();
            if (u.isStaff()){
                ids.addAll(u.getCompany().getStaffUserIds());
                ids.add(null);
            }
            return ids;
        }

        return null;
    }
}
