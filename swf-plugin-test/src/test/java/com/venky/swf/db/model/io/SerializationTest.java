package com.venky.swf.db.model.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Router;
import com.venky.swf.test.db.model.xml.Country;
import com.venky.swf.test.db.model.xml.State;
import com.venky.xml.XMLElement;

public class SerializationTest {
	@Before
	public void setUp(){ 
		Router.instance().setLoader(getClass().getClassLoader());
	}
	@Test
	public void test() throws IOException {
		Country c = Database.getTable(Country.class).newRecord();
		c.setName("India");
		c.save();
		
		State s1 = Database.getTable(State.class).newRecord();
		s1.setName("Karnataka");
		s1.setCountryId(c.getId());
		s1.save();
		
		State s2 = Database.getTable(State.class).newRecord();
		s2.setName("Maharashtra");
		s2.setCountryId(c.getId());
		s2.save();

		serializeAndDeserialize(Arrays.asList(new State[]{s1,s2}), State.class, XMLElement.class);
		serializeAndDeserialize(Arrays.asList(new State[]{s1,s2}), State.class, JSONObject.class);
		
	}
	
	private <M extends Model,T> void serializeAndDeserialize(List<M> models, Class<M> modelClass, Class<T> formatClass) throws IOException{
		ModelWriter<M,T> mw = ModelIOFactory.getWriter(modelClass,formatClass);
		ModelReader<M,T> mr = ModelIOFactory.getReader(modelClass,formatClass);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mw.write(models, baos, ModelReflector.instance(modelClass).getFields());
		System.out.println(baos.toString());
		List<M> deserialized = mr.read(new ByteArrayInputStream(baos.toByteArray()));
		Assert.assertEquals(deserialized.size(),models.size());
		for (int i = 0 ; i < models.size() ; i ++){
			Assert.assertTrue(ObjectUtil.equals(models.get(i),deserialized.get(i)));
		}
		
	}
 
}
