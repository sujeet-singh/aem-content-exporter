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

    private Map<String, List<String>> containerCompPropMap;

    @Activate
    @Modified
    public void activate(ResourceValidatorServiceConfig config) {
        this.serviceConfig = config;
        List<String> excludedCompProps = Arrays.asList(serviceConfig.excludedComponentProperties());
        compPropMap = excludedCompProps.stream()
                .map(compProp -> compProp.split("="))
                .collect(Collectors.toMap(a -> a[0],
                        a -> (a.length > 1) ? Arrays.stream(a[1].split(",")).collect(Collectors.toList()) : Collections.emptyList()));

        List<String> excludedContainerCompProps = Arrays.asList(serviceConfig.excludedContainerComponents());
        containerCompPropMap = excludedContainerCompProps.stream()
                .map(compProp -> compProp.split("="))
                .collect(Collectors.toMap(a -> a[0],
                        a -> (a.length > 1) ? Arrays.stream(a[1].split(",")).collect(Collectors.toList()) : Collections.emptyList()));
    }

    @Deactivate
    public void deactivate() {
        this.compPropMap = null;
        this.containerCompPropMap = null;
    }

    @Override
    public boolean isValid(Page page) {
        return serviceConfig.serviceDisabled()
                || BooleanUtils.isFalse(Arrays.asList(serviceConfig.excludedPages()).contains(page.getPath()) && page.isValid());
    }

    @Override
    public boolean isValid(Resource resource) {
        return serviceConfig.serviceDisabled()
                || BooleanUtils.isFalse(Arrays.asList(serviceConfig.excludedComponents()).contains(resource.getResourceType()));
    }

    @Override
    public boolean isValid(String propertyName, Resource resource) {
        if (serviceConfig.serviceDisabled()) {
            return Boolean.TRUE;
        }
        if (Objects.nonNull(resource) && Objects.nonNull(compPropMap) && compPropMap.containsKey(resource.getResourceType())) {
            List<String> props = compPropMap.get(resource.getResourceType());
            boolean isValid = BooleanUtils.isFalse(props.contains(propertyName));
            return (isValid) ? isNotExcludedInGenericConfig(propertyName) : Boolean.FALSE;
        }
        return isNotExcludedInGenericConfig(propertyName);

    }

    private boolean isNotExcludedInGenericConfig(String propertyName) {
        return Arrays.stream(serviceConfig.excludedProperties())
                .map(Pattern::compile)
                .noneMatch(pattern -> pattern.matcher(propertyName).matches());
    }

    @Override
    public boolean isContainer(Resource resource) {
        return serviceConfig.serviceDisabled()
                || (Objects.nonNull(resource) && containerCompPropMap.containsKey(resource.getResourceType()));
    }

    @Override
    public boolean mergeContainer(Resource resource) {
        List<String> props = containerCompPropMap.get(resource.getResourceType());
        return serviceConfig.serviceDisabled()
                || Objects.isNull(resource)
                || !containerCompPropMap.containsKey(resource.getResourceType())
                || props.stream().noneMatch(prop -> resource.getValueMap().containsKey(prop));
    }
}
