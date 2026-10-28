package com.example.websocket;

import com.example.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 握手拦截器 —— JWT 鉴权
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从 URL 查询参数中获取 token
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            log.warn("WebSocket 握手失败：缺少 token 参数");
            return false;
        }

        String token = null;
        String[] params = query.split("&");
        for (String param : params) {
            String[] pair = param.split("=", 2);
            if ("token".equals(pair[0]) && pair.length == 2) {
                token = pair[1];
                break;
            }
        }

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手失败：token 参数为空");
            return false;
        }

        // 验证 JWT
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket 握手失败：token 无效");
                return false;
            }

            // 解析用户信息存入 attributes，传递给 WebSocketHandler
            String userIdStr = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);
            Integer userId = Integer.parseInt(userIdStr);

            attributes.put("userId", userId);
            attributes.put("role", role);

            log.info("WebSocket 握手成功: userId={}, role={}", userId, role);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手失败：token 解析异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需额外操作
    }
}
