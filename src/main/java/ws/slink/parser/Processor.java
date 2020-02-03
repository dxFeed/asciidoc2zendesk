package ws.slink.parser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ws.slink.config.AppConfig;
import ws.slink.model.ProcessingResult;
import ws.slink.zendesk.ZendeskFacade;
import ws.slink.zendesk.ZendeskHierarchy;
import ws.slink.zendesk.ZendeskTools;

import java.time.Instant;

import static ws.slink.model.ProcessingResult.ResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Processor {

    private final @NonNull AppConfig appConfig;
    private final @NonNull DirectoryProcessor directoryProcessor;
    private final @NonNull ZendeskTools zendeskTools;
    private final @NonNull ZendeskFacade zendeskFacade;

    public String process() {
        long timeA = Instant.now().toEpochMilli();
        ProcessingResult result = new ProcessingResult();
        if (appConfig.deleteAll()) {
            zendeskFacade.getArticles().stream().forEach(a -> {
                if (zendeskFacade.removeArticle(a)) {
                    result.add(RT_DEL_SUCCESS);
                    log.info("removed article '{}' from ZenDesk server", a.getTitle());
                } else {
                    result.add(RT_DEL_FAILURE);
                    log.warn("could not remove article '{}' from ZenDesk server", a.getTitle());
                }
            });
        } else {
            if (StringUtils.isNotBlank(appConfig.dir()))
                result.merge(directoryProcessor.process(appConfig.dir(), new ZendeskHierarchy()));
        }
        long timeB = Instant.now().toEpochMilli();
        return new StringBuilder()
            .append("-------------------------------------------------------------").append("\n")
            .append("total time taken       : " + DurationFormatUtils.formatDuration( timeB - timeA, "HH:mm:ss")).append("\n")
            .append("successfully published : " + result.get(RT_PUB_SUCCESS).get()).append("\n")
            .append("publishing errors      : " + result.get(RT_PUB_FAILURE).get()).append("\n")
            .append("successfully removed   : " + result.get(RT_DEL_SUCCESS).get()).append("\n")
            .append("removal errors         : " + result.get(RT_DEL_FAILURE).get()).append("\n")
            .append("hidden documents       : " + result.get(RT_FILE_SKIPPED).get()).append("\n")
            .append("skipped directories    : " + result.get(RT_DIR_SKIPPED).get()).append("\n")
            .append("printed documents      : " + result.get(RT_FILE_PRINTED).get()).append("\n")
            .toString();
    }
}
