package com.dxfeed.zendesk;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.zendesk.client.v2.model.hc.Category;
import org.zendesk.client.v2.model.hc.Section;

@Slf4j
@Data
@Accessors(fluent = true)
public class ZendeskHierarchy {

    Category category;
    Section section;

}
