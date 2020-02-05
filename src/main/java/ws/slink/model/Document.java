package ws.slink.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;

import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class Document {

    private String   inputFilename;
    private String   category;
    private String   section;
    private String   title;
    private String   oldTitle;
    private long     position = 0;
    private boolean  draft    = false;
    private boolean  promoted = false;
    private String   contents;
    private List<String> tags;

    @Override
    public String toString() {
        return new StringBuilder()
            .append(title)
            .append(" (")
            .append(category)
            .append(", ")
            .append(section)
            .append(") ")
            .append(inputFilename)
            .toString();
    }

    public boolean canPublish() {
        return StringUtils.isNotBlank(category)
            && StringUtils.isNotBlank(section)
            && StringUtils.isNotBlank(title)
        ;
    }

    public void print(String prefix) {
        System.out.println(prefix + "input filename: " + inputFilename);
        System.out.println(prefix + "category      : " + category);
        System.out.println(prefix + "section       : " + section);
        System.out.println(prefix + "title         : " + title);
        System.out.println(prefix + "old title     : " + oldTitle);
        System.out.println(prefix + "position      : " + position);
        System.out.println(prefix + "draft         : " + draft);
        System.out.println(prefix + "promoted      : " + promoted);
        System.out.println(prefix + "tags          : " + tags);
    }
}
