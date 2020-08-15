package com.dxfeed.processor;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ImageBlockPostProcessor extends Postprocessor {

    @Override
    public String process(Document document, String convertedDocument) {
        final org.jsoup.nodes.Document doc = Jsoup.parse(convertedDocument);

        org.jsoup.nodes.Document.OutputSettings settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);
        settings.syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        doc.outputSettings(settings);

        // process center-aligned images
        doc.select("div.imageblock.text-center").stream().forEach(element -> {
//            System.err.println(element.toString());
            Element content = element.selectFirst("div.content");
            content.addClass("wysiwyg-text-align-center");
        });

        // process left-aligned images
        doc.select("div.imageblock.text-left").stream().forEach(element -> {
//            System.err.println(element.toString());
            Element content = element.selectFirst("div.content");
            content.addClass("wysiwyg-text-align-left");
        });

        // process right-aligned images
        doc.select("div.imageblock.text-right").stream().forEach(element -> {
//            System.err.println(element.toString());
            Element content = element.selectFirst("div.content");
            content.addClass("wysiwyg-text-align-right");
        });

        return doc.body().toString();
    }

}
