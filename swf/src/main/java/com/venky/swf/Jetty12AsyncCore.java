package com.venky.swf;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.thread.VirtualThreadPool;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.*;

public class Jetty12AsyncCore {
    public static void main(String[] args) throws Exception {
        // Keep Jetty’s pool for I/O; use a separate pool for your business work
        Server server = new Server(new VirtualThreadPool());
        
        
        // Plain connector; Jetty will speak HTTP/1.1 and HTTP/2 if ALPN/TLS configured.
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);
        
        // A small executor for async "business" work so we don't block Jetty’s I/O.
        ExecutorService workers = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()));
        
        // Root handler (no Servlet API) – fully async.
        Handler root = new Handler.Abstract.NonBlocking() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                String path = request.getHttpURI().getPath();
                
                // Example 1: async echo of request body (non-blocking read & write)
                if ("/echo".equals(path)) {
                    Content.Source.asStringAsync(request, StandardCharsets.UTF_8) // non-blocking body read
                            .thenCompose(body -> CompletableFuture.supplyAsync(() -> {
                                        // Simulate asynchronous business logic
                                        return "You said: " + body + "\n@ " + Instant.now() + "\n";
                                    }, workers)
                            )
                            .whenComplete((msg, fail) -> {
                                if (fail != null) {
                                    callback.failed(fail);
                                    return;
                                }
                                response.setStatus(200);
                                response.getHeaders().put("Content-Type", "text/plain; charset=utf-8");
                                // Non-blocking write; completes the overall callback when done.
                                Content.Sink.write(response, true, msg, Callback.from(callback::succeeded, callback::failed));
                            });
                    return true; // handled asynchronously
                }
                
                // Example 2: quick, non-blocking hello (no request body read)
                if ("/hello".equals(path)) {
                    String msg = "hello from jetty-core async (" + Instant.now() + ")\n";
                    response.setStatus(200);
                    response.getHeaders().put("Content-Type", "text/plain; charset=utf-8");
                    Content.Sink.write(response, true, msg, Callback.from(callback::succeeded, callback::failed));
                    return true;
                }
                
                // 404 for everything else
                response.setStatus(404);
                response.getHeaders().put("Content-Type", "text/plain; charset=utf-8");
                Content.Sink.write(response, true, "not found\n", Callback.from(callback::succeeded, callback::failed));
                return true;
            }
        };
        
        server.setHandler(root);
        server.start();
        server.join();
        // (Shutdown hooks omitted for brevity; add one to stop 'workers' and 'server' cleanly.)
    }
}
