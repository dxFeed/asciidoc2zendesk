package com.dxfeed.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CodeBlockPostProcessor extends Postprocessor {

    private static final String  CODE_START      = "(.*)(<code .*>)(.*)";
    private static final String  CODE_END        = "(.*)(</code>)(.*)";
    private static final Pattern START_PATTERN   = Pattern.compile(CODE_START, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern END_PATTERN     = Pattern.compile(CODE_END, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public String process(Document document, String convertedDocument) {
        ThreadLocal<Boolean> inCodeBlock = ThreadLocal.withInitial(() -> false);
        return Arrays.stream(convertedDocument.split("\n"))
            .map(s -> this.processString(s, inCodeBlock))
            .collect(Collectors.joining("\n"));
    }

    private String processString(String string, ThreadLocal<Boolean> inCodeBlock) {
        String input = string;
        Matcher matcherA = START_PATTERN.matcher(input);
        if (matcherA.matches()) {
//            System.err.println("A) " + input);
            input = matcherA.group(1) + matcherA.group(3);
        }
        Matcher matcherB = END_PATTERN.matcher(input);
        if (matcherB.matches()) {
//            System.err.println("B) " + input);
            input = matcherB.group(1) + matcherB.group(3);
        }
        return input;
    }

}
