package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DemoAccountRegressionTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private JwtTokenizer tokens;

    private ApplicationUser account(String name, Role role) {
        return users.save(ApplicationUser.builder().username(name).email(name + "@example.com")
            .password("unused").role(role).build());
    }

    private String token(ApplicationUser user) {
        return tokens.getAuthToken(user.getUsername(), List.of("ROLE_" + user.getRole()), user.getId());
    }

    @Test
    void publicRegistrationCannotCreateAdmin() throws Exception {
        mvc.perform(post("/api/v1/authentication/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"newadmin","email":"admin@example.com","role":"ADMIN",
                     "password":"password123","confirmPassword":"password123"}
                    """))
            .andExpect(status().isForbidden());
        assertFalse(users.existsByUsername("newadmin"));
    }

    @Test
    void registrationDoesNotExposePassword() throws Exception {
        mvc.perform(post("/api/v1/authentication/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"newcustomer","email":"customer@example.com","role":"CUSTOMER",
                     "password":"password123","confirmPassword":"password123"}
                    """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deletionRequiresOwnerAndWorksWithoutSampleData() throws Exception {
        var owner = account("demoowner", Role.CUSTOMER);
        var other = account("demoother", Role.CUSTOMER);
        mvc.perform(delete("/api/v1/authentication/demoowner")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/authentication/demoowner").header("Authorization", token(other)))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/authentication/demoowner").header("Authorization", token(owner)))
            .andExpect(status().isNoContent());
        assertFalse(users.existsByUsername("demoowner"));
    }

    @Test
    void deletedAdminTokenIsRejected() throws Exception {
        var admin = account("demoadmin", Role.ADMIN);
        var jwt = token(admin);
        users.delete(admin);
        users.flush();
        mvc.perform(get("/api/v1/users").header("Authorization", jwt)).andExpect(status().isUnauthorized());
    }
}
