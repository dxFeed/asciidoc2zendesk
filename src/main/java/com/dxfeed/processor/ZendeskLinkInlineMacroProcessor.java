package com.dxfeed.processor;

import org.apache.commons.lang.StringUtils;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.zendesk.client.v2.model.hc.Article;
import com.dxfeed.zendesk.ZendeskFacade;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Name ("zlink")
public class ZendeskLinkInlineMacroProcessor extends InlineMacroProcessor {

    private final ZendeskFacade zendeskFacade;

    public ZendeskLinkInlineMacroProcessor(ZendeskFacade zendeskFacade) {
        this.zendeskFacade = zendeskFacade;
    }

    @Override
    public Object process (ContentNode parent, String target, Map<String, Object> attributes) {
//        System.err.println(attributes);
        try {
            String category = attributes.getOrDefault("category", "").toString().replaceAll("\\+", " ");
            String section  = attributes.getOrDefault("section", "").toString().replaceAll("\\+", " ");
            String title    = target.replaceAll("\\+", " ");
            String link     = attributes.get("1").toString();
//            System.err.println("category: " + category);
//            System.err.println("section : " + section);
//            System.err.println("document: " + title);
//            System.err.println("link    : " + link);
            Optional<Article> article;
            if (StringUtils.isNotBlank(category) && StringUtils.isNotBlank(section)) {
                article = zendeskFacade.getArticleByName(category, section, title);
            } else if (StringUtils.isNotBlank(section)) {
                article = zendeskFacade.getArticleByName(section, title);
            } else {
                article = zendeskFacade.getArticleByName(title);
            }
            Map<String, Object> options = new HashMap<>();
            options.put("type", ":link");
            if (article.isPresent()) {
                options.put("target", article.get().getHtmlUrl());
            } else {
                options.put("target", "<none>");
            }
            return createPhraseNode(parent, "anchor", link, attributes, options);
        } catch (Exception e) {
            Map<String, Object> options = new HashMap<>();
            options.put("type", ":link");
            options.put("target", target);
            return createPhraseNode(parent, "anchor", target, attributes, options);
        }
    }
}
