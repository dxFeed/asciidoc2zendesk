package ws.slink.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * accepts following configuration parameters from any spring-supported configuration source
 * (properties file, command line arguments, environment variables):
 *
 *   a2z.dir     - input directory for processing
 *   a2z.profile - configuration file suffix
 *   a2z.url     - zendesk server URL
 *   a2z.user    - zendesk server user
 *   a2z.token   - zendesk server password
 *
 */

@Data
@Component
@ConfigurationProperties(prefix = "a2z")
@Accessors(fluent = true)
public class AppConfig {

    private String dir;
    private String url;
    private String user;
    private String token;
    private String profile; // configuration file suffix (.properties.profile)
    private boolean clean;
    private boolean deleteAll;

    public void print() {
        System.out.println("input directory: " + dir);
        System.out.println("zendesk url    : " + url);
        System.out.println("zendesk user   : " + user);
        System.out.println("zendesk token  : " + token);
        System.out.println("clean flag     : " + clean);
        System.out.println("delete all flag: " + deleteAll);
        System.out.println("config profile : " + profile);
        System.out.println("config file    : " + getConfigFileName());
    }

    public String getConfigFileName() {
        return StringUtils.isBlank(profile) ? ".properties" : ".properties." + profile;
    }
}
