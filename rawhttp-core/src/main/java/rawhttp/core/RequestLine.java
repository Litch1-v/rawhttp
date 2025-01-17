package rawhttp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A {@link RawHttpRequest}'s start-line.
 */
public class RequestLine implements StartLine {

    private final String method;
    private final URI uri;
    private final HttpVersion httpVersion;
    private final boolean absoluteFormURI;

    /**
     * Create a new {@link RequestLine}.
     * <p>
     * This constructor does not validate the method name. If validation is required,
     * use the {@link HttpMetadataParser#parseRequestLine(java.io.InputStream)} method.
     *
     * @param method      name of the HTTP method
     * @param uri         URI of the request target
     * @param httpVersion HTTP version of the message
     */
    public RequestLine(String method, URI uri, HttpVersion httpVersion) {
        this.method = method;
        this.uri = uri;
        this.httpVersion = httpVersion;
        this.absoluteFormURI = false;
    }

    public RequestLine(String method, URI uri, HttpVersion httpVersion,boolean absoluteFormURI) {
        this.method = method;
        this.uri = uri;
        this.httpVersion = httpVersion;
        this.absoluteFormURI = absoluteFormURI;
    }

    /**
     * @return HTTP request's method name.
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return the URI associated with this method line.
     */
    public URI getUri() {
        return uri;
    }

    @Override
    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * Copy this {@link RequestLine}, replacing the host in its URI.
     * <p>
     * The new host may include a port using the "host:port" syntax.
     * <p>
     * The new URI never maintains the old URI's port, even if the port is omitted from the new host String.
     *
     * @param host the host to be used in the method line's URI.
     * @return a copy of this method line, but with the given host
     */
    public RequestLine withHost(String host) {
        return new RequestLine(method, UriUtil.withHost(uri, host), httpVersion, absoluteFormURI);
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        writeTo(outputStream, true);
    }

    /**
     * @return the start-line for this method line.
     */
    @Override
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(256);
        try {
            writeTo(out, false);
        } catch (IOException e) {
            // cannot happen, in-memory OutputStream used
        }
        return new String(out.toByteArray(), StandardCharsets.US_ASCII);
    }

    private void writeTo(OutputStream outputStream, boolean newLine) throws IOException {
        outputStream.write(method.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(' ');

        String path;
        if (absoluteFormURI){
            path = uri.getScheme() + ":" + uri.getSchemeSpecificPart();
            outputStream.write(path.getBytes(StandardCharsets.US_ASCII));
        } else {
            path = uri.getRawPath();
            if (path == null || path.isEmpty()) {
                outputStream.write('/');
            } else {
                outputStream.write(path.getBytes(StandardCharsets.US_ASCII));
            }
            String query = uri.getRawQuery();
            if (query != null && !query.isEmpty()) {
                outputStream.write('?');
                outputStream.write(query.getBytes(StandardCharsets.US_ASCII));
            }
        }

        outputStream.write(' ');
        httpVersion.writeTo(outputStream);

        if (newLine) {
            outputStream.write('\r');
            outputStream.write('\n');
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestLine that = (RequestLine) o;
        return method.equals(that.method) &&
                uri.equals(that.uri) &&
                httpVersion == that.httpVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, httpVersion);
    }
}