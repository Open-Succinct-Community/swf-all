package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public interface CompanyNonSpecific {
    @PARTICIPANT("COMPANY")//To ensure it is anded and not ored  in queries.
    @IS_NULLABLE(true)
    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public Long getCompanyId();
    public void setCompanyId(Long id);
    public Company getCompany();
}
