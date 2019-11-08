package com.ttn.aem.content.exporter.core.service.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "AEM Content Exporter: Resource Validator Configuration")
public @interface ResourceValidatorServiceConfig {

    @AttributeDefinition(name = "Excluded Pages", description = "Pages configured here will ne blocked by content exporter service. For ex. '/content/my-project/us/en/homepage'")
    String[] excludedPages();

    @AttributeDefinition(name = "Excluded Components", description = "Component Resource Types configured here will be blocked by content exporter service in whichever page they appear. For ex. 'my-project/components/content/user-data'")
    String[] excludedComponents();

    @AttributeDefinition(name = "Excluded Properties", description = "Properties configured here will be blocked by content exporter service in whichever component they appear. One can also add regex pattern. For ex. 'emailId' or 'jcr:.*'")
    String[] excludedProperties();

    @AttributeDefinition(name = "Excluded Properties in declared components", description = "Properties configured here will be blocked by content exporter service in configured component. Define properties for one component at a time. For ex. 'my-project/components/content/profile=emailId,password'")
    String[] excludedComponentProperties();

}
