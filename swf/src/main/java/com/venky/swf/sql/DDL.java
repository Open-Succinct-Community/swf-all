package com.venky.swf.sql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.routing.Config;

public abstract class DDL extends DataManupulationStatement{

	protected List<BindVariable> values = new ArrayList<BindVariable>();
	protected String table ;

	protected DDL(String table){
		this.table = table;
	}

	protected void finalizeParameterizedSQL(){
		
	}
	@Override
	public List<BindVariable> getValues() {
		return values;
	}

	public static final class DropTable extends DDL {
		public DropTable(String table){
			super(table);
			getQuery().append("drop table ").append(table);
		}
	}
	
	public static final class CreateTable extends DDL {
		List<String> columnsSpec = new ArrayList<String>();
		List<String> pk = new ArrayList<String>();
		public CreateTable(String table){
			super(table);
		}

		String asSelect = null;
		public CreateTable as(String selectClause){
			if (columnsSpec.isEmpty() && pk.isEmpty()){
				asSelect = selectClause;
			}else {
				throw new RuntimeException("Syntax Error for Create Table");
			}
			return this;
		}
		protected void finalizeParameterizedSQL(){
			StringBuilder query = getQuery();
			query.append("create table ").append(table);
			if (asSelect != null){
				query.append(" AS ");
				query.append(asSelect);
			}else {
				query.append("( ");
				
				Iterator<String> colSpecIterator = columnsSpec.iterator();
				while(colSpecIterator.hasNext()){
					String colSpec = colSpecIterator.next();
					query.append(colSpec);
					if (colSpecIterator.hasNext()){
						query.append(" , ");
					}
				}
				
				if (pk.size() > 0 && Database.getJdbcTypeHelper().requiresSeparatePrimaryKeyClause()){
					query.append(" , ").append(" primary key(");
					Iterator<String> pkColumnIterator = pk.iterator();
					while (pkColumnIterator.hasNext()){
						String col = pkColumnIterator.next();
						query.append(col);
						if (pkColumnIterator.hasNext()){
							query.append(",");
						}
					}
					query.append(")");
				}
				
				query.append(" )");
			}
			Config.instance().getLogger(getClass().getName()).fine(query.toString());
		}
		public void addColumn(String columnSpec){
			assert asSelect == null;
			columnsSpec.add(columnSpec);
		}
		
		public void addPrimaryKeyColumn(String pkColumn){
			assert asSelect == null;
			pk.add(pkColumn);
		}
	}
	
	public static final class AlterTable extends DDL {
		public AlterTable(String table){
			super(table);
		}
		private String alterSpec = null ;
		public void dropColumn(String columnName){
			if (alterSpec == null){
				alterSpec = " drop column " + columnName;
			}
		}
		
		public void addColumn(String columnSpec){
			if (alterSpec == null){
				alterSpec = " add column " + columnSpec;
			}
		}
		protected void finalizeParameterizedSQL(){
			StringBuilder query = getQuery();
			query.append("alter table ").append(table);
			query.append(alterSpec);
		}
	}
}
