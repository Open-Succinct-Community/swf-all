import com.venky.core.string.StringUtil;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class UrlSanitizer {
    @Test
    public void testUrl() throws Exception{
        String url = "http://url/a//b/c";
        Assert.assertEquals(new URI(url).normalize().toString(),"http://url/a/b/c");
    }

}
