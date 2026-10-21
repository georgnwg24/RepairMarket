package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatImageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatListItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.CreatedChatDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.service.ChatService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ChatEndpoint.BASE_PATH)
public class ChatEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/chats";

    @Autowired private ChatService chatService;
    @Autowired private UserService userService;
    @Autowired private SimpMessagingTemplate broker;

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @PostMapping("/engage/{job_request_id}")
    public ResponseEntity<CreatedChatDto> engageConversation(
        @PathVariable("job_request_id") Long jobRequestId
    ) {
        LOGGER.info("GET " + BASE_PATH + "/{}", jobRequestId);
        LOGGER.debug("request parameters: {}", jobRequestId);
        return new ResponseEntity<>(chatService.findOrCreateChat(getCurrentUser(), jobRequestId), HttpStatus.CREATED);
    }

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @GetMapping
    public ResponseEntity<List<ChatListItemDto>> getAllChats() {
        LOGGER.info("GET " + BASE_PATH);
        return new ResponseEntity<>(chatService.getAllChatsOfUser(getCurrentUser().getUsername()), HttpStatus.OK);
    }

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @DeleteMapping("/{chat_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
        @PathVariable("chat_id") Long chatId
    ) {
        LOGGER.info("DELETE " + BASE_PATH + "/{}", chatId);

        String opponent = chatService.getOtherChatParticipant(chatId,  getCurrentUser().getUsername());

        chatService.deleteChat(getCurrentUser(), chatId);

        broker.convertAndSendToUser(
            opponent,
            "/queue/chat/deleted",
            Collections.singletonMap("chatId", chatId)
        );
    }

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @GetMapping("/{chat_id}/messages")
    public ResponseEntity<List<ChatMessageDetailDto>> getMessages(
        @PathVariable("chat_id") Long chatId
    ) {
        LOGGER.info("GET " + BASE_PATH + "/{}/messages", chatId);
        List<ChatMessageDetailDto> messages = chatService.getChatMessages(getCurrentUser(), chatId);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @GetMapping("/{chat_id}/messages/last")
    public ResponseEntity<ChatMessage> getLastMessage(
        @PathVariable("chat_id") Long chatId
    ) {
        LOGGER.info("GET " + BASE_PATH + "/{}/messages/last", chatId);
        return new ResponseEntity<>(chatService.getLastMessage(getCurrentUser(), chatId), HttpStatus.OK);
    }

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @PostMapping(value = "/uploads", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ChatImageDto> uploadImage(
        @RequestPart("image") MultipartFile image
    ) throws FileUploadException {
        LOGGER.info("POST " + BASE_PATH + "/uploads");
        LOGGER.debug("RAW IMAGE: {}", image);
        LOGGER.debug("Content-Type: {} | {}", image.getContentType(), image.getOriginalFilename());
        return new ResponseEntity<>(chatService.uploadImage(image), HttpStatus.CREATED);
    }


    private ApplicationUser getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.findUserByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }

}
