package com.venky.swf.db.model.ui;

import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.model.UserLogin;

import java.util.List;

public interface User extends com.venky.swf.db.model.User {
    @CONNECTED_VIA("USER_ID")
    public List<UserLogin> getUserLogins();
}
