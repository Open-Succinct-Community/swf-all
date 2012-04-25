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

import com.venky.core.string.StringUtil;

public class SWFClassLoader extends ClassLoader {
	private Set<File> pathsHandled = new HashSet<File>();

	public SWFClassLoader(ClassLoader parent) {
		super(parent);
		List<URL> watchedUrls = Config.instance().getResouceBaseUrls();
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

	public Class loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("com.venky.swf.routing")) {
			return super.loadClass(name);
		}else if (name.startsWith("com.venky.swf")
				&& name.substring(name.lastIndexOf(".") + 1).startsWith("_I")) {
			return super.loadClass(name);
		}

		String loc = name.replace('.', '/') + ".class";
		for (File file : pathsHandled){
			try {
				InputStream classStream = null;
				if (file.isDirectory()){
					File classFile = new File(file,loc);
					if (!classFile.exists()){
						continue;
					}
					classStream = new FileInputStream(classFile);
				}else {
					JarFile jf = new JarFile(file);
					JarEntry entry = jf.getJarEntry(loc);
					if (entry == null){
						continue;
					}
					classStream = jf.getInputStream(entry);
				}
				byte[] clazzByte = StringUtil.readBytes(classStream);
				return defineClass(name, clazzByte, 0, clazzByte.length);
			}catch (IOException e) {
				continue;
			}
		}
	
		return super.loadClass(name);
	}
}
