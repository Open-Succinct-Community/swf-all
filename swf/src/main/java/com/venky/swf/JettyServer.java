package com.venky.swf;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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
		
		String pidfile = System.getProperty("swf.pidfile");
		if (!ObjectUtil.isVoid(pidfile)){
			PrintWriter pw = new PrintWriter(new FileWriter(pidfile, true));
			String name = ManagementFactory.getRuntimeMXBean().getName();
			pw.write(name);
			pw.close();
		}
		
		JettyServer s = new JettyServer(Integer.valueOf(Config.instance().getPortNumber()));
		s.start();
	}
	public boolean isDevelopmentEnvironment(){
		return Config.instance().isDevelopmentEnvironment();
	}
	
	public void start() throws Exception {
		if (Config.instance().getBooleanProperty("System.out.close",false)){
			System.out.close();
		}
		if (Config.instance().getBooleanProperty("System.err.close",false)){
			System.err.close();
		}
		if (Config.instance().getBooleanProperty("System.in.close",false)){
			System.in.close();
		}

		Server server = new Server();
		addConnectors(server);
		server.setStopAtShutdown(true);
		server.setStopTimeout(100);
		Router router = Router.instance(); 
		if (isDevelopmentEnvironment()){
			router.setLoader(new SWFClassLoader(getClass().getClassLoader()));
		}else {
			router.setLoader(getClass().getClassLoader());
		}
		
		ContextHandler ctxHandler = new ContextHandler();
		ctxHandler.setHandler(router);

		SessionHandler sessionHandler = new SessionHandler();
		sessionHandler.setHandler(ctxHandler);
		
		server.setHandler(sessionHandler);
		
		server.start();
		server.join();
	}
	
	private void addConnectors(Server server){
		
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(this.port);

		
		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());
		SslContextFactory sslContextFactory = new SslContextFactory();

		System.out.println("SSL Store" + Config.instance().getProperty("swf.ssl.key.store.path",getClass().getClassLoader().getResource("config/keys/keystore.jks").toExternalForm()));
		sslContextFactory.setKeyStorePath(Config.instance().getProperty("swf.ssl.key.store.path",getClass().getClassLoader().getResource("config/keys/keystore.jks").toExternalForm()));
		sslContextFactory.setKeyStorePassword(Config.instance().getProperty("swf.ssl.key.store.pass","venky12"));
		
		sslContextFactory.setKeyManagerPassword(Config.instance().getProperty("swf.ssl.key.manager.pass","venky12"));
		/*
		sslContextFactory.setTrustStorePath(sslContextFactory.getKeyStorePath());
		sslContextFactory.setTrustStorePassword(Config.instance().getProperty("swf.ssl.key.manager.pass","venky12"));
		*/
		ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
		sslConnector.setPort(this.port+363); // 80 + 363 = 443 ; convention
		
		server.setConnectors(new Connector[]{connector,sslConnector});
		
	}

}
