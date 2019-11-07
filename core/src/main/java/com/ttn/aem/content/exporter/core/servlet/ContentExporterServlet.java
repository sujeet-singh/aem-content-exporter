package com.ttn.aem.content.exporter.core.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

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

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        LOG.info("Inside {}", this.getClass().getName());
    }
}
