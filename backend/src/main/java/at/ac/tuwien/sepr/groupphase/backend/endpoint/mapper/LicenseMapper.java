package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.AdminLicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDownloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseStatusUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.License;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LicenseMapper {


    LicenseDetailDto licenseToDetailDto(License license);

    LicenseDownloadDto licenseToDownloadDto(License license);

    @Named("license")
    LicenseListDto licenseToListDto(License license);

    @IterableMapping(qualifiedByName = "license")
    List<LicenseListDto> licenseToListDto(List<License> license);

    void updateLicenseFromDto(LicenseUpdateDto licenseUpdateDto, @MappingTarget License license);

    void updateStatusFromDto(LicenseStatusUpdateDto dto, @MappingTarget License license);

    @Mapping(source = "worker.username", target = "username")
    AdminLicenseListDto licenseToAdminDto(License license);

    List<AdminLicenseListDto> licenseToAdminDto(List<License> licenses);
}
