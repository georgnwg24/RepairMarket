package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportListDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "reporterUsername", source = "reporter.username")
    @Mapping(target = "targetUsername", source = "target.username")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "open", source = "open")
    @Mapping(target = "reportedAt", source = "reportedAt")
    // pull in all the JR fields:
    @Mapping(target = "jobId", source = "jobRequest.id")
    @Mapping(target = "jobTitle", source = "jobRequest.title")
    @Mapping(target = "description", source = "jobRequest.description")
    @Mapping(target = "category", source = "jobRequest.category")
    @Mapping(target = "status", source = "jobRequest.status")
    @Mapping(target = "deadline", source = "jobRequest.deadline")
    // pull in all the Message fields:
    @Mapping(target = "messageId", source = "chatMessage.id")
    @Mapping(target = "message", source = "chatMessage.message")
    @Mapping(target = "mediaName", source = "chatMessage.mediaName")
    @Mapping(target = "mediaUrl", source = "chatMessage.mediaUrl")
    @Mapping(target = "timestamp", source = "chatMessage.timestamp")
    ReportDetailDto reportToDetailDto(Report report);

    @Mapping(target = "reporterUsername", source = "reporter.username")
    @Mapping(target = "targetUsername", source = "target.username")
    @Mapping(target = "jobRequestTitle", source = "jobRequest.title")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "open", source = "open")
    @Mapping(target = "reportedAt", source = "reportedAt")
    ReportListDto reportToListDto(Report report);

    List<ReportListDto> reportToListDtoList(List<Report> reports);
}
