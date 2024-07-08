package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import org.apache.lucene.search.Query;

import java.util.List;


public class UserParticipantExtension extends CompanyNonSpecificParticipantExtension<User>{
	static {
		registerExtension(new UserParticipantExtension());
	}

	@Override
	public List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, User partiallyFilledModel,
			String fieldName) {
		
		SequenceSet<Long> ret = null;
		User u = user.getRawRecord().getAsProxy(User.class);
		if ("SELF_USER_ID".equalsIgnoreCase(fieldName)) {
			if (!u.isStaff() || partiallyFilledModel.getId() == user.getId()){
				ret = new SequenceSet<>();
				ret.add(user.getId());
			}else if (partiallyFilledModel.getId() > 0){
				ret = new SequenceSet<>();
				List<Long> accessibleCompanyIds = u.getCompanyIds();
				accessibleCompanyIds.retainAll(partiallyFilledModel.getCompanyIds());
				if (!accessibleCompanyIds.isEmpty()){
					ret.add(partiallyFilledModel.getId());
				}
			}else {
				StringBuilder q = new StringBuilder();

				for (Long companyId : u.getCompanyIds()) {
					if (companyId == null){
						continue;
					}
					if (!q.isEmpty()){
						q.append(" OR ");
					}
					q.append("(  EMAIL:\"@").append(Database.getTable(Company.class).get(companyId).getDomainName()).append("\" )");
				}
				q.append(")");
				q.insert(0,"(");
				LuceneIndexer indexer = LuceneIndexer.instance(UserEmail.class);
				Query qry = indexer.constructQuery(q.toString());
				List<Long> userEmailIds = indexer.findIds(qry,0);
				if (userEmailIds.size() < 50){
					ret = new SequenceSet<>();
					Select s = new Select().from(UserEmail.class) ;
					List<UserEmail> userEmails = s.where(new Expression(s.getPool(),"ID", Operator.IN,userEmailIds.toArray())).execute();
					for (UserEmail userEmail : userEmails) {
						ret.add(userEmail.getUserId());
					}
				}
			}
		}else if (fieldName.equals("COMPANY_ID")){
			return super.getAllowedFieldValues(user,partiallyFilledModel,fieldName);
		}else if (fieldName.equals("COUNTRY_ID")){
			ret =  null ; //DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(Country.class, user));
		}else if (fieldName.equals("STATE_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getCountryId())){
				ret =  DataSecurityFilter.getIds(partiallyFilledModel.getCountry().getStates());
			}else {
				ret = null ;//DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(State.class, user));
			}
		}else if (fieldName.equals("CITY_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getStateId())){
				State state =  partiallyFilledModel.getState();
				if (state != null) {
					ret = DataSecurityFilter.getIds(state.getCities());
				}else {
					ret = null;
				}
			}else {
				ret = null ; //DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(City.class, user));
			}
		}
		return ret;
	}
}
