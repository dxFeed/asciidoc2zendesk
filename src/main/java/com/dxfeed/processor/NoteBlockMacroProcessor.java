package com.dxfeed.processor;

import org.apache.commons.lang.StringUtils;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;

import java.util.Arrays;
import java.util.Map;

@Name ("note")
public class NoteBlockMacroProcessor extends BlockMacroProcessor {

    @Override
    public Object process(StructuralNode parent, String noteContent, Map<String, Object> attributes) {

        String style = "";

        if (attributes.containsKey("type"))
            style = attributes.get("type").toString();

        String content =
          "<div class=\"paragraph\">\n"
        + "  <div class=\"content note "
        + (StringUtils.isNotBlank(style) ? style : "")
        + "\">\n"
        + noteContent
        + "  </div>\n"
        + "</div>\n"
        ;
        return createBlock(parent, "pass", Arrays.asList(content), attributes);
    }
}
