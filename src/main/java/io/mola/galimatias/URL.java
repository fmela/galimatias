/*
 * Copyright (c) 2013 Santiago M. Mola <santi@mola.io>
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a
 *   copy of this software and associated documentation files (the "Software"),
 *   to deal in the Software without restriction, including without limitation
 *   the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *   and/or sell copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *   OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *   FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 *   DEALINGS IN THE SOFTWARE.
 */

package io.mola.galimatias;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A parsed URL. Immutable.
 *
 * TODO: Add modifier methods.
 *
 * TODO: Study android.net.URI implementation. It has interesting API
 *       bits and tricks.
 *
 */
public class URL implements Serializable {

    private final String scheme;
    private final String schemeData;
    private final String username;
    private final String password;
    private final Host host;
    private final int port;
    private final String path;
    private final String query;
    private final String fragment;

    private final boolean isHierarchical;

    URL(final String scheme, final String schemeData,
        final String username, final String password,
        final Host host, final int port,
        final Iterable<String> pathSegments,
        final String query, final String fragment,
        final boolean isHierarchical) {
        this(scheme, schemeData, username, password, host, port, pathSegmentsToString(pathSegments),
                query, fragment, isHierarchical);
    }

    URL(final String scheme, final String schemeData,
            final String username, final String password,
            final Host host, final int port,
            final String path,
            final String query, final String fragment,
            final boolean isHierarchical) {
        this.scheme = scheme;
        this.schemeData = schemeData;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
        this.isHierarchical = isHierarchical;
    }

    public String scheme() {
        return scheme;
    }

    public String schemeData() {
        return schemeData;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    /**
     * Mirrors {@link java.net.URL#getUserInfo()} behaviour.
     *
     * @return
     */
    public String userInfo() {
        if (username == null) {
            return null;
        }
        if (password == null) {
            return username;
        }
        return String.format("%s:%s", username, password);
    }

    public Host host() {
        return host;
    }

    public String authority() {
        if (!isHierarchical) {
            return null;
        }
        if (host == null) {
            return null;
        }
        StringBuilder output = new StringBuilder();
        if (username != null || password != null) {
            output.append(userInfo()).append('@');
        }
        output.append(host.toString());
        if (port != -1) {
            output.append(':').append(port);
        }
        return output.toString();
    }

    public int port() {
        return (port == -1)? defaultPort() : port;
    }

    public int defaultPort() {
        String defaultPort = URLUtils.getDefaultPortForScheme(scheme);
        if (defaultPort == null) {
            return -1;
        }
        return Integer.parseInt(defaultPort);
    }

    public String path() {
        return path;
    }

    public List<String> pathSegments() {
        if (!isHierarchical) {
            return null;
        }
        return pathStringToSegments(path);
    }

    public String query() {
        return query;
    }

    public String fragment() {
        return fragment;
    }

    public String file() {
        if (path == null && query == null) {
            return "";
        }
        final StringBuilder output = new StringBuilder(
                ((path != null)? path.length() : 0) +
                ((query != null)? query.length() + 1 : 0)
                );
        if (path != null) {
            output.append(path);
        }
        if (query != null) {
            output.append('?').append(query);
        }
        return output.toString();
    }

    public boolean isHierarchical() {
        return isHierarchical;
    }

    public boolean isOpaque() {
        return !isHierarchical;
    }

    private static String pathSegmentsToString(final Iterable<String> segments) {
        if (segments == null) {
            return null;
        }
        final StringBuilder output = new StringBuilder();
        for (final String segment : segments) {
            output.append('/').append(segment);
        }
        if (output.length() == 0) {
            return "/";
        }
        return output.toString();
    }

    private static List<String> pathStringToSegments(String path) {
        if (path == null) {
            return new ArrayList<String>();
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        final String[] segments = path.split("/", -1);
        final List<String> result = new ArrayList<String>(segments.length + 1);
        if (segments.length == 0) {
            result.add("");
            return result;
        }
        result.addAll(Arrays.asList(segments));
        return result;
    }

    /**
     * Resolves a relative reference to an absolute URL.
     *
     * This is just a convenience method equivalent to:
     *
     * <pre>
     * <code>
     *  URL base = URL.parse("http://base.com");
     *  String relativeReference = "/foo/bar";
     *  URL absoluteURL = base.resolve(relativeReference);
     * </code>
     * </pre>
     *
     * @param input Relative reference.
     * @return Resolved absolute URL.
     * @throws GalimatiasParseException
     */
    public URL resolve(final String input) throws GalimatiasParseException {
        return new URLParser(this, input).parse();
    }

    /**
     * UNIMPLEMENTED.
     *
     * @param url Absolute URL.
     * @return Relative reference.
     */
    public String relativize(final URL url) {
        //TODO
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    /**
     * Parses a URL by using the default parsing options.
     *
     * @param input
     * @return
     * @throws GalimatiasParseException
     */
    public static URL parse(final String input) throws GalimatiasParseException {
        return new URLParser(input).parse();
    }

    public static URL parse(final URL base, final String input) throws GalimatiasParseException {
        return new URLParser(base, input).parse();
    }

    public static URL parse(final URLParsingSettings settings, final String input) throws GalimatiasParseException {
        return new URLParser(input).settings(settings).parse();
    }

    public static URL parse(final URLParsingSettings settings, final URL base, final String input) throws GalimatiasParseException {
        return new URLParser(base, input).settings(settings).parse();
    }

    public URL withScheme(final String scheme) throws GalimatiasParseException {
        if (this.scheme.equalsIgnoreCase(scheme)) {
            return this;
        }
        if (scheme == null) {
            throw new NullPointerException("null scheme");
        }
        if (scheme.isEmpty()) {
            throw new GalimatiasParseException("empty scheme");
        }
        if (URLUtils.isRelativeScheme(scheme) == URLUtils.isRelativeScheme(this.scheme)) {
            return new URLParser(scheme + ":", this, URLParser.ParseURLState.SCHEME_START).parse();
        }
        return new URLParser(toString().replaceFirst(this.scheme, scheme)).parse();
    }

    public URL withUsername(final String username) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set username on opaque URL");
        }
        if (this.username != null && this.username.equals(username)) {
            return this;
        }
        final String newUsername = (username == null)? null : new URLParser(username).parseUsername();
        return new URL(this.scheme, this.schemeData, newUsername, this.password, this.host, this.port, this.path, this.query, this.fragment, true);
    }

    public URL withPassword(final String password) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set password on opaque URL");
        }
        if (this.password != null && this.password.equals(password)) {
            return this;
        }
        final String newPassword = (password == null || password.isEmpty())? null : new URLParser(password).parsePassword();
        return new URL(this.scheme, this.schemeData, this.username, newPassword, this.host, this.port, this.path, this.query, this.fragment, true);
    }

    public URL withHost(final String host) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set host on opaque URL");
        }
        return withHost(Host.parseHost(host));
    }

    public URL withHost(final Host host) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set host on opaque URL");
        }
        if (host == null) {
            throw new NullPointerException("null host");
        }
        if (this.host != null && this.host.equals(host)) {
            return this;
        }
        return new URL(this.scheme, this.schemeData, this.username, this.password, host, this.port, this.path, this.query, this.fragment, true);
    }

    public URL withPort(final int port) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set port on opaque URL");
        }
        if (port == this.port) {
            return this;
        }
        if (this.port == -1 && port == defaultPort()) {
            return this;
        }
        return new URL(this.scheme, this.schemeData, this.username, this.password, this.host, port, this.path, this.query, this.fragment, true);
    }

    public URL withPath(final String path) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set path on opaque URL");
        }
        return new URLParser(path, this, URLParser.ParseURLState.RELATIVE_PATH_START).parse();
    }

    public URL withQuery(String query) throws GalimatiasParseException {
        if (!isHierarchical) {
            throw new GalimatiasParseException("Cannot set query on opaque URL");
        }
        if (query != null && query.isEmpty()) {
            query = null;
        }
        if (this.query == query) {
            return this;
        }
        if (this.query != null && this.query.equals(query)) {
            return this;
        }
        if (query == null) {
            return new URL(this.scheme, this.schemeData, this.username, this.password, this.host, this.port, this.path, null, this.fragment, true);
        }
        final String parseQuery = (query.charAt(0) == '?')? query.substring(1, query.length()) : query;
        return new URLParser(parseQuery, this, URLParser.ParseURLState.QUERY).parse();
    }

    public URL withFragment(String fragment) throws GalimatiasParseException {
        if ("javascript".equals(scheme)) {
            throw new GalimatiasParseException("Cannot set fragment on 'javascript:' URL");
        }
        if (fragment != null && fragment.isEmpty()) {
            fragment = null;
        }
        if (this.fragment == fragment) {
            return this;
        }
        if (this.fragment != null && this.fragment.equals(fragment)) {
            return this;
        }
        if (fragment == null) {
            return new URL(this.scheme, this.schemeData, this.username, this.password, this.host, this.port, this.path, this.query, null, true);
        }
        final String parseFragment = (fragment.charAt(0) == '#')? fragment.substring(1, fragment.length()) : fragment;
        return new URLParser(parseFragment, this, URLParser.ParseURLState.FRAGMENT).parse();
    }

    /**
     * Converts to {@link java.net.URI}.
     *
     * Conversion to {@link java.net.URI} will throw
     * {@link java.net.URISyntaxException} if the URL contains
     * unescaped unsafe characters as defined in RFC 2396.
     * In order to prevent this, force RFC 2396 compliance when
     * parsing the URL. For example:
     *
     * <pre>
     * <code>
     * URLParsingSettings settings = URLParsingSettings.create()
     *     .withStandard(URLParsingSettings.Standard.RFC_2396);
     * URL url = URI.parse(settings, url);
     * java.net.URI uri = url.toJavaURI();
     * </code>
     * </pre>
     *
     * @return
     */
    public java.net.URI toJavaURI() throws URISyntaxException {
        return new URI(toString());
    }

    /**
     * Converts to {@link java.net.URL}.
     *
     * This method is guaranteed to not throw an exception
     * for URL protocols http, https, ftp, file and jar.
     *
     * It might or might not throw {@link java.net.MalformedURLException}
     * for other URL protocols.
     *
     * @return
     */
    public java.net.URL toJavaURL() throws MalformedURLException {
        return new java.net.URL(toString());
    }

    /**
     * Construct a URL from a {@link java.net.URI}.
     *
     * @param uri
     * @return
     */
    public static URL fromJavaURI(java.net.URI uri) {
        //TODO: Let's do this more efficient.
        try {
            return new URLParser(uri.toString()).parse();
        } catch (GalimatiasParseException e) {
            // This should not happen.
            throw new RuntimeException("BUG", e);
        }
    }

    /**
     * Construct a URL from a {@link java.net.URL}.
     *
     * @param url
     * @return
     */
    public static URL fromJavaURL(java.net.URL url) {
        //TODO: Let's do this more efficient.
        try {
            return new URLParser(url.toString()).parse();
        } catch (GalimatiasParseException e) {
            // This should not happen.
            throw new RuntimeException("BUG", e);
        }
    }

    /**
     * Serializes the URL.
     *
     * Note that the "exclude fragment flag" (as in WHATWG standard) is not implemented.
     *
     */
    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder();

        output.append(scheme).append(':');

        if (isHierarchical) {
            output.append("//");
            if (username != null || password != null) {
                if (username != null) {
                    output.append(username);
                }
                if (password != null) {
                   output.append(':').append(password);
                }
                output.append('@');
            }
            if (host != null) {
                if (host instanceof IPv6Address) {
                    output.append('[').append(host).append(']');
                } else {
                    output.append(host);
                }
            }
            if (port != -1) {
                output.append(':').append(port);
            }
            if (path != null) {
                output.append(path);
            }
        } else {
            output.append(schemeData);
        }

        if (query != null) {
            output.append('?').append(query);
        }

        if (fragment != null) {
            output.append('#').append(fragment);
        }

        return output.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof URL)) {
            return false;
        }
        final URL other = (URL) obj;
        return  isHierarchical == other.isHierarchical &&
                ((scheme == null)? other.scheme == null : scheme.equals(other.scheme)) &&
                ((username == null)? other.username == null : username.equals(other.username)) &&
                ((password == null)? other.password == null : password.equals(other.password)) &&
                ((host == null)? other.host == null : host.equals(other.host)) &&
                port == other.port &&
                ((path == null)? other.host == null : path.equals(other.path)) &&
                ((fragment == null)? other.fragment == null : fragment.equals(other.fragment)) &&
                ((query == null)? other.query == null : query.equals(other.query))
                ;
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (schemeData != null ? schemeData.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != -1 ? port : 0);
        result = 31 * result + (path != null? path.hashCode() : 0);
        result = 31 * result + (query != null ? query.hashCode() : 0);
        result = 31 * result + (fragment != null ? fragment.hashCode() : 0);
        result = 31 * result + (isHierarchical ? 1 : 0);
        return result;
    }

}