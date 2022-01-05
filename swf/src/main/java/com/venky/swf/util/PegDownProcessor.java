package com.venky.swf.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class PegDownProcessor {
    public PegDownProcessor(){

    }
    final private static MutableDataHolder OPTIONS = new MutableDataSet();
    static {
        OPTIONS.setFrom(ParserEmulationProfile.GITHUB);
    }
    /*final private static DataHolder OPTIONS = PegdownOptionsAdapter.flexmarkOptions(true,
            Extensions.ALL
    );*/


    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    public String markdownToHtml(String read) {
        return RENDERER.render(PARSER.parse(read));
    }
}