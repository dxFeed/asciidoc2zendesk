package ws.slink;

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
import ws.slink.config.AppConfig;
import ws.slink.parser.Processor;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@DependsOn({"commandLineArguments"})
public class DocProcessorApplicationRunner implements CommandLineRunner, ApplicationContextAware {

    private final @NonNull AppConfig appConfig;
    private final @NonNull Processor processor;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run(String... args) {

        int exitCode = 0;

        if (!checkConfiguration()) {
            printUsage();
            exitCode = 1;
        } else {
            System.out.println(processor.process());
        }

        // close up
        applicationContext.close();
        System.exit(exitCode);

    }

    private boolean checkConfiguration() {
        return !( StringUtils.isNotBlank(appConfig.url()) && (StringUtils.isBlank(appConfig.user()) || StringUtils.isBlank(appConfig.token()))
        || StringUtils.isNotBlank(appConfig.user()) && (StringUtils.isBlank(appConfig.url()) || StringUtils.isBlank(appConfig.token()))
        || StringUtils.isNotBlank(appConfig.token()) && (StringUtils.isBlank(appConfig.user()) || StringUtils.isBlank(appConfig.url()))
        || (StringUtils.isBlank(appConfig.dir()) && !appConfig.deleteAll())
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



 // Мета-параметры документа
     // + :ZENDESK-CATEGORY:  // не надо, т.к. структурированный каталог - см. ".properties"
     // + :ZENDESK-SECTION:   // не надо, т.к. структурированный каталог - см. ".properties"
     // + :ZENDESK-TITLE:     // заголовок документа
     // + :ZENDESK-OLD-TITLE: // для переименования - если указан, то делаем "PUT" для документа с новым именем и старым ID
     // + :ZENDESK-ORDER:     // он же "position" - сортировка
     // + :ZENDESK-TAGS:      // CSV-список меток
     // + :ZENDESK-DRAFT:     //
     // + :ZENDESK-PROMOTED:  // true / false для публикации на главной странице
     // + :ZENDESK-HIDDEN:    // deny document publication

 // Параметры по-умолчанию
     // - user_segment_id       = null
     // + permission_group_id   = 'Agents and managers' - application.yml
     // + locale                = en-US                 - application.yml
     // + comments_disabled     = false
     // + notify_subscribers    = true
     // + body

 // readonly-параметры (сбросятся при перезаписи)
     // vote-sum
     // vote-count

 // удаляем всё, чего нет в репе (уже после публикации)

 // создаём, если такого документа нет, или редактируем, если он есть
 // добавить поддержку переименования (?)

 // --properties=... // для тестов можем всё писать в тестовую категорию

 // поддержка разных языков?

 // удаление -> архивация
 // - label_names
