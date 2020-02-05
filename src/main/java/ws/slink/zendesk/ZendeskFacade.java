package ws.slink.zendesk;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zendesk.client.v2.Zendesk;
import org.zendesk.client.v2.ZendeskResponseException;
import org.zendesk.client.v2.ZendeskResponseRateLimitException;
import org.zendesk.client.v2.model.hc.*;
import ws.slink.config.AppConfig;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZendeskFacade {

    private final @NonNull AppConfig appConfig;

    @Value("${zendesk.max-attempts}")
    private int maxRequestAttempts;

    private Zendesk zendesk = null;

    @PostConstruct
    private void init() {
        System.out.println("--- Application Configuration ----------------------------------");
        appConfig.print();
        try {
            zendesk = new Zendesk.Builder(appConfig.url())
                    .setUsername(appConfig.user())
                    .setToken(appConfig.token())
                    .build();
        } catch (Exception e) {
            log.warn("Could not initialize ZenDesk client");
        }
    }


    public List<Category> getCategories() {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return StreamSupport
                        .stream(zendesk.getCategories().spliterator(), true)
                        .collect(Collectors.toList());
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting categories: {} {}", e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error requesting categories from zendesk: {}", e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum request attempts reached, no category data received from zendesk");
        return Collections.emptyList();
    }
    /**
     * retrieve category from zendesk by category name
     *
     * @param categoryName
     * @return Optional<Category> or empty if no category found or error occurred
     */
    public Optional<Category> getCategory(String categoryName) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return StreamSupport
                    .stream(zendesk.getCategories().spliterator(), true)
                    .filter(v -> v.getName().equalsIgnoreCase(categoryName))
                    .findFirst();
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting category '{}': {} {}", categoryName, e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error requesting category '{}' from zendesk: {}", categoryName, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum request attempts reached, no category data received from zendesk");
        return Optional.empty();
    }
    /**
     * add category to zendesk server
     *
     * @param name
     * @param description
     * @param position
     * @return newly created category or empty if could not add category
     */
    public Optional<Category> addCategory(String name, String description, long position) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                Category category = new Category();
                category.setName(name);
                category.setDescription(description);
                category.setPosition(position);
                return Optional.ofNullable(zendesk.createCategory(category));
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred creating category '{}': {} {}", name, e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error creating category '{}': {}", name, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum API request attempts reached");
        return Optional.empty();
    }
    /**
     * update category on zendesk server
     *
     * @param category
     * @param newName
     * @param newDescription
     * @param newPosition
     * @return updated category or empty if could not update
     */
    public Optional<Category> updateCategory(Category category, String newName, String newDescription, long newPosition) {
        if (category.getName().equals(newName)
        && category.getDescription().equalsIgnoreCase(newDescription)
        && category.getPosition() == newPosition) {
            log.info("category '{}' not changed, no update needed", newName);
            return Optional.of(category);
        }
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                category.setPosition(newPosition);
                Category categoryUpdated = zendesk.updateCategory(category);
                StreamSupport.stream(getTranslations(categoryUpdated).spliterator(), false)
                        .findFirst()
                        .ifPresent(t -> {
                            t.setTitle(newName);
                            t.setBody(newDescription);
                            zendesk.updateCategoryTranslation(categoryUpdated.getId(), t.getLocale(), t);
                        });
                return Optional.ofNullable(categoryUpdated);
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred updating category '{}': {} {}", newName, e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error updating category '{}': {}", newName, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum API request attempts reached");
        return Optional.empty();
    }
    /**
     * get category by its name or create new one with given parameters
     *
     * @param name
     * @param description
     * @param position
     * @param update if existing category should be forcefully updated on a server with given parameters
     * @return Optional<Category> or empty if error occurred
     */
    public Optional<Category> getCategory(String name, String newName, String description, long position, boolean update) {
        Optional<Category> categoryOpt = getCategory(name);
        if (categoryOpt.isPresent()) {
            if (update) {
                log.trace("updating category '{}': {} #{}", categoryOpt.get().getId(), newName, position);
                return updateCategory(categoryOpt.get(), newName, description, position);
            } else {
                log.trace("category found: {} #{}", categoryOpt.get().getId(), position);
                return categoryOpt;
            }
        } else {
            log.trace("adding category '{}' #{}", newName, position);
            return addCategory(newName, description, position);
        }
    }
    public boolean removeCategory(Category category) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                zendesk.deleteCategory(category);
                return true;
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred removing category '{}': {} {}", category.getName(), e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error removing category '{}' from zendesk: {}", category.getName(), e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum request attempts reached, could not delete category '{}' from zendesk", category.getName());
        return false;
    }


    /**
     * retrieve section from zendesk by category and sectionName
     *
     * @param sectionName
     * @return Optional<Section> or empty if no category found or error occurred
     */
    public Optional<Section> getSection(Category category, String sectionName) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return StreamSupport
                    .stream(zendesk.getSections(category).spliterator(), true)
                    .filter(v -> v.getName().equalsIgnoreCase(sectionName))
                    .findFirst();
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting section '{}': {} {}", sectionName, e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error requesting section '{}' from zendesk: {}", sectionName, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum request attempts reached, no data received from zendesk");
        return Optional.empty();
    }
    public Optional<Section> getSection(String categoryName, String sectionName) {
        Optional<Category> categoryOpt = getCategory(categoryName);
        if (categoryOpt.isPresent()) {
            return getSection(categoryOpt.get(), sectionName);
        } else {
            log.trace("no category found for name '{}', won't query for section '{}'", categoryOpt, sectionName);
            return Optional.empty();
        }
    }
    /**
     * add section to zendesk server
     *
     * @param name
     * @param description
     * @param position
     * @return newly created section or empty if could not add
     */
    public Optional<Section> addSection(Category category, String name, String description, long position) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                Section section = new Section();
                section.setCategoryId(category.getId());
                section.setName(name);
                section.setDescription(description);
                section.setPosition(position);
                return Optional.ofNullable(zendesk.createSection(section));
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred creating section '{}': {} {}", name, e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error creating section '{}': {}", name, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum API request attempts reached");
        return Optional.empty();
    }
    public Optional<Section> addSection(String categoryName, String name, String description, long position) {
        Optional<Category> categoryOpt = getCategory(categoryName);
        if (categoryOpt.isPresent()) {
            return addSection(categoryOpt.get(), name, description, position);
        } else {
            log.trace("no category found for name '{}', won't add section '{}'", categoryOpt, name);
            return Optional.empty();
        }
    }
    /**
     * update section on zendesk server
     *
     * @param section
     * @param newName
     * @param newDescription
     * @param newPosition
     * @return updated category or empty if could not update
     */
    public Optional<Section> updateSection(Section section, String newName, String newDescription, long newPosition) {
        if (section.getName().equals(newName)
        && section.getDescription().equalsIgnoreCase(newDescription)
        && section.getPosition() == newPosition) {
            log.info("section '{}' not changed, no update needed", newName);
            return Optional.of(section);
        }
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                section.setPosition(newPosition);
                Section sectionUpdated = zendesk.updateSection(section);
                StreamSupport.stream(getTranslations(sectionUpdated).spliterator(), false)
                        .findFirst()
                        .ifPresent(t -> {
                            t.setTitle(newName);
                            t.setBody(newDescription);
                            zendesk.updateSectionTranslation(sectionUpdated.getId(), t.getLocale(), t);
                        });
                return Optional.ofNullable(sectionUpdated);
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred updating section '{}': {} {}", newName, e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error updating section '{}': {}", newName, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum API request attempts reached");
        return Optional.empty();
    }
    /**
     * get section by its name or create new one with given parameters
     *
     * @param categoryName
     * @param name
     * @param description
     * @param position
     * @param update if existing section should be forcefully updated on a server with given parameters
     * @return Optional<Section> or empty if error occurred
     */
    public Optional<Section> getSection(String categoryName, String name, String description, long position, boolean update) {
        Optional<Section> sectionOpt = getSection(categoryName, name);
        if (sectionOpt.isPresent()) {
            if (update) {
                log.trace("updating section '{}': {} #{}", sectionOpt.get().getId(), name, position);
                return updateSection(sectionOpt.get(), name, description, position);
            } else {
                log.trace("section found: {} #{}", sectionOpt.get().getId(), position);
                return sectionOpt;
            }
        } else {
            log.trace("adding section '{}' #{}", name, position);
            return addSection(categoryName, name, description, position);
        }
    }
    public Optional<Section> getSection(Category category, String name, String newName, String description, long position, boolean update) {
        Optional<Section> sectionOpt = getSection(category, name);
        if (sectionOpt.isPresent()) {
            if (update) {
                log.trace("updating section '{}': {} #{}", sectionOpt.get().getId(), newName, position);
                return updateSection(sectionOpt.get(), newName, description, position);
            } else {
                log.trace("section found: {} #{}", sectionOpt.get().getId(), position);
                return sectionOpt;
            }
        } else {
            log.trace("adding section '{}' #{}", newName, position);
            return addSection(category, newName, description, position);
        }
    }


    public List<Article> getArticles(Section section) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return StreamSupport
                        .stream(zendesk.getArticles(section).spliterator(), false)
                        .collect(Collectors.toList());
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting article list: {} {} {}", e.getStatusCode(), e.getStatusText(), e.getMessage());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error requesting article list from zendesk: {}", e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("Maximum request attempts reached, no data received from Zendesk");
        return Collections.EMPTY_LIST;
    }
    public List<Article> getArticles() {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return StreamSupport
                        .stream(zendesk.getArticles().spliterator(), false)
                        .collect(Collectors.toList());
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting article list: {} {} {}", e.getStatusCode(), e.getStatusText(), e.getMessage());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error requesting article list from zendesk: {}", e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("Maximum request attempts reached, no data received from Zendesk");
        return Collections.EMPTY_LIST;
    }


    public Optional<Article> getArticle(Section section, String articleTitle) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return StreamSupport
                        .stream(zendesk.getArticles(section).spliterator(), true)
                        .filter(v -> v.getTitle().equalsIgnoreCase(articleTitle))
                        .findFirst();
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting article '{}': {} {} {}", articleTitle, e.getStatusCode(), e.getStatusText(), e.getMessage());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error requesting article '{}' from zendesk: {}", articleTitle, e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("Maximum request attempts reached, no data received from Zendesk");
        return Optional.empty();
    }
    public Optional<Article> addArticle(Article article) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                return Optional.ofNullable(zendesk.createArticle(article));
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred creating article '{}': {} {}", article.getTitle(), e.getStatusCode(), e.getStatusText());
                if (log.isTraceEnabled())
                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error creating article '{}': {}", article.getTitle(), e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum API request attempts reached");
        return Optional.empty();
    }
    public Optional<Article> updateArticle(Article article) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                Optional<Article> updatedArticle = Optional.ofNullable(zendesk.updateArticle(article));
                StreamSupport.stream(zendesk.getArticleTranslations(article.getId()).spliterator(), false)
                    .findFirst().ifPresent(translation -> {
                        translation.setBody(article.getBody());
                        translation.setDraft(article.getDraft());
                        translation.setTitle(article.getTitle());
//                        zendesk.deleteTranslation(translation);
//                        zendesk.createArticleTranslation(article.getId(), translation);
                        zendesk.updateArticleTranslation(article.getId(), translation.getLocale(), translation);
//                        if (null == zendesk.updateArticleTranslation(article.getId(), article.getLocale(), translation)) {
//                            log.warn("could not update translation for article '{}'", article.getTitle());
//                        }
                    }
                 );
                return updatedArticle;
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred updating article '{}': {} {}", article.getTitle(), e.getStatusCode(), e.getStatusText());
//                if (log.isTraceEnabled())
//                    log.warn("{}", e.getBody());
            } catch (Exception e) {
                log.warn("error updating article '{}': {}", article.getTitle(), e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("maximum API request attempts reached");
        return Optional.empty();
    }
    public boolean removeArticle(Article article) {
        for (int i = 0; i < maxRequestAttempts; i++) {
            try {
                zendesk.deleteArticle(article);
                return true;
            } catch (ZendeskResponseRateLimitException rateLimit) {
                apiRateLimitWait(rateLimit.getRetryAfter());
            } catch (ZendeskResponseException e) {
                log.warn("zendesk exception occurred requesting article list: {} {} {}", e.getStatusCode(), e.getStatusText(), e.getMessage());
            } catch (Exception e) {
                log.warn("error requesting article list from zendesk: {}", e.getMessage());
                if (log.isTraceEnabled())
                    e.printStackTrace();
            }
        }
        log.info("Maximum request attempts reached, no data received from Zendesk");
        return false;
    }

    public Iterable<Translation> getTranslations(Article article) {
        return zendesk.getArticleTranslations(article.getId());
    }
    public Iterable<Translation> getTranslations(Category category) {
        return zendesk.getCategoryTranslations(category.getId());
    }
    public Iterable<Translation> getTranslations(Section section) {
        return zendesk.getSectionTranslations(section.getId());
    }

    public Long getPermissionGroupId(String permissionGroupTitle) {
        Optional<PermissionGroup> groupOpt = StreamSupport.stream(zendesk.getPermissionGroups().spliterator(), true)
             .filter(pg -> pg.getName().equalsIgnoreCase(permissionGroupTitle))
             .findFirst();
        if (groupOpt.isPresent())
            return groupOpt.get().getId();
        else
            return null;
    }

    private void apiRateLimitWait(long seconds) {
        log.info("Zendesk API rate limit reached; waiting for {} seconds to continue", seconds);
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
