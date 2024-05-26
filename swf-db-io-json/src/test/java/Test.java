import com.venky.swf.extensions.JSONRegistrar;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.JSON;
import com.venky.swf.routing.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Test {
    @org.junit.BeforeClass
    public static void setup() throws Exception{
        Class.forName(JSONRegistrar.class.getName());
    }
    @org.junit.Test
    public void test(){
        JSONObject o = new JSONObject();
        JSONArray arr = new JSONArray();
        o.put("Items",arr );
        JSONObject item =new JSONObject();
        item.put("Name","Venky");
        arr.add(item);
        Config.instance().setProperty("swf.api.keys.case","SNAKE");
        FormatHelper<JSONObject> f = new JSON(o);



    }
}
