package com.dxfeed.processor;

import org.apache.commons.lang.StringUtils;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;

import java.util.Arrays;
import java.util.Map;

@Name ("zvideo")
public class VideoBlockMacroProcessor extends BlockMacroProcessor {

    @Override
    public Object process(StructuralNode parent, String videoId, Map<String, Object> attributes) {

        String service = attributes.get("1").toString();
        String w       = attributes.getOrDefault("width", "").toString();
        String h       = attributes.getOrDefault("height", "").toString();

        String link = "";
        if (service.equalsIgnoreCase("vimeo"))
            link = "https://player.vimeo.com/video/" + videoId;
        else if (service.equalsIgnoreCase("youtube"))
            link = "https://www.youtube.com/embed/" + videoId + "?rel=0";

        String width = "", height = "";
        if (StringUtils.isNotBlank(w))
            width = " width=\"" + w + "\" ";
        if (StringUtils.isNotBlank(h))
            height = " height=\"" + h + "\" ";

        String content =
          "<div class=\"videoblock\">\n"
        + "  <div class=\"content\">\n"
        + "    <iframe src=\"" + link + "\"" + width + height + "frameborder=\"0\" allowfullscreen=\"allowfullscreen\"></iframe>\n"
        + "  </div>\n"
        + "</div>\n"
        ;
        return createBlock(parent, "pass", Arrays.asList(content), attributes);
    }
}
