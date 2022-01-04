package javax.servlet;




import java.io.IOException;

public interface ServletRequest extends jakarta.servlet.ServletRequest {
    public ServletInputStream getInputStream() throws IOException;
}
