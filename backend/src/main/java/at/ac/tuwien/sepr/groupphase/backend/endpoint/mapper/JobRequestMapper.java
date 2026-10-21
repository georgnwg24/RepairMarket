package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListWithMinPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestWithCustomerDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobRequestMapper {

    /**
     * Maps a JobRequest entity to a DetailDto.
     *
     * @param jobRequest the JobRequest that gets mapped
     * @return a JobRequestDetailDto with information from jobRequest
     */

    @Mapping(source = "property.id", target = "propertyId")
    JobRequestDetailDto jobRequestToDetailDto(JobRequest jobRequest);

    /**
     * Maps a JobRequest entity to an UpdateDto.
     *
     * @param jobRequest the JobRequest that gets mapped
     * @return a jobRequestToUpdateDto with information from jobRequest
     */

    @Mapping(source = "property.id", target = "propertyId")
    JobRequestUpdateDto jobRequestToUpdateDto(JobRequest jobRequest);

    void updateJobRequestFromDto(JobRequestUpdateDto jobRequestUpdateDto, @MappingTarget JobRequest jobRequest);


    /**
     * Maps a JobRequest entity to a ListDto, including the property's address.
     *
     * @param jobRequest the JobRequest that gets mapped
     * @return a JobRequestListDto with information from jobRequest
     */
    @Mapping(source = "property.address", target = "address")
    @Named("jobRequest")
    JobRequestListDto jobRequestToListDto(JobRequest jobRequest);

    @IterableMapping(qualifiedByName = "jobRequest")
    List<JobRequestListDto> jobRequestToListDto(List<JobRequest> jobRequest);

    /**
     * Maps a JobRequestCreateDto to a JobRequest entity.
     *
     * @param createDto the DTO with creation information
     * @return a JobRequest entity with information from the createDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "property", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "jobRequestImages", ignore = true)
    @Mapping(target = "receivedJobOffers", ignore = true)
    JobRequest createDtoToJobRequest(JobRequestCreateDto createDto);

    @Mapping(target = "lowestPrice", ignore = true)
    JobRequestListWithMinPriceDto jobRequestToListWithMinPriceDto(JobRequest jobRequest);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.username", target = "customerUsername")
    @Mapping(source = "property.id", target = "propertyId")
    JobRequestWithCustomerDto jobRequestToWithCustomerDto(JobRequest jobRequest);
}
