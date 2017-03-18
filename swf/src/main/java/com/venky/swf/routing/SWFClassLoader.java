package com.venky.swf.routing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.swf.routing.Config;

public class SWFClassLoader extends ClassLoader {
	private Set<File> pathsHandled = new HashSet<File>();

	public SWFClassLoader(ClassLoader parent) {
		super(parent);
		List<URL> watchedUrls = Config.instance().getResourceBaseUrls();
		for (URL watchedUrl : watchedUrls) {
			pathsHandled.add(getFile(watchedUrl, ""));
		}
	}

	public File getFile(URL watchedUrl, String loc) {
		String path = watchedUrl.getPath();

		if (watchedUrl.getProtocol().equals("jar")) {
			return new File(path.substring("file:".length(),
					path.lastIndexOf("!")));
		} else if (watchedUrl.getProtocol().equals("file")) {
			return new File(path.substring(0, path.length() - loc.length()));
		} else {
			throw new RuntimeException("Don't know how to load Class from url:"
					+ watchedUrl.toString());
		}
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("com.venky.swf.routing")) {
			return super.loadClass(name);
		}else if (name.startsWith("com.venky.swf.util.SWFLogger") || name.startsWith("com.venky.core.log")) {
			return super.loadClass(name);
		}else if (name.startsWith("com.venky.swf")
				&& name.substring(name.lastIndexOf(".") + 1).startsWith("_I")) {
			return super.loadClass(name);
		}

		String loc = name.replace('.', '/') + ".class";
		for (File file : pathsHandled){
			try {
				byte[] clazzByte = null;
				if (file.isDirectory()){
					File classFile = new File(file,loc);
					if (!classFile.exists()){
						continue;
					}
					clazzByte = StringUtil.readBytes(new FileInputStream(classFile));
				}else {
					JarFile jf = new JarFile(file);
					JarEntry entry = jf.getJarEntry(loc);
					if (entry == null){
						continue;
					}
					clazzByte = StringUtil.readBytes(jf.getInputStream(entry));
					jf.close();
				}
				return defineClass(name, clazzByte, 0, clazzByte.length);
			}catch (IOException e) {
				continue;
			}
		}
	
		return super.loadClass(name);
	}
	public InputStream getResourceAsStream(String name) {
		for (File file : pathsHandled){
			try {
				byte[] clazzByte = null;
				if (file.isDirectory()){
					File classFile = new File(file,name);
					if (!classFile.exists()){
						continue;
					}
					clazzByte = StringUtil.readBytes(new FileInputStream(classFile));
				}else {
					JarFile jf = new JarFile(file);
					JarEntry entry = jf.getJarEntry(name);
					if (entry == null){
						continue;
					}
					clazzByte = StringUtil.readBytes(jf.getInputStream(entry));
					jf.close();
				}
				return new ByteArrayInputStream(clazzByte);
			}catch(IOException e){
				continue;
			}
		}
		return super.getResourceAsStream(name);
	}
}
