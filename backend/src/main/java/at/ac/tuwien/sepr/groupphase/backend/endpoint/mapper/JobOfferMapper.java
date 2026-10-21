package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailWithUsernameDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferSummaryDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface JobOfferMapper {

    /**
     * Convert a JobOffer entity to its detailed DTO representation.
     *
     * @param jobOffer the entity
     * @return the detail DTO
     */
    @Mapping(source = "jobRequest.id", target = "jobRequestId")
    @Mapping(source = "worker.id", target = "workerId")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToInstant")
    JobOfferDetailDto jobOfferToDetailDto(JobOffer jobOffer);

    @Mapping(source = "jobRequest.id", target = "jobRequestId")
    @Mapping(source = "jobRequest.title", target = "requestTitle")
    @Mapping(target = "lowestPrice", ignore = true)
    JobOfferSummaryDto jobOfferToSummaryDto(JobOffer jobOffer);

    @Mapping(source = "jobOffer.jobRequest.id", target = "jobRequestId")
    @Mapping(source = "jobOffer.worker.id", target = "workerId")
    @Mapping(source = "jobOffer.worker.username", target = "workerUsername")
    @Mapping(source = "workerAverageRating", target = "workerAverageRating")
    JobOfferDetailWithUsernameDto jobOfferToDetailDtoWithWorker(JobOffer jobOffer, Float workerAverageRating);

    JobOfferCreateDto jobOfferToCreateDto(JobOffer jobOffer);

    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime ldt) {
        return (ldt == null) ? null : ldt.atOffset(ZoneOffset.UTC).toInstant();
    }
}