package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.config.interceptors.ChatAuthorizationInterceptor;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAuthorizationRegressionTest {
    private final ChatService chats = mock(ChatService.class);
    private final ChatAuthorizationInterceptor interceptor = new ChatAuthorizationInterceptor(mock(JwtAuthorizationFilter.class), chats);

    private Message<byte[]> frame(StompCommand command, String destination) {
        var headers = StompHeaderAccessor.create(command);
        headers.setUser(() -> "member");
        headers.setDestination(destination);
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    @Test
    void rejectsDirectBrokerWritesEvenForMembers() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame(StompCommand.SEND, "/topic/chat/1"), null));
    }

    @Test
    void checksMembershipForAllChatSubscriptions() {
        for (String suffix : new String[]{"", "/read", "/messageAction"}) {
            var message = frame(StompCommand.SUBSCRIBE, "/topic/chat/1" + suffix);
            assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
        }
    }

    @Test
    void permitsMembersToSubscribeAndSendApplicationCommands() {
        when(chats.isMember(1L, "member")).thenReturn(true);
        var subscription = frame(StompCommand.SUBSCRIBE, "/topic/chat/1/messageAction");
        var send = frame(StompCommand.SEND, "/app/chat/1/sendMessage");
        assertSame(subscription, interceptor.preSend(subscription, null));
        assertSame(send, interceptor.preSend(send, null));
    }
}
