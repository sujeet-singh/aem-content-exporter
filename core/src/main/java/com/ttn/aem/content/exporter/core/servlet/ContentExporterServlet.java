package com.ttn.aem.content.exporter.core.servlet;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.ttn.aem.content.exporter.core.service.ContentExporterService;
import com.ttn.aem.content.exporter.core.service.ResourceValidatorService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Objects;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=sling/servlet/default",
                "sling.servlet.methods=GET",
                "sling.servlet.extensions=json",
                "sling.servlet.selectors=composer"
        }

)
public class ContentExporterServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ContentExporterServlet.class);

    @Reference
    private ContentExporterService contentExporterService;
    @Reference
    private ResourceValidatorService resourceValidatorService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        LOG.info("Inside {}", this.getClass().getName());

        ResourceResolver resourceResolver = request.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        if (Objects.nonNull(page) && resourceValidatorService.isValid(page)) {
            String composedJson = contentExporterService.composeJson(resourceResolver, page);
            response.getWriter().write(composedJson);
        } else {
            response.getWriter().write("Invalid Path");
        }

    }
}
