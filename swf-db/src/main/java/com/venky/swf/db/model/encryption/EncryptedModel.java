package com.venky.swf.db.model.encryption;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

import java.util.List;

public interface EncryptedModel extends Model {
    @UNIQUE_KEY
    String getName();
    public void setName(String name);

    public List<EncryptedField> getEncryptedFields();

}
