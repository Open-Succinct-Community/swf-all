package com.venky.swf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.Router;
import com.venky.swf.routing.SWFClassLoader;

/**
 * Hello world!
 * 
 */
public class JettyServer {
	int port = 8080;

	public JettyServer() {
	}

	public JettyServer(int port) {
		this();
		this.port = port;
	}

	public static void main(String[] args) throws Exception {
		String port = System.getenv("PORT");
		if (ObjectUtil.isVoid(port)) {
			port = "8080";
		}

		JettyServer s = new JettyServer(Integer.valueOf(port));
		s.start();
	}

	public void start() throws Exception {
		addDirectoryWatches();
		Server server = new Server(this.port);
		server.setGracefulShutdown(100);
		Router router = Router.instance();
		router.setLoader(new SWFClassLoader(getClass().getClassLoader()));
		SessionHandler sessionHandler = new SessionHandler();
		sessionHandler.setHandler(router);
		server.setHandler(sessionHandler);
		server.start();
		server.join();
	}

	private void addDirectoryWatches() {
		List<File> watchedFiles = new ArrayList<File>();
		for (URL url : Config.instance().getResouceBaseUrls()) {
			File root = null;
			if (url.getProtocol().equals("jar")) {
				root = new File(url.getFile().substring("file:".length(),
						url.getFile().lastIndexOf("!"))).getParentFile();
			} else if (url.getProtocol().equals("file")) {
				root = new File(url.getPath());
			}
			if (root != null) {
				watchedFiles.add(root);
			}
		}
		try {
			Watcher watcher = new Watcher(watchedFiles);
			Thread th = new Thread(watcher);
			th.setDaemon(true);
			th.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static class Watcher implements Runnable {
		Map<WatchKey, Path> watchKeys = new HashMap<WatchKey, Path>();
		WatchService watcher = null;

		public Watcher(List<File> files) throws IOException{
			this.watcher = FileSystems.getDefault().newWatchService();
			for (File file:files){
				addWatch(file);
			}
		}
		public void addWatch(File file) throws IOException{
			Stack<File> files = new Stack<File>();
			files.push(file);
			while (!files.isEmpty()) {
				File f = files.pop();
				if (f.isDirectory()) {
					Path directory = Paths.get(f.getPath());
					Logger.getLogger(getClass().getName()).log(Level.INFO,"Added Watch for {0}",directory);
					watchKeys.put(directory.register(watcher,
							StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_DELETE,
							StandardWatchEventKinds.ENTRY_MODIFY), directory);
					files.addAll(Arrays.asList(f.listFiles()));
				}
			}
		}

		public void run() {
			WatchKey key = null;
			do {
				try {
					key = watcher.take();
				} catch (InterruptedException e) {
					break;
				}
				Path watched = watchKeys.get(key);
				boolean changeDetected = false; 
				for (WatchEvent<?> event : key.pollEvents()) {
					Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						System.out.println("OVERFLOW Event");
						continue;
					}
					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path name = ev.context();
					Path child = watched.resolve(name);
					
					Logger.getLogger(getClass().getName()).log(Level.INFO, "Received {0} of {1} inside {2} " , new Object[] {kind , name , watched });
					if (kind == StandardWatchEventKinds.ENTRY_CREATE){
						File childFile = child.toFile();
						if (childFile.isDirectory()){
							try {
								addWatch(childFile);
							} catch (IOException e) {
								//
							}
						}
					}
					changeDetected = true;
					
				}
				key.reset();
				if (changeDetected){
					Router.instance().setLoader(new SWFClassLoader(getClass().getClassLoader()));
				}
			} while (true);
		}
	}

}
