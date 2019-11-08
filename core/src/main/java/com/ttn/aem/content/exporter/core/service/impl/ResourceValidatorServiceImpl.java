package com.ttn.aem.content.exporter.core.service.impl;

import com.day.cq.wcm.api.Page;
import com.ttn.aem.content.exporter.core.service.ResourceValidatorService;
import com.ttn.aem.content.exporter.core.service.config.ResourceValidatorServiceConfig;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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

    private Map<String, List<String>> compPropMap;

    @Activate
    @Modified
    public void activate(ResourceValidatorServiceConfig config) {
        this.serviceConfig = config;
        List<String> excludedCompProps = Arrays.asList(serviceConfig.excludedComponentProperties());
        compPropMap = excludedCompProps.stream()
                .map(compProp -> compProp.split("="))
                .collect(Collectors.toMap(a -> a[0],
                        a -> (a.length > 1) ? Arrays.stream(a[1].split(",")).collect(Collectors.toList()) : Collections.emptyList()));
    }

    @Deactivate
    public void deactivate() {
        this.compPropMap = null;
    }

    @Override
    public boolean isValid(Page page) {
        if (serviceConfig.serviceDisabled()) {
            return true;
        }
        return !Arrays.asList(serviceConfig.excludedPages()).contains(page.getPath()) && page.isValid();
    }

    @Override
    public boolean isValid(Resource resource) {
        if (serviceConfig.serviceDisabled()) {
            return true;
        }
        return !Arrays.asList(serviceConfig.excludedComponents()).contains(resource.getResourceType());
    }

    @Override
    public boolean isValid(String propertyName, Resource resource) {
        if (serviceConfig.serviceDisabled()) {
            return true;
        }
        boolean isExcludedInGenericPropertySet;
        if (Objects.nonNull(resource) && compPropMap != null && compPropMap.containsKey(resource.getResourceType())) {
            List<String> props = compPropMap.get(resource.getResourceType());
            isExcludedInGenericPropertySet = props.contains(propertyName);
            if (!isExcludedInGenericPropertySet) {
                isExcludedInGenericPropertySet = Arrays.asList(serviceConfig.excludedProperties()).stream().
                        map(prop -> Pattern.compile(prop)).
                        anyMatch(pattern -> pattern.matcher(propertyName).matches());
            }
        } else {
            isExcludedInGenericPropertySet = Arrays.asList(serviceConfig.excludedProperties()).stream().
                    map(prop -> Pattern.compile(prop)).
                    anyMatch(pattern -> pattern.matcher(propertyName).matches());
        }
        return BooleanUtils.isFalse(isExcludedInGenericPropertySet);
    }

    @Override
    public boolean isContainer(Resource resource) {
        List<String> excludedContainerCompProps = Arrays.asList(serviceConfig.excludedContainerComponents());
        Map<String, List<String>> containerCompPropMap = excludedContainerCompProps.stream()
                .map(compProp -> compProp.split("="))
                .collect(Collectors.toMap(a -> a[0],
                        a -> (a.length > 1) ? Arrays.stream(a[1].split(",")).collect(Collectors.toList()) : Collections.emptyList()));
        return Objects.nonNull(resource) && containerCompPropMap.containsKey(resource.getResourceType());
    }

    @Override
    public boolean mergeContainer(Resource resource) {
        List<String> excludedContainerCompProps = Arrays.asList(serviceConfig.excludedContainerComponents());
        Map<String, List<String>> containerCompPropMap = excludedContainerCompProps.stream()
                .map(compProp -> compProp.split("="))
                .collect(Collectors.toMap(a -> a[0],
                        a -> (a.length > 1) ? Arrays.stream(a[1].split(",")).collect(Collectors.toList()) : Collections.emptyList()));
        if (Objects.nonNull(resource) && containerCompPropMap.containsKey(resource.getResourceType())) {
            List<String> props = containerCompPropMap.get(resource.getResourceType());
            for (String propertyName : props) {
                if (resource.getValueMap().containsKey(propertyName)) {
                    return false;
                }
            }
        }
        return true;
    }
}
