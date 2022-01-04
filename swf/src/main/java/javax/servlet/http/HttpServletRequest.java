package javax.servlet.http;


import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.IOException;

public interface HttpServletRequest extends jakarta.servlet.http.HttpServletRequest, ServletRequest {
    @Override
    ServletInputStream getInputStream() throws IOException;
}
