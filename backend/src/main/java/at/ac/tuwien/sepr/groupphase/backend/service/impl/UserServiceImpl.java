package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.geolocation.GeonamesApiResponse;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Chat;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.License;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import at.ac.tuwien.sepr.groupphase.backend.entity.Rating;
import at.ac.tuwien.sepr.groupphase.backend.entity.Report;
import at.ac.tuwien.sepr.groupphase.backend.exception.EmailAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.UserAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PushSubscriptionRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenizer jwtTokenizer;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PropertyRepository propertyRepository;
    private final JobRequestRepository jobRequestRepository;
    private final RatingRepository ratingRepository;
    private final ReportRepository reportRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenizer jwtTokenizer, UserMapper userMapper, ObjectMapper objectMapper, PushSubscriptionRepository pushSubscriptionRepository,
                           PropertyRepository propertyRepository, JobRequestRepository jobRequestRepository, RatingRepository ratingRepository, ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenizer = jwtTokenizer;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.propertyRepository = propertyRepository;
        this.jobRequestRepository = jobRequestRepository;
        this.ratingRepository = ratingRepository;
        this.reportRepository = reportRepository;
    }

    @Override
    public ApplicationUser register(UserRegistrationDto userRegistrationDto) {
        LOGGER.debug("Register user: {}", userRegistrationDto);
        if (userRepository.existsByEmail(userRegistrationDto.getEmail())) {
            throw new EmailAlreadyExistsException(userRegistrationDto.getEmail());
        }
        if (userRepository.existsByUsername(userRegistrationDto.getUsername())) {
            throw new UserAlreadyExistsException(userRegistrationDto.getUsername());
        }

        ApplicationUser user = userMapper.userRegistrationDtoToApplicationUser(userRegistrationDto);
        String encodedPassword = passwordEncoder.encode(userRegistrationDto.getPassword());
        user.setPassword(encodedPassword);

        // Only geocode if the user is a worker and has location data
        if (user.getRole() == Role.WORKER && user.getPostalCode() != null && user.getCountryCode() != null) {
            updateUserWithGeonamesData(user);
        }

        return userRepository.save(user);
    }

    @Override
    public String login(UserLoginDto userLoginDto) {
        LOGGER.debug("Login user: {}", userLoginDto);
        ApplicationUser user = findUserByUsername(userLoginDto.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.isBanned()) {
            throw new DisabledException("Your account has been banned");
        }

        // Validate password
        if (!passwordEncoder.matches(userLoginDto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        List<String> roles = getAuthoritiesForUser(user)
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        return jwtTokenizer.getAuthToken(
            user.getUsername(),
            roles,
            user.getId());

    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.debug("loadUserByUsername: {}", username);
        try {
            ApplicationUser user = findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username: " + username));

            if (user.isBanned()) {
                throw new DisabledException("Your account has been banned");
            }

            List<GrantedAuthority> grantedAuthorities = getAuthoritiesForUser(user);

            return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), grantedAuthorities);
        } catch (NotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<ApplicationUser> findUserByUsername(String username) {
        LOGGER.debug("Find application user by username: {}", username);
        return userRepository.findUserByUsername(username);
    }

    @Transactional
    @Override
    public void deleteUserByUsername(String username) {
        ApplicationUser user = userRepository.findUserByUsername(username).orElseThrow(() -> new NotFoundException("No user with username: " + username));

        // remove from push notifications
        pushSubscriptionRepository.deleteByUser(user);

        ApplicationUser deletedUser = userRepository.findUserByUsername("deleted user").orElseThrow();

        // swap user with deletedUser in ratings
        List<Rating> ratingsFrom = ratingRepository.findAllByFromUser(user);
        if (ratingsFrom != null) {
            for (Rating rating : ratingsFrom) {
                rating.setFromUser(deletedUser);
            }
        }

        List<Rating> ratingsTo = ratingRepository.findAllByToUser(user);
        if (ratingsTo != null) {
            for (Rating rating : ratingsTo) {
                rating.setToUser(deletedUser);
            }
        }

        // swap user with deletedUser in reports
        List<Report> reportsByReporter = reportRepository.findAllByReporter(user);
        if (reportsByReporter != null) {
            for (Report report : reportsByReporter) {
                report.setReporter(deletedUser);
            }
        }

        List<Report> reportsByTarget = reportRepository.findAllByTarget(user);
        if (reportsByTarget != null) {
            for (Report report : reportsByTarget) {
                report.setTarget(deletedUser);
            }
        }

        if (user.getRole() == Role.CUSTOMER) {

            // delete pending job requests and set deleted customer at other requests
            List<JobRequest> jobRequests = user.getJobRequests();
            if (jobRequests != null) {
                for (JobRequest jobRequest : jobRequests) {
                    jobRequest.setProperty(null);

                    if (jobRequest.getStatus() == JobStatus.PENDING) {
                        jobRequestRepository.delete(jobRequest);
                    } else {
                        // if accepted: set joboffer to done
                        if (jobRequest.getStatus() == JobStatus.ACCEPTED) {
                            jobRequest.setStatus(JobStatus.DONE);

                            List<JobOffer> receivedJobOffers = jobRequest.getReceivedJobOffers();
                            if (receivedJobOffers != null) {
                                for (JobOffer receivedJobOffer : receivedJobOffers) {
                                    if (receivedJobOffer.getStatus() == JobOfferStatus.ACCEPTED) {
                                        receivedJobOffer.setStatus(JobOfferStatus.DONE);
                                    }
                                }
                            }
                        }
                        jobRequest.setCustomer(deletedUser);
                    }
                }
            }

            // delete properties
            List<Property> properties = user.getProperties();
            if (properties != null) {
                propertyRepository.deleteAll(properties);
            }

            // set deleted user at chats and chat messages
            List<Chat> customerChats = user.getCustomerChats();
            if (customerChats != null) {
                for (Chat customerChat : customerChats) {
                    customerChat.setCustomer(deletedUser);

                    List<ChatMessage> chatMessages = customerChat.getChatMessages();
                    if (chatMessages != null) {
                        for (ChatMessage chatMessage : chatMessages) {
                            if (chatMessage.getSender() != null && chatMessage.getSender().equals(user)) {
                                chatMessage.setSender(deletedUser);
                            }
                        }
                    }
                }
            }

        } else if (user.getRole() == Role.WORKER) {

            // set deleted user at job offers
            List<JobOffer> sentJobOffers = user.getSentJobOffers();
            if (sentJobOffers != null) {
                for (JobOffer sentJobOffer : sentJobOffers) {
                    sentJobOffer.setWorker(deletedUser);
                }
            }

            // set deleted user at licenses
            List<License> licenses = user.getLicenses();
            if (licenses != null) {
                for (License license : licenses) {
                    license.setWorker(deletedUser);
                }
            }

            // set deleted user at chats and chat messages
            List<Chat> workerChats = user.getWorkerChats();
            if (workerChats != null) {
                for (Chat workerChat : workerChats) {
                    workerChat.setWorker(deletedUser);

                    List<ChatMessage> chatMessages = workerChat.getChatMessages();
                    if (chatMessages != null) {
                        for (ChatMessage chatMessage : chatMessages) {
                            if (chatMessage.getSender() != null && chatMessage.getSender().equals(user)) {
                                chatMessage.setSender(deletedUser);
                            }
                        }
                    }
                }
            }
        }

        userRepository.delete(user);
    }

    private List<GrantedAuthority> getAuthoritiesForUser(ApplicationUser user) {
        if (user.getRole() == Role.ADMIN) {
            return AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_CUSTOMER", "ROLE_WORKER");
        } else if (user.getRole() == Role.CUSTOMER) {
            return AuthorityUtils.createAuthorityList("ROLE_CUSTOMER");
        } else {
            return AuthorityUtils.createAuthorityList("ROLE_WORKER");
        }
    }


    /**
     * Updates a user's profile information based on the provided DTO.
     * This method assumes the DTO contains the username of the user to be updated.
     * Access control is enforced: only the user themselves or an ADMIN can perform the update.
     *
     * @param userUpdate The DTO containing the new information, including the username of the user to update.
     * @return The updated user information as a DTO.
     */
    @Override
    public UserUpdateDto update(UserUpdateDto userUpdate) {
        LOGGER.trace("update() called for DTO: {}", userUpdate);

        ApplicationUser currentUser = getCurrentUser();

        ApplicationUser userToUpdate = userRepository.findUserByUsername(userUpdate.getUsername())
            .orElseThrow(() -> new NotFoundException("Target user not found: " + userUpdate.getUsername()));

        if (!currentUser.getId().equals(userToUpdate.getId()) && currentUser.getRole() != Role.ADMIN) {
            LOGGER.warn("User '{}' (ID: {}) attempted to update user '{}' (ID: {}) without permission.",
                currentUser.getUsername(), currentUser.getId(), userToUpdate.getUsername(), userToUpdate.getId());
            throw new AccessDeniedException("You are not authorized to update this user's profile.");
        }

        String oldCountryCode = userToUpdate.getCountryCode();
        String oldPostalCode = userToUpdate.getPostalCode();
        String oldArea = userToUpdate.getArea();

        userMapper.updateUserFromDto(userUpdate, userToUpdate);

        boolean locationChanged = !Objects.equals(oldCountryCode, userToUpdate.getCountryCode())
            || !Objects.equals(oldPostalCode, userToUpdate.getPostalCode())
            || !Objects.equals(oldArea, userToUpdate.getArea());


        if (locationChanged && userToUpdate.getRole() == Role.WORKER
            && userToUpdate.getPostalCode() != null && !userToUpdate.getPostalCode().isBlank()
            && userToUpdate.getCountryCode() != null && !userToUpdate.getCountryCode().isBlank()) {
            LOGGER.info("Location data has changed for worker {}. Updating geocoding data.", userToUpdate.getUsername());
            updateUserWithGeonamesData(userToUpdate);
        } else if (locationChanged && userToUpdate.getRole() == Role.WORKER) {
            LOGGER.warn("Location data changed for worker {} but postal code or country code is missing. Skipping geocoding.", userToUpdate.getUsername());
            userToUpdate.setLatitude(null);
            userToUpdate.setLongitude(null);
        }

        userRepository.save(userToUpdate);
        return userUpdate;
    }


    @Override
    public UserUpdateDetailDto getUserByUserNameForEdit(String username) {
        LOGGER.trace("getCustomerByUsername() with parameters: {}", username);
        ApplicationUser user = userRepository.findUserByUsername(username)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.applicationUserToUserUpdateDetailDto(user);
    }

    @Override
    public UserDetailDto getUserDetailsById(Long id) {
        LOGGER.trace("getUserDetailsByUserName() with parameters: {}", id);
        ApplicationUser user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.applicationUserToUserDetailDto(user);
    }

    @Override
    public ApplicationUser getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return this.findUserByUsername(username).orElseThrow(() -> new NotFoundException("User not found: " + username));
    }

    @Override
    public PageDto<UserListDto> getAllUsersByPage(int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("username").ascending());
        Page<ApplicationUser> userPage = userRepository.findByRoleNotAndUsernameNot(Role.ADMIN, "deleted user", pageable);
        List<UserListDto> userListDto = userMapper.applicationUsersToUserListDto(userPage.getContent());
        return new PageDto<>(userListDto, (int) userPage.getTotalElements(), limit, offset);
    }


    @Override
    public void banUser(Long id) {
        ApplicationUser user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        user.setBanned(true);
        userRepository.save(user);
    }

    @Override
    public void unbanUser(Long id) {
        ApplicationUser user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        user.setBanned(false);
        userRepository.save(user);
    }

    @Override
    public PageDto<UserListDto> searchUsers(String usernamePart, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("username").ascending());
        Page<ApplicationUser> userPage = userRepository.findByUsernameContainingIgnoreCaseAndRoleNotAndUsernameNot(
            usernamePart,
            Role.ADMIN,
            "deleted user",
            pageable
        );
        List<UserListDto> userListDto = userMapper.applicationUsersToUserListDto(userPage.getContent());
        return new PageDto<>(userListDto, (int) userPage.getTotalElements(), limit, offset);
    }

    private void updateUserWithGeonamesData(ApplicationUser user) {
        try {
            String jsonResponse = fetchGeonamesDataFromApi(user.getPostalCode(), user.getCountryCode(), user.getArea());
            if (jsonResponse != null) {
                GeonamesApiResponse apiResponse = objectMapper.readValue(jsonResponse, GeonamesApiResponse.class);

                if (apiResponse != null && apiResponse.getResults() != null && !apiResponse.getResults().isEmpty()) {
                    var result = apiResponse.getResults().getFirst();
                    if (result != null) {
                        user.setLatitude(result.getLatitude());
                        user.setLongitude(result.getLongitude());
                        LOGGER.info("Geocoded user for zipcode {}: area='{}', lat={}, lon={}",
                            user.getPostalCode(), user.getArea(), user.getLatitude(), user.getLongitude());
                    }
                } else {
                    LOGGER.warn("Geonames API returned no records for zipcode: {} and country: {}", user.getPostalCode(), user.getCountryCode());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to parse Geonames API response for zipcode {}: {}", user.getPostalCode(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during geocoding for zipcode {}: {}", user.getPostalCode(), e.getMessage());
        }
    }

    private String fetchGeonamesDataFromApi(String postalCode, String countryCode, String area) throws IOException {
        String whereClause;
        // Improved null safety for the area parameter
        if (area == null || area.isBlank()) {
            whereClause = String.format("postal_code='%s' and country_code='%s'", postalCode, countryCode);
        } else {
            whereClause = String.format("postal_code='%s' and country_code='%s' and place_name='%s'", postalCode, countryCode, area);
        }

        String url = UriComponentsBuilder
            .fromHttpUrl("https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/geonames-postal-code@public/records")
            .queryParam("where", whereClause)
            .build()
            .toUriString();

        LOGGER.info("Calling Geonames API: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            LOGGER.warn("Could not fetch data from Geonames API for postalCode {}: {} {}", postalCode, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        }
    }
}
