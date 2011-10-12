package com.venky.swf;

import com.venky.swf.db.Database;
import com.venky.swf.routing.Router;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * Hello world!
 *
 */
public class JettyServer {
		int port = 8080;
		public JettyServer(){
		}
		public JettyServer(int port){
			this();
			this.port = port;	
		}

    public static void main(String[] args) throws Exception {
				JettyServer s = new JettyServer();
				s.start();
    }

		public  void start() throws Exception{
        Runtime.getRuntime().addShutdownHook(new Thread(new Hook(Database.getInstance())));
        Server server = new Server(this.port);
        server.setGracefulShutdown(100);
        Router router = new Router();
        SessionHandler sessionHandler= new SessionHandler();
        sessionHandler.setHandler(router);
        server.setHandler(sessionHandler);
        server.start();
        server.join();
		}
    
    public static class Hook implements Runnable {
        Database db = null; 
        public Hook(Database db){
            this.db = db;
        }
        public void run() {
            System.out.println("Closing the DB");
            db.close();
        }
        
    }
}

