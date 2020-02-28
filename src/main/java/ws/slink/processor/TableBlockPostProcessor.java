package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;

@Slf4j
public class TableBlockPostProcessor extends Postprocessor {

    @Override
    public String process(Document document, String convertedDocument) {
        final org.jsoup.nodes.Document doc = Jsoup.parse(convertedDocument);
        org.jsoup.nodes.Document.OutputSettings settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);
        settings.syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        doc.outputSettings(settings);
        doc.select("table").stream().forEach(element -> {
            String classAttribute = element.attributes().get("class");
            element.attributes().put("class", classAttribute + " table");
//            element.attributes().put("border", "1");
        });
        return doc.body().toString();
    }

}
