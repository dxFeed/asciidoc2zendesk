package com.dxfeed.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

@Slf4j
public class AdmonitionBlockPostProcessor extends Postprocessor {

    @Override
    public String process(Document document, String convertedDocument) {
        final org.jsoup.nodes.Document doc = Jsoup.parse(convertedDocument);
        org.jsoup.nodes.Document.OutputSettings settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);
        settings.syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        doc.outputSettings(settings);
        doc.select("div.admonitionblock").stream().forEach(element -> {
            Elements td = element.select("td.content");
            if (null != td)
                element.html(td.html());
        });
        return doc.body().toString();
    }

}
