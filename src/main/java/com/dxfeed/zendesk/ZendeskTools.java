package com.dxfeed.zendesk;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zendesk.client.v2.model.hc.Article;
import org.zendesk.client.v2.model.hc.Category;
import org.zendesk.client.v2.model.hc.Section;
import com.dxfeed.model.Document;

import java.util.Optional;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZendeskTools {

    public final @NonNull ZendeskFacade zendeskFacade;

    @Value("${properties.template.category.title}")
    private String categoryTitleTemplate;

    @Value("${properties.template.category.oldTitle}")
    private String categoryOldTitleTemplate;

    @Value("${properties.template.category.description}")
    private String categoryDescriptionTemplate;

    @Value("${properties.template.category.position}")
    private String categoryPositionTemplate;

    @Value("${properties.template.section.title}")
    private String sectionTitleTemplate;

    @Value("${properties.template.section.oldTitle}")
    private String sectionOldTitleTemplate;

    @Value("${properties.template.section.description}")
    private String sectionDescriptionTemplate;

    @Value("${properties.template.section.position}")
    private String sectionPositionTemplate;

    @Value("${zendesk.forced-update:false}")
    private boolean shouldUpdate;

    @Value("${zendesk.permission-group-title:}")
    private String permissionGroupTitle;

//    @Value("${zendesk.permission-group-id:}")
//    private Integer permissionGroupId;

    @Value("${zendesk.locale:en-us}")
    private String locale;

    @Value("${zendesk.comments-disabled:false}")
    private boolean commentsDisabled;

    public boolean updateHierarchy(ZendeskHierarchy hierarchy, Properties properties) {

        String catName    = properties.getProperty(categoryTitleTemplate, null);
        String catOldName = properties.getProperty(categoryOldTitleTemplate, null);
        String catDesc    = properties.getProperty(categoryDescriptionTemplate, "");
        int     catPos    = Integer.valueOf(properties.getProperty(categoryPositionTemplate, "0"));

        String secName    = properties.getProperty(sectionTitleTemplate, null);
        String secOldName = properties.getProperty(sectionOldTitleTemplate, null);
        String secDesc    = properties.getProperty(sectionDescriptionTemplate, "");
        int     secPos    = Integer.valueOf(properties.getProperty(sectionPositionTemplate, "0"));

        if (StringUtils.isBlank(catName) && (StringUtils.isBlank(secName))) {
            log.warn("neither category nor section name set in properties");
            return false;
        }

        // reset section on directory change
        if (StringUtils.isBlank(secName)) {
            hierarchy.section = null;
        }

        // load category if needed
        if (!StringUtils.isBlank(catName) && (null == hierarchy.category() || !hierarchy.category().getName().equalsIgnoreCase(catName))) {
            if (!StringUtils.isBlank(catOldName)) {
                log.warn(">>> RENAMING '{}' -> '{}'", catOldName, catName);
            }
            Optional<Category> categoryOpt = zendeskFacade.getCategory(catOldName, catName, catDesc, catPos, shouldUpdate);
            if (categoryOpt.isPresent()) {
                log.trace("~~~~~~~~~ got category: '{}' #{} #{}", categoryOpt.get().getName(), categoryOpt.get().getPosition(), categoryOpt.get().getId());
                hierarchy.category(categoryOpt.get());
            } else {
                log.warn("could not load category '{}'", (StringUtils.isBlank(catOldName)) ? catName : catOldName);
                return false;
            }
        }

        // load section if needed
        if (!StringUtils.isBlank(secName) && (null == hierarchy.section() || !hierarchy.section().getName().equalsIgnoreCase(secName))) {
            if (null == hierarchy.category()) {
                log.warn("category not set in hierarchy structure");
                return false;
            }
            Optional<Section> sectionOpt = zendeskFacade.getSection(hierarchy.category(), secOldName, secName, secDesc, secPos, shouldUpdate);
            if (sectionOpt.isPresent()) {
                hierarchy.section(sectionOpt.get());
            } else {
                log.warn("could not load section '{}'", (StringUtils.isBlank(secOldName)) ? secName : secOldName);
                return false;
            }
        }

        return true;
    }

    public Optional<Article> createArticle(Document document, Section section, String contents, Integer groupId) {
        if (null == document || null == section || StringUtils.isBlank(contents))
            return Optional.empty();
        try {
            Article article = new Article();
            article.setSectionId(section.getId());
            article.setTitle(document.title());
            article.setPosition(document.position());
            article.setDraft(document.draft());
            article.setPromoted(document.promoted());
            article.setBody(contents);
            article.setLabelNames(document.tags());
            article.setUserSegmentId(null);
            article.setPermissionGroupId(
                (null == groupId)
                ? zendeskFacade.getPermissionGroupId(permissionGroupTitle)
                : groupId
            );
            article.setCommentsDisabled(commentsDisabled);
            return Optional.of(article);
        } catch (Exception e) {
            log.error("error creating article: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Article> updateArticle(Article article, Document document, String contents) {
        if (null == article || null == document || StringUtils.isBlank(contents))
            return Optional.empty();
        try {
            article.setTitle(document.title());
            article.setBody(contents);
            article.setDraft(document.draft());
            article.setLabelNames(document.tags());
            article.setPromoted(document.promoted());
            article.setPosition(document.position());
            return Optional.of(article);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
