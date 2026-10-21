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

        if (accessor == null) {
            return msg;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(jwtFilter.extractAuthentication(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("Authentication required");
            }
            String dest = accessor.getDestination();
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                if ("/user/queue/notifications".equals(dest) || "/user/queue/chat/deleted".equals(dest)) {
                    return msg;
                }
                if (dest == null || !dest.matches("^/topic/chat/-?\\d+(/read|/messageAction)?$")) {
                    throw new AccessDeniedException("Cannot subscribe to " + dest);
                }
            } else if (dest == null || !dest.matches("^/app/chat/-?\\d+/(sendMessage|read|messageAction)$")) {
                throw new AccessDeniedException("Cannot send to " + dest);
            }
            long chatId = Long.parseLong(dest.split("/")[3]);
            if (!chatService.isMember(chatId, accessor.getUser().getName())) {
                throw new AccessDeniedException("Not a participant of chat " + chatId);
            }
        }
        return msg;
    }
}
