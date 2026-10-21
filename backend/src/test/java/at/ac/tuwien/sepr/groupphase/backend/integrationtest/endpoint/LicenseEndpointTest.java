package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.License;
import at.ac.tuwien.sepr.groupphase.backend.repository.LicenseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
public class LicenseEndpointTest implements TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanup cleanup;

    private Long testLicenseId;
    private ApplicationUser testUser;

    private static final String LICENSE_URI = "/api/v1/licenses";
    private final Gson gson = new Gson();

    @BeforeEach
    public void setupSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ethan", "12345678", List.of())
        );
    }

    @BeforeEach
    public void setup() {

        cleanup.clearAll();

        testUser = userRepository.findUserByUsername("ethan")
            .orElseGet(() -> {
                ApplicationUser newUser = ApplicationUser.builder()
                    .username("ethan")
                    .email("ethan@example.com")
                    .password("12345678")
                    .role(Role.WORKER)
                    .build();
                return userRepository.save(newUser);
            });


        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ethan", "12345678", List.of())
        );

        License license = License.builder()
            .filename("test-license.pdf")
            .description("A test license")
            .file("Dummy content".getBytes(StandardCharsets.UTF_8))
            .mediaType("application/pdf")
            .worker(testUser)
            .build();

        testLicenseId = licenseRepository.save(license).getId();
    }

    @AfterEach
    public void cleanupTestData() {
        licenseRepository.deleteById(testLicenseId);
    }
    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void getWorkerLicenses_shouldReturnList() throws Exception {
        mockMvc.perform(get(LICENSE_URI + "/user/ethan")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void getLicenseById_whenValid_shouldReturnOk() throws Exception {
        mockMvc.perform(get(LICENSE_URI + "/"+ testLicenseId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void getLicenseById_whenInvalid_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(LICENSE_URI + "/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void createLicense_whenValid_shouldReturnOk() throws Exception {
        var licenseDto = new LicenseCreateDto("Test License.pdf", "Sample Description", null);
        var jsonPart = new MockMultipartFile(
            "certificateInfo",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            gson.toJson(licenseDto).getBytes(StandardCharsets.UTF_8)
        );

        var filePart = new MockMultipartFile(
            "certificate",
            "license.pdf",
            "application/pdf",
            "Dummy PDF content".getBytes()
        );

        mockMvc.perform(multipart(LICENSE_URI)
                .file(jsonPart)
                .file(filePart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void createLicense_whenMissingFile_shouldReturnBadRequest() throws Exception {
        var licenseDto = new LicenseCreateDto("No File", "Missing file test", null);
        var jsonPart = new MockMultipartFile(
            "certificateInfo",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            gson.toJson(licenseDto).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(LICENSE_URI)
                .file(jsonPart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void updateLicense_whenMissingFile_shouldReturnBadRequest() throws Exception {
        var updateDto = new LicenseCreateDto("Missing File", "Should fail", null);
        var jsonPart = new MockMultipartFile(
            "certificateInfo",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            gson.toJson(updateDto).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(LICENSE_URI + "/-104/edit")
                .file(jsonPart)
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                })
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void deleteLicense_whenNotExist_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete(LICENSE_URI + "/-104"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void deleteLicense_whenValid_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete(LICENSE_URI + "/" + testLicenseId))
            .andExpect(status().isNoContent());
    }


    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void getLicenseFile_whenValid_shouldReturnFile() throws Exception {
        mockMvc.perform(get(LICENSE_URI + "/" + testLicenseId + "/file"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"test-license.pdf\""))
            .andExpect(content().contentTypeCompatibleWith("application/pdf"));
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void updateLicense_whenInvalidFileType_shouldReturnBadRequest() throws Exception {
        var updateDto = new LicenseCreateDto("Invalid File Type", "Should fail", null);
        var jsonPart = new MockMultipartFile(
            "certificateInfo",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            gson.toJson(updateDto).getBytes(StandardCharsets.UTF_8)
        );

        var filePart = new MockMultipartFile(
            "certificate",
            "license.txt",
            "text/plain",
            "Not a valid type".getBytes()
        );

        mockMvc.perform(multipart(LICENSE_URI + "/" + testLicenseId + "/edit")
                .file(jsonPart)
                .file(filePart)
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                })
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void createLicense_whenOversizedFile_shouldReturnBadRequest() throws Exception {
        var licenseDto = new LicenseCreateDto("Too Large", "File size too big", null);
        var jsonPart = new MockMultipartFile(
            "certificateInfo",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            gson.toJson(licenseDto).getBytes(StandardCharsets.UTF_8)
        );

        byte[] largeFile = new byte[6 * 1024 * 1024];
        var filePart = new MockMultipartFile(
            "certificate",
            "large.pdf",
            "application/pdf",
            largeFile
        );

        mockMvc.perform(multipart(LICENSE_URI)
                .file(jsonPart)
                .file(filePart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void getLicenseFile_whenInvalidId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(LICENSE_URI + "/999999/file"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void updateLicenseStatus_whenInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch(LICENSE_URI + "/" + testLicenseId + "/status")
                .param("status", "NOT_A_REAL_STATUS"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ethan", roles = {"WORKER"})
    public void createLicense_withImageFile_shouldReturnOk() throws Exception {
        var licenseDto = new LicenseCreateDto("Test License.png", "Image upload", null);
        var jsonPart = new MockMultipartFile(
            "certificateInfo",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            gson.toJson(licenseDto).getBytes(StandardCharsets.UTF_8)
        );

        var filePart = new MockMultipartFile(
            "certificate",
            "license.png",
            "image/png",
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        mockMvc.perform(multipart(LICENSE_URI)
                .file(jsonPart)
                .file(filePart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }




}
