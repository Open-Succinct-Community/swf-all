package com.venky.swf.db.model.ui;

import com.venky.swf.db.model.UserLogin;

import java.util.List;

public interface User extends com.venky.swf.db.model.User {
    public List<UserLogin> getUserLogins();
}
