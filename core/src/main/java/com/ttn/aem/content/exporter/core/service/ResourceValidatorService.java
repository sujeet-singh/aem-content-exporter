package com.ttn.aem.content.exporter.core.service;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;

public interface ResourceValidatorService {

    boolean isValid(Page page);

    boolean isValid(Resource resource);

    boolean isValid(String propertyName, Resource resource);

    boolean isContainer(Resource resource);

    boolean mergeContainer(Resource resource);
}
