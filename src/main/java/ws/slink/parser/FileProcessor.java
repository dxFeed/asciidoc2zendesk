package ws.slink.parser;

import ch.qos.logback.core.encoder.EchoEncoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zendesk.client.v2.model.hc.Article;
import ws.slink.config.AppConfig;
import ws.slink.model.Document;
import ws.slink.model.ProcessingResult;
import ws.slink.processor.*;
import ws.slink.tools.FileTools;
import ws.slink.zendesk.ZendeskFacade;
import ws.slink.zendesk.ZendeskHierarchy;
import ws.slink.zendesk.ZendeskTools;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ws.slink.model.ProcessingResult.ResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileProcessor {

    @Value("${asciidoc.template.title}")
    private String titleTemplate;

    @Value("${asciidoc.template.title-old}")
    private String titleOldTemplate;

    @Value("${asciidoc.template.position}")
    private String positionTemplate;

    @Value("${asciidoc.template.draft}")
    private String draftTemplate;

    @Value("${asciidoc.template.promoted}")
    private String promotedTemplate;

    @Value("${asciidoc.template.tags}")
    private String tagsTemplate;

    @Value("${zendesk.publish}")
    private boolean performPublication;

    private final @NonNull AppConfig appConfig;
    private final @NonNull ZendeskTools zendeskTools;
    private final @NonNull ZendeskFacade zendeskFacade;
    private final @NonNull FileTools fileTools;

    @SuppressWarnings("unchecked")
    private void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

    @PostConstruct
    private void init() {
        disableAccessWarnings();
    }

    private Asciidoctor initializeAsciidoctor() {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        // register preprocessors
        asciidoctor.javaExtensionRegistry().preprocessor(VideoMacroPreProcessor.class);
        asciidoctor.javaExtensionRegistry().preprocessor(ZendeskLinkMacroPreProcessor.class);

        // register block processors
        // ...

        // register (block) macro processors
        asciidoctor.javaExtensionRegistry().blockMacro(VideoBlockMacroProcessor.class);

        // register inline macro processors
        asciidoctor.javaExtensionRegistry().inlineMacro(new ZendeskLinkInlineMacroProcessor(zendeskFacade));

        // register postprocessors
        asciidoctor.javaExtensionRegistry().postprocessor(CodeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(ImageBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(TableBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(AdmonitionBlockPostProcessor.class);

        return asciidoctor;
    }

    public ProcessingResult process(String inputFilename, ZendeskHierarchy hierarchy) {
        log.info(">> start file processing: '{}'", inputFilename);
        ProcessingResult result = new ProcessingResult();
        if (null == hierarchy.category() || null == hierarchy.section()) {
            File file = new File(inputFilename);
            String sectionDir = null, categoryDir = null;
            try {
                sectionDir  = file.getParent();
                categoryDir = new File(file.getParent()).getParent();
            } catch (Exception e) {
                log.warn("could not get parent directories for input file '{}'", inputFilename);
            }
            if (!zendeskTools.updateHierarchy(hierarchy, fileTools.readProperties(categoryDir))) {
                log.warn("could not load category data");
                result.add(RT_DIR_SKIPPED);
            }
            else if (!zendeskTools.updateHierarchy(hierarchy, fileTools.readProperties(sectionDir))) {
                log.warn("could not load section data");
                result.add(RT_DIR_SKIPPED);
            }
        }
        if (StringUtils.isNotBlank(inputFilename) && result.get(RT_DIR_SKIPPED).get() == 0) {
            read(inputFilename, hierarchy).ifPresent(d -> convert(d).ifPresent(cd -> result.merge(publishOrPrint(d, cd, hierarchy))));
        }
        return result;
    }

    public Optional<Document> read(String inputFilename, ZendeskHierarchy hierarchy) {
        List<String> lines;
        try {
            lines = FileUtils.readLines(new File(inputFilename), "utf-8");
        } catch (IOException e) {
            log.error("error reading file: {}", e.getMessage());
            return Optional.empty();
        }

        try {
            Document document =
                new Document()
                    .inputFilename(inputFilename)
                    .category(hierarchy.category().getName())
                    .section(hierarchy.section().getName())
                    .title(getDocumentParam(lines, titleTemplate, null))
                    .oldTitle(getDocumentParam(lines, titleOldTemplate, null))
                    .position(getDocumentIntParam(lines, positionTemplate, Integer.MAX_VALUE))
                    .draft(getDocumentBooleanParam(lines, draftTemplate))
                    .promoted(getDocumentBooleanParam(lines, promotedTemplate))
                    .contents(lines.stream().collect(Collectors.joining("\n")))
                    .tags(
                        Arrays.asList(getDocumentParam(lines, tagsTemplate, null).split(","))
                            .stream()
                            .filter(s -> StringUtils.isNotBlank(s))
                            .map(s -> s.trim())
                            .collect(Collectors.toList())
                    );

            if (!FilenameUtils.getBaseName(inputFilename)
                    .replaceAll(" ", "_")
                    .equalsIgnoreCase(document.title().replaceAll(" ", "_")))
                log.warn("document title does not match with file name: '{}' - '{}'",
                    document.title(),
                    FilenameUtils.getName(inputFilename));

            return Optional.of(document);

        } catch (Exception e) {
            log.warn("error reading document: {}", e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<String> convert(Document document) {
        Asciidoctor asciidoctor = initializeAsciidoctor(/*document*/);
        try {
            String result = asciidoctor
            .convertFile(
                new File(document.inputFilename()),
                OptionsBuilder.options()
                    .backend("xhtml5")
                    .toFile(false)
                    .safe(SafeMode.UNSAFE)
            );
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.warn("error converting file: {}", e.getMessage());
            return Optional.empty();
        } finally {
            asciidoctor.shutdown();
        }
    }

    public ProcessingResult publishOrPrint(Document document, String convertedDocument, ZendeskHierarchy hierarchy) {

        if (performPublication) { // publish document
            // for renaming support we need to query existing articles either with document's 'title' or 'oldTitle'
            String requestTitle = StringUtils.isBlank(document.oldTitle()) ? document.title() : document.oldTitle();

            Optional<Article> requestedArticle = zendeskFacade.getArticle(hierarchy.section(), requestTitle);

            // if we're trying to rename already renamed document (forgot to clean OLD-TITLE tag)
            if (!requestedArticle.isPresent()) {
                requestedArticle = zendeskFacade.getArticle(hierarchy.section(), document.title());
            }

            Optional<Article> newArticle;
            if (requestedArticle.isPresent()) {
                log.trace("updating existing article '{}'", requestedArticle.get().getTitle());
                newArticle = zendeskTools.updateArticle(requestedArticle.get(), document, convertedDocument);
            } else {
                log.trace("creating new article '{}'", requestTitle);
                newArticle = zendeskTools.createArticle(document, hierarchy.section(), convertedDocument, appConfig.group());
            }

            if (!newArticle.isPresent()) {
                log.warn("could not create or update article '{}'", requestTitle);
                return new ProcessingResult(RT_PUB_FAILURE);
            } else {
                Optional<Article> processedArticle;
                if (requestedArticle.isPresent()) {
                    log.trace("updating existing article in zendesk '{}'", newArticle.get().getTitle());
                    processedArticle = zendeskFacade.updateArticle(newArticle.get());
                } else {
                    log.trace("creating new article in zendesk '{}'", newArticle.get().getTitle());
                    processedArticle = zendeskFacade.addArticle(newArticle.get());
                }
                if (!processedArticle.isPresent()) {
                    log.warn("could not create or update article '{}' on zendesk server", newArticle.get().getTitle());
                    return new ProcessingResult(RT_PUB_FAILURE);
                }
            }
            if (document.draft())
                return new ProcessingResult(/*RT_PUB_SUCCESS*/).add(RT_PUB_DRAFT);
            else
                return new ProcessingResult(RT_PUB_SUCCESS);
        } else { // just print document to stdout
            System.out.println("-------------------------------------------------------------------------------------");
            document.print("    ");
            System.out.println(convertedDocument);
            return new ProcessingResult(RT_FILE_PRINTED);
        }
    }

    private String getDocumentParam(List<String> lines, String key, String override) {
        return (StringUtils.isNotBlank(override))
            ? override
            : lines
            .stream()
            .filter(s -> s.contains(key))
            .findFirst()
            .orElse("")
            .replace(key, "")
            .replace("/", "")
            .trim()
        ;
    }
    private boolean getDocumentBooleanParam(List<String> lines, String key) {
        Optional<String> argument =
            lines.stream()
                .filter(s -> s.startsWith("//") && s.contains(key))
                .findFirst();
        if (!argument.isPresent()) {
            return false;
        } else {
            String value = argument.get()
                .replaceAll("/", "")
                .replace(key, "")
                .trim();
            log.trace("boolean value for {} is '{}'", key, value);
            return (StringUtils.isBlank(value)) ? true : Boolean.parseBoolean(value);
        }
    }
    private int getDocumentIntParam(List<String> lines, String key, int defaultValue) {
        Optional<String> argument =
                lines.stream()
                        .filter(s -> s.startsWith("//") && s.contains(key))
                        .findFirst();
        if (!argument.isPresent()) {
            return defaultValue;
        } else {
            String value = argument.get()
                    .replaceAll("/", "")
                    .replace(key, "")
                    .trim();
            log.trace("integer value for {} is '{}'", key, value);
            try {
                return Integer.valueOf(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }
}
