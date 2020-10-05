package com.venky.swf.db.drivers;

import org.postgresql.Driver;

/* Hack to use CockroachHelper as the types are not consistent with postgresql*/
public class CockroachDBDummyDriver extends Driver {

}
