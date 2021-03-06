/*
 * Copyright (C) 2013
 *
 * 52°North Initiative for Geospatial Open Source Software GmbH
 * Contact: Andreas Wytzisk
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.n52.sos.service.it;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann <c.autermann@52north.org>
 */
class MockHttpClient implements Client {
    private ServletContext context = null;
    private final Map<String, Set<String>> headers =
            new HashMap<String, Set<String>>(5);
    private final Map<String, Set<String>> query =
            new HashMap<String, Set<String>>(5);
    private final HttpServlet servlet;
    private String method = null;
    private String path = null;
    private String content = null;

    MockHttpClient(HttpServlet servlet, String method, String path) {
        this.servlet = servlet;
        this.method = method;
        this.path = path;
    }

    @Override
    public MockHttpClient header(String header, String value) {
        Set<String> set = headers.get(header);
        if (set == null) {
            headers.put(header, set = new HashSet<String>(1));
        }
        set.add(value);
        return this;
    }

    @Override
    public MockHttpClient contentType(String type) {
        return header("Content-Type", type);
    }

    @Override
    public MockHttpClient accept(String type) {
        return header("Accept", type);
    }

    @Override
    public MockHttpClient query(String key, String value) {
        Set<String> set = query.get(key);
        if (set == null) {
            query.put(key, set = new HashSet<String>(1));
        }
        set.add(value);
        return this;
    }

    @Override
    public MockHttpClient query(Enum<?> key, String value) {
        return query(key.name(), value);
    }

    @Override
    public MockHttpClient query(Enum<?> key, Enum<?> value) {
        return query(key.name(), value.name());
    }

    @Override
    public MockHttpClient query(String key, Enum<?> value) {
        return query(key, value.name());
    }

    @Override
    public MockHttpClient entity(String content) {
        this.content = content;
        return this;
    }

    private MockHttpServletRequest build() {
        try {
            final MockHttpServletRequest req =
                    new MockHttpServletRequest(context);
            req.setMethod(method);
            for (String header : headers.keySet()) {
                for (String value : headers.get(header)) {
                    req.addHeader(header, value);
                }
            }
            final StringBuilder queryString = new StringBuilder();
            if (query != null && !query.isEmpty()) {
                boolean first = true;
                for (String key : query.keySet()) {
                    final Set<String> values = query.get(key);
                    req.addParameter(key, values.toArray(new String[values
                            .size()]));
                    if (first) {
                        queryString.append("?");
                        first = false;
                    } else {
                        queryString.append("&");
                    }
                    queryString.append(key).append("=");
                    Iterator<String> i = values.iterator();
                    queryString.append(i.next());
                    while (i.hasNext()) {
                        queryString.append(",").append(i.next());
                    }
                }
                req.setQueryString(queryString.toString());
            }
            req.setRequestURI(path + queryString.toString());
            if (path == null) {
                path = "/";
            }
            req.setPathInfo(path);
            if (content != null) {
                req.setContent(content
                        .getBytes(MockHttpExecutor.ENCODING));
            }
            return req;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public MockHttpResponse response() {
        try {
            MockHttpResponse res = new MockHttpResponse();
            this.servlet.service(build(), res);
            return res;
        } catch (ServletException ex) {
            // FIXME message
            throw new AssertionError();
        } catch (IOException ex) {
            // FIXME message
            throw new AssertionError();
        }
    }

    @Override
    public void execute() {
        response();
    }
}
