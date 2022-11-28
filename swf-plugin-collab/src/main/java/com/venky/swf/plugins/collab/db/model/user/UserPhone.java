package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;

@ORDER_BY("ID")
public interface UserPhone extends Phone, Model {
    @PARTICIPANT
    @UNIQUE_KEY
    public long getUserId();
    public void setUserId(long userId);
    public User getUser();

    @Index
    @UNIQUE_KEY
    @Override
    public String getPhoneNumber();
    @Override
    public void setPhoneNumber(String phoneNumber);




}
