package com.ttn.aem.content.exporter.core.service.impl;

import com.day.cq.wcm.api.Page;
import com.ttn.aem.content.exporter.core.service.ResourceValidatorService;
import com.ttn.aem.content.exporter.core.service.config.ResourceValidatorServiceConfig;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component(service = ResourceValidatorService.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=AEM Content Exporter: Resource Validator Service",
        })
@Designate(ocd = ResourceValidatorServiceConfig.class)
public class ResourceValidatorServiceImpl implements ResourceValidatorService {

    private ResourceValidatorServiceConfig serviceConfig;

    @Activate
    public void activate(ResourceValidatorServiceConfig config){
        this.serviceConfig = config;
    }

    @Override
    public boolean isValid(Page page) {
        return !Arrays.asList(serviceConfig.excludedPages()).contains(page.getPath()) && page.isValid();
    }

    @Override
    public boolean isValid(Resource resource) {
        return !Arrays.asList(serviceConfig.excludedComponents()).contains(resource.getResourceType());
    }

    @Override
    public boolean isValid(String propertyName, Resource resource) {
        List<String> excludedCompProps = Arrays.asList(serviceConfig.excludedComponentProperties());
        Map<String, List<String>> compPropMap = excludedCompProps.stream()
                .map(compProp -> compProp.split("="))
                .collect(Collectors.toMap(a-> a[0],
                        a -> (a.length > 1) ? Arrays.stream(a[1].split(",")).collect(Collectors.toList()) : Collections.emptyList()));
        boolean isExcludedInGenericPropertySet;
        if( Objects.nonNull(resource) && compPropMap.containsKey(resource.getResourceType())){
            List<String> props = compPropMap.get(resource.getResourceType());
            isExcludedInGenericPropertySet = props.contains(propertyName);
        } else {
            isExcludedInGenericPropertySet = Arrays.asList(serviceConfig.excludedProperties()).stream().
                    map(prop -> Pattern.compile(prop)).
                    anyMatch(pattern -> pattern.matcher(propertyName).matches());
        }
        return BooleanUtils.isFalse(isExcludedInGenericPropertySet);
    }
}
