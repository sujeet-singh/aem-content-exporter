package com.ttn.aem.content.exporter.core.service.impl;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ttn.aem.content.exporter.core.service.ContentExporterService;
import com.ttn.aem.content.exporter.core.service.config.ContentExporterServiceConfig;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

@Component(service = ContentExporterService.class, immediate = true,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Content Exporter Service",
        })
@Designate(ocd = ContentExporterServiceConfig.class)
public class ContentExporterServiceImpl implements ContentExporterService {
    private static final String PAGE_PROPERTIES = "pageProperties";
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper mapper = new ObjectMapper();
    private JsonNode rootNode = mapper.createObjectNode();
    private Session session;
    private List<Node> nodeList = new ArrayList<>();

    @Override
    public String composeJson(ResourceResolver resolver, Page page) {
        logger.info("Inside {}", this.getClass().getName());
        session = resolver.adaptTo(Session.class);
        String path = page.getContentResource().getPath();
        //Creating Nodes list for Json processing
        createNodeList(path);

        try {
            createPageJson(page);
            return mapper.writeValueAsString(rootNode);
        } catch (RepositoryException | JsonProcessingException e) {
            logger.error("Exception Caused while parsing Json: {}", e.getMessage());
        }

        return null;
    }

    private void createPageJson(Page page) throws RepositoryException {
        Node jcrContentNode = page.getContentResource().adaptTo(Node.class);
        JsonNode propertyJsonObject = createPropertyJsonObject(jcrContentNode);
        ((ObjectNode) rootNode).set(PAGE_PROPERTIES, propertyJsonObject);
    }

    private JsonNode createPropertyJsonObject(Node node) throws RepositoryException {
        JsonNode jsonProperties = mapper.createObjectNode();
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            ArrayNode arrayNode = mapper.createArrayNode();
            if (property.isMultiple()) {
                for (Value value : property.getValues()) {
                    switch (property.getType()) {
                        case PropertyType.BOOLEAN: {
                            arrayNode.add(value.getBoolean());
                            break;
                        }
                        case PropertyType.STRING: {
                            arrayNode.add(value.getString());
                            break;
                        }
                        case PropertyType.DATE: {
                            arrayNode.add(value.getDate().toString());
                            break;
                        }
                        case PropertyType.LONG: {
                            arrayNode.add(value.getLong());
                            break;
                        }
                        default: {
                            arrayNode.add(value.getString());
                        }
                    }
                    ((ObjectNode) jsonProperties).put(property.getName(), arrayNode);
                }
            } else {
                switch (property.getType()) {
                    case PropertyType.BOOLEAN: {
                        ((ObjectNode) jsonProperties).put(property.getName(), property.getValue().getBoolean());
                        break;
                    }
                    case PropertyType.STRING: {
                        ((ObjectNode) jsonProperties).put(property.getName(), property.getValue().getString());
                        break;
                    }
                    case PropertyType.DATE: {
                        ((ObjectNode) jsonProperties).put(property.getName(), property.getValue().getDate().toString());
                        break;
                    }
                    case PropertyType.LONG: {
                        ((ObjectNode) jsonProperties).put(property.getName(), property.getValue().getLong());
                        break;
                    }
                    default: {
                        ((ObjectNode) jsonProperties).put(property.getName(), property.getValue().getString());
                    }
                }
            }
        }
        return jsonProperties;
    }

    private void createNodeList(String path) {
        String queryString = "SELECT * FROM [nt:base] AS node WHERE ISDESCENDANTNODE(node, \"" + path + "\")";
        NodeIterator nodeIterator = executeQuery(session, queryString);
        while (nodeIterator.hasNext()) {
            nodeList.add(nodeIterator.nextNode());
        }
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
}
