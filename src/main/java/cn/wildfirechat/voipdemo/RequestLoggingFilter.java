package cn.wildfirechat.voipdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 所有 HTTP 請求最先經過的過濾器，記錄請求的方法、路徑、參數、body 以及最終響應狀態碼。
 * 用於排查「請求沒有任何反應」的問題：如果這裡都沒有日誌，說明請求根本沒到達服務。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        byte[] body = readBody(req);
        if (body.length > 0) {
            req = new RepeatableBodyRequestWrapper(req, body);
        }

        String uri = req.getRequestURI();
        String query = req.getQueryString();
        String bodyText = body.length > 0 ? new String(body, bodyCharset(req)) : "";
        if (bodyText.length() > 4096) {
            bodyText = bodyText.substring(0, 4096) + "...(truncated)";
        }

        LOG.info(">>> {} {}{} from {} content-type={} body={}",
                req.getMethod(),
                uri,
                query != null ? "?" + query : "",
                req.getRemoteAddr(),
                req.getContentType(),
                bodyText);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, resp);
        } catch (Exception e) {
            LOG.error("<<< {} {} exception after {}ms", req.getMethod(), uri, System.currentTimeMillis() - start, e);
            throw e;
        }
        LOG.info("<<< {} {} status={} cost={}ms", req.getMethod(), uri, resp.getStatus(), System.currentTimeMillis() - start);
    }

    private static byte[] readBody(HttpServletRequest req) throws IOException {
        if (req.getContentLengthLong() <= 0) {
            return new byte[0];
        }
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            return new byte[0];
        }
        InputStream in = req.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static Charset bodyCharset(HttpServletRequest req) {
        String encoding = req.getCharacterEncoding();
        if (encoding == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static class RepeatableBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        RepeatableBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), bodyCharset((HttpServletRequest) getRequest())));
        }
    }
}
