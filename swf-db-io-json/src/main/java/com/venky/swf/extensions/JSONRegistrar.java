package com.venky.swf.extensions;

import java.io.InputStream;

import org.json.simple.JSONObject;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.io.ModelReader;
import com.venky.swf.db.model.io.ModelReaderFactory;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.db.model.io.ModelWriterFactory;
import com.venky.swf.db.model.io.json.JSONModelReader;
import com.venky.swf.db.model.io.json.JSONModelWriter;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelperBuilder;
import com.venky.swf.integration.JSON;

public class JSONRegistrar {
	static {
		FormatHelper.registerFormat(MimeType.APPLICATION_JSON,
				JSONObject.class, new FormatHelperBuilder<JSONObject>() {

					@Override
					public FormatHelper<JSONObject> constructFormatHelper(
							InputStream in) {
						return new JSON(in);
					}

					@Override
					public FormatHelper<JSONObject> constructFormatHelper(
							String root, boolean isPlural) {
						return new JSON(root, isPlural);
					}

					@Override
					public FormatHelper<JSONObject> constructFormatHelper(
							JSONObject rootElement) {
						return new JSON(rootElement);
					}

				});

		ModelIOFactory.registerIOFactories(JSONObject.class,
				new ModelReaderFactory<JSONObject>() {

					@Override
					public <M extends Model> ModelReader<M, JSONObject> createModelReader(
							Class<M> modelClass) {
						return new JSONModelReader<M>(modelClass);
					}
				}, new ModelWriterFactory<JSONObject>() {

					@Override
					public <M extends Model> ModelWriter<M, JSONObject> createModelWriter(
							Class<M> modelClass) {
						return new JSONModelWriter<M>(modelClass);
					}
				});
	}
}
