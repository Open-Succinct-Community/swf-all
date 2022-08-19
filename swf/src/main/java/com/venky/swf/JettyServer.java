package com.venky.swf;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.Router;
import com.venky.swf.routing.SWFClassLoader;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

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
			PrintWriter pw = new PrintWriter(new FileWriter(pidfile, false));
			String name = ManagementFactory.getRuntimeMXBean().getName();
			pw.write(name.split("@")[0]);
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
		sessionHandler.setMaxInactiveInterval(Config.instance().getIntProperty("swf.jetty.session.timeout.seconds",10*60)); //10 minutes;
		
		server.setHandler(sessionHandler);
		
		server.start();
		server.join();
	}
	
	private void addConnectors(Server server){
		
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(this.port);

		List<Connector> connectors = new ArrayList<>(); 
		connectors.add(connector);
		server.setConnectors(connectors.toArray(new Connector[]{}));
		
	}

}
