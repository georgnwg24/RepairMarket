package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.ApplicationUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobRequests", ignore = true)
    @Mapping(target = "properties", ignore = true)
    @Mapping(target = "customerChats", ignore = true)
    @Mapping(target = "sentJobOffers", ignore = true)
    @Mapping(target = "licenses", ignore = true)
    @Mapping(target = "workerChats", ignore = true)
    @Mapping(target = "ratingsReceived", ignore = true)
    ApplicationUser userRegistrationDtoToApplicationUser(UserRegistrationDto dto);

    UserUpdateDetailDto applicationUserToUserUpdateDetailDto(ApplicationUser user);

    UserDetailDto applicationUserToUserDetailDto(ApplicationUser user);

    void updateUserFromDto(UserUpdateDto userUpdate, @MappingTarget ApplicationUser user);

    @Mapping(target = "banned", source = "banned")
    UserListDto applicationUserToUserListDto(ApplicationUser user);

    List<UserListDto> applicationUsersToUserListDto(List<ApplicationUser> users);
}
