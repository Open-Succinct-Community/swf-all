package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public interface UserEmail extends com.venky.swf.db.model.UserEmail , Email {
    @PARTICIPANT
    @IS_VIRTUAL
    public Long getCompanyId();
    public void setCompanyId(Long companyId);

    @IS_VIRTUAL
    public Company getCompany();

}
