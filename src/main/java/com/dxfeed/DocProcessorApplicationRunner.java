package com.dxfeed;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.zendesk.client.v2.model.hc.Category;
import org.zendesk.client.v2.model.hc.Section;
import com.dxfeed.config.AppConfig;
import com.dxfeed.parser.FileProcessor;
import com.dxfeed.parser.Processor;
import com.dxfeed.zendesk.ZendeskFacade;
import com.dxfeed.zendesk.ZendeskHierarchy;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@DependsOn({"commandLineArguments"})
public class DocProcessorApplicationRunner implements CommandLineRunner, ApplicationContextAware {

    private final @NonNull AppConfig appConfig;
    private final @NonNull Processor processor;
    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull ZendeskFacade zendeskFacade;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run(String... args) {
        int exitCode = 0;

        if (StringUtils.isNotBlank(appConfig.test())) {
            fileProcessor
                .read(appConfig.test(), new ZendeskHierarchy().category(new Category()).section(new Section()))
                .map(fileProcessor::convert)
                .ifPresent(System.out::println)
            ;
        } else {
            if (!zendeskFacade.initialized()) {
                log.error("Zendesk client not initialized");
                exitCode = 2;
            } else {
                if (!checkConfiguration()) {
                    printUsage();
                    exitCode = 1;
                } else {
                    System.out.println(processor.process());
                }
            }
        }


        // close up
        applicationContext.close();
        System.exit(exitCode);
    }

    private boolean checkConfiguration() {
        return !( StringUtils.isNotBlank(appConfig.url()) && (StringUtils.isBlank(appConfig.user()) || StringUtils.isBlank(appConfig.token()))
        || StringUtils.isNotBlank(appConfig.user()) && (StringUtils.isBlank(appConfig.url()) || StringUtils.isBlank(appConfig.token()))
        || StringUtils.isNotBlank(appConfig.token()) && (StringUtils.isBlank(appConfig.user()) || StringUtils.isBlank(appConfig.url()))
        || (StringUtils.isBlank(appConfig.dir()) && StringUtils.isBlank(appConfig.file()) && !appConfig.deleteAll())
        )
        ;
    }

    public void printUsage() {
        System.out.println("Usage: ");
        System.out.println("  java -jar asciidoc2zendesk.jar --dir=<path/to/directory> --url=<zendesk url> --user=<login> --token=<token> [--profile=<propfile name>]");
        System.out.println("\t--dir\t\t\tDirectory to process asciidoc files recursively");
        System.out.println("\t--url\t\t\tZendesk server URL (e.g. http://test.zendesk.com)");
        System.out.println("\t--user\t\t\tZendesk user with publish rights");
        System.out.println("\t--token\t\t\tZendesk access token");
        System.out.println("\t--profile\t\tNon-standard profile to use during documents upload (profile configuration will be read from .properties.<profile> file in each directory)");
        System.out.println("\t--clean\t\t\tRemove articles from ZenDesk server, which are not exist in local repository");
        System.out.println("\t--delete-all\tRemove all articles from ZenDesk server");
        System.exit(1);
    }

}
