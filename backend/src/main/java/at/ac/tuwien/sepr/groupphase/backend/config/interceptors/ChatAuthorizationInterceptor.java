package at.ac.tuwien.sepr.groupphase.backend.config.interceptors;

import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.ChatService;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ChatAuthorizationInterceptor implements ChannelInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final JwtAuthorizationFilter jwtFilter;
    private final ChatService chatService;

    @Autowired
    public ChatAuthorizationInterceptor(JwtAuthorizationFilter jwtFilter, ChatService chatService) {
        this.jwtFilter = jwtFilter;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> msg, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(msg, StompHeaderAccessor.class);
        LOGGER.trace("PRESEND: \n accessor={} \n msg={} \n user={}",
            accessor, msg, (accessor == null ? "null" : accessor.getUser()));

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            LOGGER.debug("STOMP CONNECT");
            // Access authentication header(s) and invoke accessor.setUser(user) (spring doc)
            accessor.setUser(jwtFilter.extractAuthentication(accessor.getNativeHeader("Authorization").getFirst()));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination(); // "/topic/chat/123" OR "/user/queue/notifications"

            if ("/user/queue/notifications".equals(dest)) {
                return msg;
            }

            if (dest != null && dest.matches("^/topic/chat/-?\\d+/read$")) {
                return msg;
            }

            if (dest != null && dest.matches("^/topic/chat/-?\\d+/messageAction$")) {
                return msg;
            }

            if ("/user/queue/chat/deleted".equals(dest)) {
                return msg;
            }

            if (dest != null && dest.matches("^/topic/chat/-?\\d+$")) {
                long chatId = Long.parseLong(dest.substring("/topic/chat/".length()));
                String user = accessor.getUser().getName();
                if (!chatService.isMember(chatId, user)) {
                    throw new AccessDeniedException("Cannot subscribe to chat " + chatId);
                }
                return msg;
            }

            throw new AccessDeniedException("Cannot subscribe to " + dest);
        }
        return msg;
    }

}
