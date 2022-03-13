package com.venky.swf.db.model.encryption;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.model.Model;

public interface EncryptedField extends Model {
    @IS_NULLABLE(value = false)
    public Long getEncryptedModelId();
    public void setEncryptedModelId(Long id);
    public EncryptedModel getEncryptedModel();

    public String getFieldName();
    public void setFieldName(String encryptedFieldName);

}
