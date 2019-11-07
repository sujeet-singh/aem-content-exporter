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

@Component(service = ContentExporterService.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Content Exporter Service",
        })
@Designate(ocd = ContentExporterServiceConfig.class)
public class ContentExporterServiceImpl implements ContentExporterService {
    private static final String PAGE_PROPERTIES = "pageProperties";
    private static final String CONTENT = "content";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

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
            ((ObjectNode) rootNode).set(PAGE_PROPERTIES, createPageJson(mapper, page));

            // Creating Page Content Json
//            ((ObjectNode) rootNode).set(CONTENT, createPageContent(mapper, nodeList, path));

            return mapper.writeValueAsString(rootNode);
        } catch (RepositoryException | JsonProcessingException e) {
            logger.error("Exception Caused while parsing Json: {}", e.getMessage());
        }

        return null;
    }

    private JsonNode createPageJson(ObjectMapper mapper, Page page) throws RepositoryException {
        Node jcrContentNode = page.getContentResource().adaptTo(Node.class);
        JsonNode propertyJsonObject = createPropertyJsonObject(mapper, jcrContentNode);
        return propertyJsonObject;
    }

    private JsonNode createPageContent(ObjectMapper mapper, List<Node> nodeList, String path) throws RepositoryException {
        JsonNode propertyJsonObject = mapper.createObjectNode();
        for (Node node : nodeList) {
            int depth = getDepth(path, node.getPath());
            ((ObjectNode) propertyJsonObject).put(node.getName(), createPropertyJsonObject(mapper, node));
        }

        return propertyJsonObject;
    }

    private int getDepth(String rootPath, String path) {
        return path.replace(rootPath, "").split("/").length;
    }

    private JsonNode createPropertyJsonObject(ObjectMapper mapper, Node node) throws RepositoryException {
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

    private List<Node> createNodeList(Session session, String path) {
        String queryString = "SELECT * FROM [nt:base] AS node WHERE ISDESCENDANTNODE(node, \"" + path + "\")";
        List<Node> nodeList = new ArrayList<>();
        NodeIterator nodeIterator = executeQuery(session, queryString);
        while (nodeIterator.hasNext()) {
            nodeList.add(nodeIterator.nextNode());
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
}
