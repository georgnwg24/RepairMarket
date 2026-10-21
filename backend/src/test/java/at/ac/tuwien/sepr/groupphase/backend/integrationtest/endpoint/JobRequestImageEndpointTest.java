package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@EnableWebMvc
@WebAppConfiguration
@AutoConfigureMockMvc
@Transactional
public class JobRequestImageEndpointTest implements TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    private String getAuthTokenFor(List<String> roles) {
        return jwtTokenizer.getAuthToken("ned", roles, -104L);
    }

    @Test
    public void uploadImage_whenValid_shouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        mockMvc.perform(multipart(JOB_REQUEST_URI + "/-106/images")
                .file(file)
                .param("displayPosition", "0")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.imageType").value("image/jpeg"))
            .andExpect(jsonPath("$.displayPosition").value(0))
            .andExpect(jsonPath("$.downloadUrl").exists());
    }

    @Test
    public void uploadImage_whenInvalidFileType_shouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "invalid content".getBytes()
        );

        mockMvc.perform(multipart(JOB_REQUEST_URI +"/-106/images")
                .file(file)
                .param("displayPosition", "0")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void uploadImage_whenExceedsMaxImages_shouldReturnConflict() throws Exception {
        for (int i = 0; i < 4; i++) {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "test" + i + ".jpg",
                "image/jpeg",
                ("test image " + i).getBytes()
            );
            mockMvc.perform(multipart(JOB_REQUEST_URI +"/-106/images")
                    .file(file)
                    .param("displayPosition", String.valueOf(i))
                    .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test5.jpg",
            "image/jpeg",
            "test image 5".getBytes()
        );
        mockMvc.perform(multipart(JOB_REQUEST_URI + "/-106/images")
                .file(file)
                .param("displayPosition", "5")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isConflict());
    }

    @Test
    public void getAllImages_whenJobRequestExists_shouldReturnList() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image".getBytes()
        );
        mockMvc.perform(multipart(JOB_REQUEST_URI +"/-106/images")
                .file(file)
                .param("displayPosition", "0")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated());

        mockMvc.perform(get(JOB_REQUEST_URI + "/-106/images")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].imageType").value("image/jpeg"))
            .andExpect(jsonPath("$[0].displayPosition").value(0));
    }

    @Test
    public void getImageData_whenValid_shouldReturnImageBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image".getBytes()
        );
        MvcResult uploadResult = mockMvc.perform(multipart(JOB_REQUEST_URI + "/-106/images")
                .file(file)
                .param("displayPosition", "0")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andReturn();

        String content = uploadResult.getResponse().getContentAsString();
        Long imageId = JsonPath.parse(content).read("$.id", Long.class);

        mockMvc.perform(get(JOB_REQUEST_URI + "/-106/images/" + imageId)
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(content().bytes("test image".getBytes()));
    }

    @Test
    public void deleteImage_whenValid_shouldReturnNoContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image".getBytes()
        );
        MvcResult uploadResult = mockMvc.perform(multipart(JOB_REQUEST_URI + "/-106/images")
                .file(file)
                .param("displayPosition", "0")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER")))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andReturn();

        String content = uploadResult.getResponse().getContentAsString();
        Long imageId = JsonPath.parse(content).read("$.id", Long.class);

        mockMvc.perform(delete(JOB_REQUEST_URI + "/-106/images/" + imageId)
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER"))))
            .andExpect(status().isNoContent());

        mockMvc.perform(get(JOB_REQUEST_URI + "/-106/images/" + imageId)
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER"))))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getImageData_whenInvalidId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(JOB_REQUEST_URI + "/-106/images/9999")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER"))))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteImage_whenInvalidId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete(JOB_REQUEST_URI + "/-106/images/9999")
                .header("Authorization", getAuthTokenFor(List.of("ROLE_CUSTOMER"))))
            .andExpect(status().isNotFound());
    }
}