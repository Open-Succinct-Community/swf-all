package com.venky.swf.plugins.bugs.extensions;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.plugins.collab.extensions.participation.CompanySpecificParticipantExtension;
import com.venky.swf.pm.DataSecurityFilter;

import java.util.Arrays;
import java.util.List;

public class IssueParticipantExtension extends CompanySpecificParticipantExtension<Issue> {
    @Override
    protected List<Long> getAllowedFieldValues(User user, Issue partiallyFilledModel, String fieldName) {
        if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
            return super.getAllowedFieldValues(user,partiallyFilledModel,fieldName);
        }else if ("ASSIGNED_TO_ID".equalsIgnoreCase(fieldName)){
            return Arrays.asList(user.getId());
        }

        return null;
    }
}
