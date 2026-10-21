package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequestImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobRequestImageMapper {
    @Mapping(target = "downloadUrl", expression = "java(getDownloadUrl(image))")
    JobRequestImageDetailDto toDetailDto(JobRequestImage image);

    default String getDownloadUrl(JobRequestImage image) {
        return String.format("/job-requests/%d/images/%d",
            image.getJobRequest().getId(),
            image.getId()
        );
    }
}
