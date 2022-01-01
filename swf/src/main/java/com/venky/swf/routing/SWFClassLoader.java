package com.venky.swf.routing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.swf.routing.Config;

public class SWFClassLoader extends ClassLoader {
    private Set<File> pathsHandled = new HashSet<>();

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
    Set<Pattern> exceptionClasses = new HashSet<Pattern>(){{
        add(Pattern.compile("com\\.venky\\..*\\._[^\\.]*"));
        add(Pattern.compile("com\\.venky\\.swf\\.routing\\..*"));
        add(Pattern.compile("com\\.venky\\.core\\.log\\..*"));
        add(Pattern.compile("javax\\.servlet\\..*"));
    }};

    public Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
        /*
        import com.venky.swf.db._IDatabase;
        import com.venky.swf.path._IPath;
        import com.venky.swf.views._IView;
        import javax.servlet.ServletException;
        import javax.servlet.http.HttpServletRequest;
        import javax.servlet.http.HttpServletResponse;
        import jakarta.servlet.http.HttpSession;

         */
        boolean matches = false;
        for (Iterator<Pattern> pi = exceptionClasses.iterator() ; pi.hasNext()  && !matches ; ){
            Pattern p = pi.next();
            matches = matches || p.matcher(name).matches();
        }
        if (matches){
            return super.loadClass(name,resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c != null){
                return c;
            }
            String loc = name.replace('.', '/') + ".class";
            for (File file : pathsHandled) {
                try {
                    byte[] clazzByte = null;
                    if (file.isDirectory()) {
                        File classFile = new File(file, loc);
                        if (!classFile.exists()) {
                            continue;
                        }
                        clazzByte = StringUtil.readBytes(new FileInputStream(classFile));
                    } else {
                        JarFile jf = new JarFile(file);
                        JarEntry entry = jf.getJarEntry(loc);
                        if (entry == null) {
                            continue;
                        }
                        clazzByte = StringUtil.readBytes(jf.getInputStream(entry));
                        jf.close();
                    }
                    return defineClass(name, clazzByte, 0, clazzByte.length);
                } catch (IOException e) {
                    continue;
                }
            }
            return super.loadClass(name,resolve);
        }
    }

    public InputStream getResourceAsStream(String name) {
        for (File file : pathsHandled) {
            try {
                byte[] clazzByte = null;
                if (file.isDirectory()) {
                    File classFile = new File(file, name);
                    if (!classFile.exists()) {
                        continue;
                    }
                    clazzByte = StringUtil.readBytes(new FileInputStream(classFile));
                } else {
                    JarFile jf = new JarFile(file);
                    JarEntry entry = jf.getJarEntry(name);
                    if (entry == null) {
                        continue;
                    }
                    clazzByte = StringUtil.readBytes(jf.getInputStream(entry));
                    jf.close();
                }
                return new ByteArrayInputStream(clazzByte);
            } catch (IOException e) {
                continue;
            }
        }
        return super.getResourceAsStream(name);
    }
}
