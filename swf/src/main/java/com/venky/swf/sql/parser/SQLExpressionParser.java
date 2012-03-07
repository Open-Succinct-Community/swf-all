package com.venky.swf.sql.parser;

import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.parse.Rule;
import com.venky.parse.Rule.Element;
import com.venky.parse.character.CharRange;
import com.venky.parse.character.Exclude;
import com.venky.parse.character.Include;
import com.venky.parse.composite.Any;
import com.venky.parse.composite.CharSequence;
import com.venky.parse.composite.Multiple;
import com.venky.parse.composite.OneOrMore;
import com.venky.parse.composite.Sequence;
import com.venky.parse.composite.ZeroOrMore;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;


public class SQLExpressionParser {
	
	Table<? extends Model> table = null; 
	public SQLExpressionParser(Class<? extends Model> modelClass){
		table = Database.getInstance().getTable(modelClass);
	}
	
	public Expression parse(InputStream is){
		return parse(StringUtil.read(is));
	}
	public Expression parse(String expression){
		ExpressionRule rule = new ExpressionRule();
		Expression ret = null; 
		if (rule.match(expression) && expression.length() == rule.getMatch().length()){
			Element e = rule.getMatch();
			ret = new Expression(Conjunction.AND);
			parse(e,ret);
		}
		return ret;
	}

	private void parse(Element e, Expression parentExpression){
		if (e.getRule().getClass() == EQ.class){
			parentExpression.add(createExpression(e, Operator.EQ));
		}else if (e.getRule().getClass() == LT.class){
			parentExpression.add(createExpression(e, Operator.LT));
		}else if (e.getRule().getClass() == GT.class){
			parentExpression.add(createExpression(e, Operator.GT));
		}else if (e.getRule().getClass() == LE.class){
			parentExpression.add(createExpression(e, Operator.LE));
		}else if (e.getRule().getClass() == GE.class){
			parentExpression.add(createExpression(e, Operator.GE));
		}else if (e.getRule().getClass() == NE.class){
			parentExpression.add(createExpression(e, Operator.NE));
		}else if (e.getRule().getClass() == LIKE.class){
			parentExpression.add(createExpression(e, Operator.LK));
		}else if (e.getRule().getClass() == IN.class){
			parentExpression.add(createExpression(e, Operator.IN));
		}else {
			Expression ex = null;
			if (e.getRule().getClass() == And.class){
				ex = new Expression(Conjunction.AND);
			}else if (e.getRule().getClass() == Or.class){
				ex = new Expression(Conjunction.OR);
			}
			if (ex != null){
				parentExpression.add(ex);
				parentExpression = ex;
			}
		}
			
		for (Element child: e.getChildren()){
			parse(child,parentExpression);
		}
	}
	private Expression createExpression(Element e, Operator op){
		String columnName = columnName(e);
		ColumnDescriptor cd = table.getColumnDescriptor(columnName);
		if (cd == null){
			throw new RuntimeException(table.getRealTableName() + " does not have column " + columnName);
		}
		List<BindVariable> columnValues = columnValues(e,cd);

		return new Expression(columnName,op,columnValues.toArray(new BindVariable[]{}));
	}
	private String columnName(Element e){
		List<Element> columnName = hunt(ColumnName.class,e);
		assert (columnName.size() == 1);
		return columnName.get(0).getText().trim();
	}
	private List<BindVariable> columnValues(Element e, ColumnDescriptor cd){
		Collection<Element> columnValues = hunt(ColumnValue.class,e);
		List<BindVariable> values = new ArrayList<BindVariable>();
		for (Element columnValue:columnValues){
			String sValue = columnValue.getText().trim();
			if (sValue.startsWith("'")){
				List<Element> quotedValues =  hunt(QuotedValue.class,columnValue);
				assert quotedValues.size() == 1 ;
				sValue = quotedValues.get(0).getText();
			}
			Object value = sValue;
			if (cd != null){
				int jdbcType = cd.getJDBCType();
				if (jdbcType != Types.VARCHAR){
					value = Database.getInstance().getJdbcTypeHelper().getTypeRef(jdbcType).getTypeConverter().valueOf(sValue);
				}
			}
			values.add(new BindVariable(value));
		}
		return values;
	}
	
	private List<Element> hunt(Class<? extends Rule> elementForRule , Element within){
		List<Element> ret = new ArrayList<Element>();
		if (within.getRule().getClass() == elementForRule){
			ret.add(within);
		}else {
			for (Element child : within.getChildren()){
				Collection<Element> hunted = hunt(elementForRule,child);
				if (hunted != null){
					ret.addAll(hunted);
				}
			}
		}
		//TODO Exception if nothing found at recurssion top.
		return ret;
	}
	
	
	public static class ExpressionRule extends PossiblyEnclosed {
		public ExpressionRule(){
			super(new Compound(new Operation()));
		}
	}
	
	public static class Operation extends PossiblyEnclosed {
		public Operation(){
			super(new Compound(new PossiblyEnclosed(new Compound(new SimpleOperation()))));
		}
	}

	
	public static class PossiblyEnclosed extends Rule {
		private Rule ruleTemplate = null;
		private int maxNumBrackets = UNKNOWN; 
		private static final int UNKNOWN = -1;
		public PossiblyEnclosed(Rule rule){
			this(rule,UNKNOWN);
		}
		public PossiblyEnclosed(Rule rule,int maxnumBrackets){
			this.ruleTemplate = rule;
			this.maxNumBrackets = maxnumBrackets;
		}
		
		@Override
		public boolean match(String input, int offset) {
			if (maxNumBrackets == UNKNOWN){
				Rule bracket = zeroOrMore(openParen());
				boolean bracketsPresent = bracket.match(input, offset);
				if (bracketsPresent){
					this.maxNumBrackets = bracket.getMatch().length();
				}
			}
			for (int i = 0 ; i <= maxNumBrackets ; i ++){
				Rule r = createRule(i);
				if (r.match(input,offset)){
					Element match = new Element(this);
					match.add(r.getMatch());
					setMatch(match);
					return true;
				}
			}
			return false;
		}
		
		private Rule createRule(int numBrackets){
			return new Sequence(new Multiple(openParen(),numBrackets,numBrackets), 
					ruleTemplate.createClone() , new Multiple(closeParen(),numBrackets,numBrackets));
		}
	}
	
	public static class Compound extends Any{
		public Compound(Rule rule){
			super (new And(rule.createClone()),new Or(rule.createClone()),rule.createClone());
		}
	}
	
	public static class And extends Sequence {
		public And(Rule rule){
			super(rule.createClone(), oneOrMore(sequence(new CharSequence("AND"), spaces(), rule.createClone())));
		}
	}
	public static class Or extends Sequence {
		public Or(Rule rule){
			super(rule.createClone(), oneOrMore(sequence(new CharSequence("OR"), spaces(), rule.createClone())));
		}
	}



	public static class ColumnName extends Sequence{
		public ColumnName(){
			super(letter(),new ZeroOrMore(new Any(letter(),underscore(),digit())),spaces());
		}
	}

	public static class ColumnValue extends Any {
		public ColumnValue(){
			super(new Sequence(new Include('\''),new QuotedValue(), new Include('\''),spaces()),
					new Sequence(new OneOrMore(new Exclude(' ','\t','\r','\n','\f','(', ')', '\'' , '>' , '<' , '=' , '!')),spaces()));
		}
	}
	
	public static class QuotedValue extends OneOrMore {
		public QuotedValue(){
			super(new Exclude('\''));
		}
	}

	public static class SimpleOperation extends Any{
		public SimpleOperation(){
			super(new EQ(),new LT(), new GT(), new LE() ,new GE(), new NE() , new LIKE() , new IN() );
		}
	}

	public static class EQ extends Sequence{
		public EQ(){
			super(columnName(), new Include('='),spaces() , columnValue());
		}
	}
	public static class LT extends Sequence{
		public LT(){
			super(columnName(), new Include('<'),spaces() , columnValue());
		}
	}
	public static class GT extends Sequence{
		public GT(){
			super(columnName(), new Include('>'),spaces() , columnValue());
		}
	}

	public static class LE extends Sequence{
		public LE(){
			super(columnName(), new Include('<'), new Include('='), spaces() , columnValue());
		}
	}

	public static class GE extends Sequence{
		public GE(){
			super(columnName(), new Include('>'), new Include('='), spaces() , columnValue());
		}
	}
	public static class NE extends Any{
		public NE(){
			super(
					new Sequence(columnName(), new Include('!'), new Include('='), spaces() , columnValue()),
					new Sequence(columnName(), new Include('<'), new Include('>'), spaces() , columnValue())
			);
		}
	}
	
	public static class LIKE extends Sequence{
		public LIKE(){
			super(columnName(), new CharSequence("LIKE"), spaces() , columnValue());
		}
	}
	
	
	public static class IN extends Sequence {
		public IN(){
			super(columnName(), new CharSequence("IN"),spaces() , columnValues());
		}
	}


	public static Rule columnName(){
		return new ColumnName();
	}
	
	public static Rule columnValue(){
		return new ColumnValue();
	}
	
	public static Rule columnValues(){
		return new Sequence( openParen(), 
				columnValue() , new ZeroOrMore(new Sequence(new Include(',') , columnValue())) , 
				closeParen() );
	}
	
	public static Rule underscore(){
		return new Include('_');
	}


	public static Rule openParen(){
		return new Sequence(new Include('('),spaces());
	}
	public static Rule closeParen(){
		return new Sequence(new Include(')'),spaces());
	}

	public static Rule letter(){
		return new Any(new CharRange('A', 'Z'),new CharRange('a', 'z'));
	}
	public static Rule digit(){
		return new CharRange('0','9');
	}
	public static Rule whiteSpace(){
		return new Include(' ','\t','\r','\n','\f');
	}
	
	public static Rule spaces() {
		return new ZeroOrMore(whiteSpace());
	}
	
	public static Rule sequence(Rule... rules){
		return new Sequence(rules);
	}

	public static Rule any(Rule... rules){
		return new Any(rules);
	}
	
	public static Rule oneOrMore(Rule rule){
		return new OneOrMore(rule);
	}
	public static Rule zeroOrMore(Rule rule){
		return new ZeroOrMore(rule);
	}

}
