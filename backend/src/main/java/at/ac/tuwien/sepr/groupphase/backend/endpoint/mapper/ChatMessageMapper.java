package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessagePayloadDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(source = "sender.username", target = "senderUsername")
    ChatMessagePayloadDto entityToPayLoadDto(ChatMessage chatMessage);

    ChatMessage dtoToEntity(ChatMessageDto chatMessageDto);

    ChatMessageDto entityToDto(ChatMessage chatMessage);

}
