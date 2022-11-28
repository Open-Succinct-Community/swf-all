package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;

public interface CompanyRelationship extends Model {
    
    @PARTICIPANT
    public Long getCustomerId();
    public void setCustomerId(Long id);
    public Company getCustomer();


    @PARTICIPANT
    public Long getVendorId();
    public void setVendorId(Long id);
    public Company getVendor();

}
