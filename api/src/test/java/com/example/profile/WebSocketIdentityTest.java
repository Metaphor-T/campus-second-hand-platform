package com.example.profile;

import com.example.service.*;
import com.example.websocket.*;
import com.example.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;
import java.util.Map;
import static org.mockito.Mockito.*;

class WebSocketIdentityTest {
    @Test void revokedOpenSessionCannotWriteChatOrUseHeartbeat() throws Exception {
        var identity=mock(SessionIdentityService.class);var manager=mock(WebSocketSessionManager.class);var messages=mock(ChatMessageService.class);var json=mock(ObjectMapper.class);
        var handler=new ChatWebSocketHandler(identity,manager,messages,json);var session=mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userId",2,"role","USER","credential","old-proof"));
        doThrow(new BusinessException("revoked",401)).when(identity).validateSession(2,"USER","old-proof");
        handler.handleMessage(session,new TextMessage("{\"type\":\"MESSAGE\",\"toUserId\":1,\"content\":\"must not save\"}"));
        verify(session).close(CloseStatus.POLICY_VIOLATION);verifyNoInteractions(messages,json);
    }
}
