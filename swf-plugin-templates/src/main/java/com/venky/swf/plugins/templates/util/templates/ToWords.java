package com.venky.swf.plugins.templates.util.templates;

import com.venky.core.string.StringUtil;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Map;
import java.util.StringTokenizer;

public class ToWords implements TemplateDirectiveModel{

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
        if (!params.isEmpty()) {
            throw new TemplateModelException(
                "This directive doesn't allow parameters.");
        }
        if (loopVars.length != 0) {
            throw new TemplateModelException(
                    "This directive doesn't allow loop variables.");
        }
        // If there is non-empty nested content:
        if (body != null) {
            // Executes the nested body. Same as <#nested> in FTL, except
            // that we use our own writer instead of the current output writer.
            body.render(new ToWordsFilterWriter(env.getOut()));
        } else {
            throw new RuntimeException("missing body");
        }
    }

    private class ToWordsFilterWriter extends Writer {
        private  final Writer out;
        public ToWordsFilterWriter(Writer out) {
            this.out = out;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            String s = new String(cbuf,off,len);
            try {
                StringTokenizer tokenizer = new StringTokenizer(StringUtil.toWords(NumberFormat.getInstance().parse(s).intValue()));
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    out.write(StringUtil.camelize(token));
                    if (tokenizer.hasMoreTokens()) {
                        out.write(" ");
                    }
                }
            }catch (Exception ex){
                out.write(s);
            }
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}
