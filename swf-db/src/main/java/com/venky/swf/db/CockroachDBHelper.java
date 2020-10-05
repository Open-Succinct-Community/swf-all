/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import com.venky.core.date.DateUtils;
import com.venky.core.util.Bucket;
import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.model.Count;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.sql.Select;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.TimeZone;

/**
 *
 * @author venky
 */
public class CockroachDBHelper extends PostgresqlHelper{
    protected CockroachDBHelper() {
		registerjdbcSQLType(Integer.class, new TypeRef<>(java.sql.Types.BIGINT,
						"BIGINT", 0, 0, false,false,new IntegerConverter()));
		registerjdbcSQLType(int.class, new TypeRef<>(java.sql.Types.BIGINT,
						"BIGINT", 0, 0, false,false,new IntegerConverter()));
    }
	@Override
	public String getAutoIncrementInstruction() {
		return (" BIGINT DEFAULT unique_rowid() NOT NULL ");
	}

}
