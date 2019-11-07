package com.ttn.aem.content.exporter.core.service;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ResourceResolver;

public interface ContentExporterService {

    public String composeJson(ResourceResolver resourceResolver, Page path);
}
