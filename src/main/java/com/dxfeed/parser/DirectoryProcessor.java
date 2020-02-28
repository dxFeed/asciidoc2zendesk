package com.dxfeed.parser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zendesk.client.v2.model.hc.Article;
import com.dxfeed.config.AppConfig;
import com.dxfeed.model.ProcessingResult;
import com.dxfeed.tools.FileTools;
import com.dxfeed.zendesk.ZendeskFacade;
import com.dxfeed.zendesk.ZendeskHierarchy;
import com.dxfeed.zendesk.ZendeskTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dxfeed.model.ProcessingResult.ResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor (onConstructor = @__(@Autowired))
public class DirectoryProcessor {

    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull AppConfig appConfig;
    private final @NonNull ZendeskTools zendeskTools;
    private final @NonNull ZendeskFacade zendeskFacade;
    private final @NonNull FileTools fileTools;

    public ProcessingResult process(String directoryPath, ZendeskHierarchy hierarchy) {
        log.info("> start directory processing: '{}'", directoryPath);
        ProcessingResult result = new ProcessingResult();

        if (!zendeskTools.updateHierarchy(hierarchy, fileTools.readProperties(directoryPath))) {
            log.warn("could not load zendesk hierarchy data");
            result.add(RT_DIR_SKIPPED);
        } else {
            result.merge(processAllFiles(directoryPath, hierarchy));
            if (appConfig.clean())
                result.merge(removeStaleArticles(directoryPath, hierarchy));
        }
        result.merge(processAllDirectories(directoryPath, hierarchy));

        return result;
    }

    private ProcessingResult removeStaleArticles(String directoryPath, ZendeskHierarchy hierarchy) {
        try {

            if (null == hierarchy.section() || null == hierarchy.category())
                return new ProcessingResult(RT_NONE);

            ProcessingResult result = new ProcessingResult();

            Map<String, Article> zendeskArticles = zendeskFacade.getArticles(hierarchy.section())
                .stream().collect(Collectors.toMap(Article::getTitle, Function.identity()));

            Set<String> repositoryArticles = Files.list(Paths.get(directoryPath))
                .map(Path::toFile).filter(File::isFile)
                .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
                .map(f -> f.getAbsolutePath())
                .map(f -> fileProcessor.read(f, hierarchy))
                .filter(od -> od.isPresent())
                .map(od -> od.get())
                .map(d -> d.title())
                .collect(Collectors.toSet())
            ;

            Set<String> titlesToRemove = new HashSet(zendeskArticles.keySet());
            titlesToRemove.removeAll(repositoryArticles);

            titlesToRemove.stream().forEach(t -> {
                Article article = zendeskArticles.get(t);
                log.info("removing stale article '{}' #({}) from zendesk", article.getTitle(), article.getId());
                result.add(zendeskFacade.removeArticle(article) ? RT_DEL_SUCCESS : RT_DEL_FAILURE);
            });

            return result;
        } catch (IOException e) {
            log.error("error processing files for removed articles in {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
            return new ProcessingResult(RT_DEL_FAILURE);
        }
    }

    private ProcessingResult processAllFiles(String directoryPath, ZendeskHierarchy hierarchy) {
        try {
            ProcessingResult result = new ProcessingResult();
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
                .map(f -> f.getAbsolutePath())
                .parallel()
                .forEach(f -> result.merge(fileProcessor.process(f, hierarchy)));
            return result;
        } catch (IOException e) {
            log.error("error processing files in {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
            return new ProcessingResult(RT_PUB_FAILURE);
        }
    }

    private ProcessingResult processAllDirectories(String directoryPath, ZendeskHierarchy hierarchy) {
        try {
            ProcessingResult result = new ProcessingResult();
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(f -> f.isDirectory())
                .map(File::toPath)
                .map(Path::toString)
                .parallel()
                .forEach(d -> result.merge(process(d, hierarchy)));
            return result;
        } catch (IOException e) {
            log.error("error processing directory {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
            return new ProcessingResult(RT_PUB_FAILURE);
        }
    }

}
