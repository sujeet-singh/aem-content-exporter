package com.ttn.aem.content.exporter.core.service.impl;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ttn.aem.content.exporter.core.service.ContentExporterService;
import com.ttn.aem.content.exporter.core.service.ResourceValidatorService;
import com.ttn.aem.content.exporter.core.service.config.ContentExporterServiceConfig;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

@Component(service = ContentExporterService.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=AEM Content Exporter: Content Exporter Service",
        })
@Designate(ocd = ContentExporterServiceConfig.class)
public class ContentExporterServiceImpl implements ContentExporterService {

    private static final String PAGE_PROPERTIES = "pageProperties";
    private static final String DATE_FORMAT = "dd MMM yyyy, EEEE";
    private static final String CONTENT_STRUCTURE = "contentStructure";
    private static final String PATH_SEPARATOR = "/";
    private static final String COMPONENT_TYPE = "componentType";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceValidatorService resourceValidatorService;

    @Override
    public String composeJson(ResourceResolver resolver, Page page) {
        logger.info("Inside {}", this.getClass().getName());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.createObjectNode();
        Session session = resolver.adaptTo(Session.class);
        String path = page.getContentResource().getPath();

        //Creating Nodes list for Json processing
        List<Node> nodeList = createNodeList(session, path);

        try {
            // Creating Page Properties Json
            ((ObjectNode) rootNode).set(PAGE_PROPERTIES, createPageJson(resolver, mapper, page));

            // Creating Page Content Json
            ((ObjectNode) rootNode).set(CONTENT_STRUCTURE, createPageContent(resolver, mapper, nodeList, path));

            return mapper.writeValueAsString(rootNode);
        } catch (RepositoryException | JsonProcessingException e) {
            logger.error("Exception Caused while parsing Json: {}", e.getMessage());
        }

        return null;
    }

    private JsonNode createPageJson(ResourceResolver resolver, ObjectMapper mapper, Page page) throws RepositoryException {
        Node jcrContentNode = page.getContentResource().adaptTo(Node.class);
        return createPropertyJsonObject(resolver, page.getContentResource(), mapper, jcrContentNode);
    }

    private JsonNode createPageContent(ResourceResolver resolver, ObjectMapper mapper, List<Node> nodeList, String path) throws RepositoryException {
        ObjectNode propertyJsonObject = mapper.createObjectNode();
        String excludeComponentsParentPath = null;
        for (Node node : nodeList) {
            Resource resource = resolver.getResource(node.getPath());
            if (resourceValidatorService.isValid(resource) && StringUtils.isEmpty(excludeComponentsParentPath)) {
                if (BooleanUtils.isFalse(resourceValidatorService.mergeContainer(resource))) {
                    ((ObjectNode) getParentNode(propertyJsonObject, node, path)).put(node.getName(), createPropertyJsonObject(resolver, resource, mapper, node));
                }
            } else {
                excludeComponentsParentPath = StringUtils.isNotEmpty(excludeComponentsParentPath)
                        && !resource.getPath().contains(excludeComponentsParentPath) ? null : resource.getPath();
            }
        }
        return propertyJsonObject;
    }

    private String getType(Node node) throws RepositoryException {
        if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
            String resourceType = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();
            return resourceType.substring(resourceType.lastIndexOf(PATH_SEPARATOR) + 1);
        }
        return null;
    }

    private JsonNode getParentNode(ObjectNode propertyJsonObject, Node node, String path) throws RepositoryException {
        String searchPath = node.getParent().getPath().replace(path, "");
        String[] paths = searchPath.replaceFirst(PATH_SEPARATOR, "").split(PATH_SEPARATOR);
        StringBuilder fullPath = new StringBuilder();

        for (String key : paths) {
            String tempPath = fullPath + PATH_SEPARATOR + key;
            if (!propertyJsonObject.at(tempPath).isMissingNode()) {
                fullPath.append(PATH_SEPARATOR + key);
            }
        }

        return StringUtils.isEmpty(fullPath) ? propertyJsonObject : propertyJsonObject.at(fullPath.toString());
    }


    private JsonNode createPropertyJsonObject(ResourceResolver resolver, Resource resource, ObjectMapper mapper, Node node) throws RepositoryException {
        ObjectNode jsonProperties = mapper.createObjectNode();
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (resourceValidatorService.isValid(property.getName(), resource)) {
                setJsonProperty(mapper, jsonProperties, property);
                String componentType = getType(node);
                if (StringUtils.isNotEmpty(componentType))
                    jsonProperties.put(COMPONENT_TYPE, componentType);
            }
        }
        return jsonProperties;
    }

    private void setJsonProperty(ObjectMapper mapper, ObjectNode jsonProperties, Property property) throws RepositoryException {
        if (property.isMultiple()) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (Value value : property.getValues()) {
                switch (property.getType()) {
                    case PropertyType.BOOLEAN:
                        arrayNode.add(value.getBoolean());
                        break;
                    case PropertyType.DATE:
                        arrayNode.add(getFormattedDate(value.getDate().getTime(), DATE_FORMAT));
                        break;
                    case PropertyType.DECIMAL:
                        arrayNode.add(value.getDecimal());
                        break;
                    case PropertyType.DOUBLE:
                        arrayNode.add(value.getDouble());
                        break;
                    case PropertyType.LONG:
                        arrayNode.add(value.getLong());
                        break;
                    default:
                        arrayNode.add(value.getString());
                }
                jsonProperties.put(property.getName(), arrayNode);
            }
        } else {
            switch (property.getType()) {
                case PropertyType.BOOLEAN:
                    jsonProperties.put(property.getName(), property.getValue().getBoolean());
                    break;
                case PropertyType.DATE:
                    jsonProperties.put(property.getName(), getFormattedDate(property.getValue().getDate().getTime(), DATE_FORMAT));
                    break;
                case PropertyType.DECIMAL:
                    jsonProperties.put(property.getName(), property.getValue().getDecimal());
                    break;
                case PropertyType.DOUBLE:
                    jsonProperties.put(property.getName(), property.getValue().getDouble());
                    break;
                case PropertyType.LONG:
                    jsonProperties.put(property.getName(), property.getValue().getLong());
                    break;
                default:
                    jsonProperties.put(property.getName(), property.getValue().getString());
            }
        }
    }

    private List<Node> createNodeList(Session session, String path) {
        String queryString = "SELECT * FROM [nt:base] AS node WHERE ISDESCENDANTNODE(node, \"" + path + "\")";
        List<Node> nodeList = new ArrayList<>();
        NodeIterator nodeIterator = executeQuery(session, queryString);
        if (Objects.nonNull(nodeIterator)) {
            while (nodeIterator.hasNext()) {
                nodeList.add(nodeIterator.nextNode());
            }
        }
        return nodeList;
    }

    private NodeIterator executeQuery(Session session, String queryStr) {
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryStr, Query.JCR_SQL2);
            QueryResult result = query.execute();
            return result.getNodes();
        } catch (RepositoryException e) {
            logger.error("Query Exception: {}", e.getMessage());
        }
        return null;
    }

    private String getFormattedDate(Date date, String format) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(format);
        return dateFormatter.format(date);
    }
}
