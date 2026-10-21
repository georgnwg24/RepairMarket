package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.repository.ChatMessageRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestImageRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.LicenseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.MessageType;
import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


/**
 * This component is only instantiated when the {@code datagen} profile is active.
 * It populates the database with test data upon initialization.
 * Activate this profile by adding {@code -Dspring.profiles.active=datagen} to your runtime arguments.
 */
@Component
@Profile("datagen")
public class DataGenerator implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private record Geolocation(Double latitude, Double longitude, String area) {
        public static Geolocation empty() {
            return new Geolocation(null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeonamesApiResponse {
        private List<GeonamesResult> results;

        public List<GeonamesResult> getResults() {
            return results;
        }

        public void setResults(List<GeonamesResult> results) {
            this.results = results;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeonamesResult {
        @JsonProperty("place_name")
        private String placeName;
        private Coordinates coordinates;

        public String getPlaceName() {
            return placeName;
        }

        public Coordinates getCoordinates() {
            return coordinates;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Coordinates {
        private double lat;
        private double lon;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }
    }

    private static final String INSERT_USER_SQL =
        "INSERT INTO app_user (id, username, password, email, role, first_name, last_name, country_code, postal_code, area, latitude, longitude, banned) "
            + "VALUES (:id, :username, :password, :email, :role, :firstName, :lastName, :countryCode, :postalCode, :area, :latitude, :longitude, :banned)";

    private static final String INSERT_PROPERTY_SQL =
        "INSERT INTO property (id, customer_id, area, address, country_code, postal_code, latitude, longitude) "
            + "VALUES (:id, :customerId, :area, :address, :countryCode, :postalCode, :latitude, :longitude)";

    private static final String INSERT_JOB_REQUEST_SQL =
        "INSERT INTO job_request (id, customer_id, property_id, title, description, category, deadline, status, created_at) "
            + "VALUES (:id, :customerId, :propertyId, :title, :description, :category, :deadline, :status, NOW())";

    private static final String INSERT_JOB_REQUEST_IMAGE_SQL =
        "INSERT INTO job_request_image (id, job_request_id, image, image_type, display_position) "
            + "VALUES (:id, :jobRequestId, :image, :imageType, :displayPosition)";

    private static final String INSERT_LICENSE_SQL =
        "INSERT INTO license (id, worker_id, filename, description, file, media_type, status, upload_time) "
            + "VALUES (:id, :workerId, :filename, :description, :file, :mediaType, :status, :uploadTime)";

    private static final String INSERT_JOB_OFFER_SQL =
        "INSERT INTO job_offer (id, job_request_id, worker_id, price, comment, status, created_at) "
            + "VALUES (:id, :jobRequestId, :workerId, :price, :comment, :status, NOW())";

    private static final String INSERT_RATING_SQL =
        "INSERT INTO rating (id, from_user_id, to_user_id, job_request_id, stars, comment, created_at) "
            + "VALUES (:id, :fromUserId, :toUserId, :jobRequestId, :stars, :comment, :createdAt)";

    private static final String INSERT_CHAT_SQL =
        "INSERT INTO chat (id, customer_id, worker_id, job_request_id, created_at) "
            + "VALUES (:id, :customerId, :workerId, :jobRequestId, :createdAt)";

    private static final String INSERT_CHAT_MESSAGE_SQL =
        "INSERT INTO chat_message (id, chat_id, sender_id, message_type, message, media_name, media_url, read, edited, timestamp) "
            + "VALUES (:id, :chatId, :senderId, :messageType, :message, :mediaName, :mediaUrl, :read, :edited, :timestamp)";

    private static final String INSERT_REPORT_SQL =
        "INSERT INTO report (id, reporter_id, target_id, job_request_id, chat_message_id, type, reason, is_open, reported_at) "
            + "VALUES (:id, :reporterId, :targetId, :jobRequestId, :chatMessage, :type, :reason, :isOpen, :reportedAt)";

    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;
    private final JobRequestRepository jobRequestRepository;
    private final JobRequestImageRepository jobRequestImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final PropertyRepository propertyRepository;
    private final LicenseRepository licenseRepository;
    private final RatingRepository ratingRepository;
    private final ReportRepository reportRepository;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DataGenerator(UserRepository userRepository,
                         JobOfferRepository jobOfferRepository,
                         JobRequestRepository jobRequestRepository,
                         JobRequestImageRepository jobRequestImageRepository,
                         EntityManager entityManager,
                         PasswordEncoder passwordEncoder,
                         PropertyRepository propertyRepository,
                         LicenseRepository licenseRepository,
                         RatingRepository ratingRepository,
                         ChatRepository chatRepository,
                         ChatMessageRepository chatMessageRepository,
                         ReportRepository reportRepository,
                         ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.jobRequestRepository = jobRequestRepository;
        this.jobRequestImageRepository = jobRequestImageRepository;
        this.propertyRepository = propertyRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.licenseRepository = licenseRepository;
        this.ratingRepository = ratingRepository;
        this.reportRepository = reportRepository;
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        LOGGER.info("Starting data generation...");
        LOGGER.debug("Deleting existing test data with negative IDs...");

        ratingRepository.deleteAllByIdLessThan(0L);
        reportRepository.deleteAllByIdLessThan(0L);
        chatMessageRepository.deleteAllByIdLessThan(0L);
        chatRepository.deleteAllByIdLessThan(0L);
        jobOfferRepository.deleteAllByIdLessThan(0L);
        jobRequestImageRepository.deleteAllByIdLessThan(0L);
        jobRequestRepository.deleteAllByIdLessThan(0L);
        propertyRepository.deleteAllByIdLessThan(0L);
        licenseRepository.deleteAllByIdLessThan(0L);
        userRepository.deleteAllByIdLessThan(0L);

        LOGGER.info("Deletion of existing test data completed.");
        LOGGER.debug("Creating Data...");

        LOGGER.debug("Creating Users...");

        // Deleted User
        createUser(-100L, "deleted user", "123459876543216789", null, null, null, null, Role.CUSTOMER, "");

        // Customers (IDs -101 to -104)
        createUser(-101L, "marie", "12345678", "Marie", "Curie", "AT", "1020", Role.CUSTOMER, "marie@example.com");
        createUser(-102L, "ellen", "12345678", "Ellen", "Ripley", "AT", "6700", Role.CUSTOMER, "ellen@example.com");
        createUser(-103L, "frodo", "12345678", "Frodo", "Baggins", "AT", "5020", Role.CUSTOMER, "frodo@example.com");
        createUser(-104L, "ned", "12345678", "Eddard", "Stark", "AT", "1150", Role.CUSTOMER, "stark@example.com");
        createUser(-105L, "vince", "12345678", "Vincent", "Vega", "AT", "1140", Role.CUSTOMER, "vince@example.com");
        createUser(-106L, "troll", "12345678", "Troll", "Llolol", "AT", "1010", Role.CUSTOMER, "troll@example.com");
        createUser(-1007L, "romy", "12345678", "Romy", "Schneider", "AT", "3100", Role.CUSTOMER, "romy@example.com");
        createUser(-1008L, "micol", "12345678", "Michael", "Burg", "AT", "2700", Role.CUSTOMER, "mici@example.com");


        // Workers (IDs -9 to -12)
        createUser(-109L, "james", "12345678", "James", "Bond", "AT", "1010", Role.WORKER, "james@example.com");
        createUser(-110L, "sarah", "12345678", "Sarah", "Connor", "AT", "8010", Role.WORKER, "sarah@example.com");
        createUser(-111L, "ethan", "12345678", "Ethan", "Hunt", "AT", "5020", Role.WORKER, "ethan@example.com");

        // Admin (ID -117)
        createUser(-117L, "tom", "12345678", "Tom", "Riddle", "AT", "1040", Role.ADMIN, "tom@example.com");

        entityManager.flush();
        LOGGER.info("Users created.");

        LOGGER.debug("Creating Properties...");

        // Customer -101: (Vienna)
        createProperty(-101L, -101L, "Schönbrunner Schloßstraße 47, Schönbrunn Palace", "AT", "1130");
        createProperty(-102L, -101L, "Stephansplatz 3, St. Stephen's Cathedral", "AT", "1010");
        createProperty(-103L, -101L, "Schottenring 30, Austrian Parliament Building", "AT", "1010");

        // Customer -102: (Salzburg)
        createProperty(-104L, -102L, "Mirabellplatz 4, Mirabell Palace", "AT", "5020");

        // Customer -103: (Innsbruck)
        createProperty(-105L, -103L, "Herzog-Friedrich-Straße 15, Goldenes Dachl", "AT", "6020");

        // Customer -104: (Graz)
        createProperty(-106L, -104L, "Lendkai 1, Kunsthaus Graz", "AT", "8010");
        createProperty(-108L, -104L, "Eggenberger Allee 90, Schloss Eggenberg", "AT", "8020");

        // Customer -105: (Vienna)
        createProperty(-107L, -105L, "Singerstraße 16, Palais Neupauer‑Breuner", "AT", "1010");

        // Customer troll:
        createProperty(-109L, -106L, "Herrengasse 23, Palais Liechtenstein", "AT", "1010");

        // Customer -1007: (St Pölten)
        createProperty(-1110L, -1007L, "Rathausplatz 1, Rathaus St. Pölten", "AT", "3100");

        // Customer -1008: (Wiener NSt.)
        createProperty(-1111L, -1008L, "Domplatz 1, Wiener Neustädter Dom", "AT", "2700");


        entityManager.flush();
        LOGGER.info("Properties created.");

        LOGGER.debug("Creating JobRequests with images...");

        createJobRequest(-101L, -101L, -101L, "Small Bathroom Renovation Helper",
            "I’m looking for someone who knows their way around walls, floors, and pipes to give my little bathroom a fresh start. You’ll help knock "
                + "out the old surfaces, fit new wall panels and flooring, and reroute or replace pipes where needed. It’s a cozy space, so attention to detail and "
                + "a good eye for neat work really matter. If you’ve got the skills and a fair price, I’d love to hear from you.",
            Category.RENOVATION, "2026-07-25", JobStatus.PENDING);
        createJobRequestImage(-101L, -101L, "test-images/bathroomRen1.jpg", "image/jpeg", 0);
        createJobRequestImage(-102L, -101L, "test-images/bathroomRen2.jpg", "image/jpeg", 1);
        createJobRequestImage(-103L, -101L, "test-images/bathroomRen3.jpg", "image/jpeg", 2);
        createJobRequestImage(-104L, -101L, "test-images/bathroomRen4.jpg", "image/jpeg", 3);

        createJobRequest(-102L, -102L, -104L, "Friendly Laptop Reassembly Helper",
            "I popped open my laptop to swap a broken SSD and now I can’t figure out how to put it back together. I’m looking for someone who knows all "
                + "about laptop guts, who can come by, snap everything back into place and have it running again in no time. I’m happy to pay around €120 for the job, "
                + "let me know if you’re interested!",
            Category.ELECTRICAL, "2026-07-12", JobStatus.PENDING);
        createJobRequestImage(-105L, -102L, "test-images/laptop1.jpg", "image/jpeg", 0);
        createJobRequestImage(-106L, -102L, "test-images/laptop2.jpg", "image/jpeg", 1);
        createJobRequestImage(-107L, -102L, "test-images/laptop3.jpg", "image/jpeg", 2);
        createJobRequestImage(-108L, -102L, "test-images/laptop4.jpg", "image/jpeg", 3);

        createJobRequest(-103L, -102L, -104L, "Balcony Plant Sitter",
            "I’m looking for someone friendly and reliable to pop by my apartment ten days in a row to water, weed and generally keep my balcony garden "
                + "happy while I’m away. You won’t need any fancy gardening degree—just a bit of care, a watering can and a good eye for wilting leaves. I’ll give "
                + "you a quick walkthrough on where everything lives and how much each plant likes to drink. For ten days of daily visits I’d budget at least €200, "
                + "which works out to about €20 per visit.",
            Category.GARDENING, "2026-08-01", JobStatus.PENDING);
        createJobRequestImage(-109L, -103L, "test-images/balconyGarden1.png", "image/png", 0);
        createJobRequestImage(-110L, -103L, "test-images/balconyGarden2.jpg", "image/jpeg", 1);
        createJobRequestImage(-111L, -103L, "test-images/balconyGarden3.jpg", "image/jpeg", 2);

        createJobRequest(-104L, -101L, -102L, "Cable Repair",
            "Hey there, I need someone to finish up a messy temporary fix. Six weeks ago Verizon cut my electrical cable and just slapped a quick "
                + "above‑ground patch on it, but they still haven’t come back to bury the final cable. I’m looking for an electrician who can dig the trench, lay "
                + "the cable properly and make sure everything’s safe and neat.",
            Category.ELECTRICAL, "2026-07-15", JobStatus.PENDING);
        createJobRequestImage(-112L, -104L, "test-images/aboveGroundCable1.jpg", "image/jpeg", 0);
        createJobRequestImage(-113L, -104L, "test-images/aboveGroundCable2.jpg", "image/jpeg", 1);
        createJobRequestImage(-114L, -104L, "test-images/aboveGroundCable3.jpg", "image/jpeg", 2);
        createJobRequestImage(-115L, -104L, "test-images/aboveGroundCable4.jpg", "image/jpeg", 3);

        createJobRequest(-105L, -103L, -105L, "Help Needed to Erase a Dog Graffiti from My House Wall",
            "Hey there, I’ve got a little problem—someone spray‑painted a cartoonish dog on the outside of my wall and I’d love to have it cleaned off."
                + " I’m looking for someone who knows how to safely remove spray paint without damaging the surface underneath. It’d just be washing, scrubbing, "
                + "or maybe a bit of power‑washing, depending on what works best. If you’ve tackled graffiti before and can make my wall look good as new, please "
                + "get in touch.",
            Category.PAINTING, "2026-07-28", JobStatus.PENDING);
        createJobRequestImage(-116L, -105L, "test-images/dogGraffiti.jpg", "image/jpeg", 0);

        createJobRequest(-106L, -103L, -105L, "Kitchen Floor Cleaning Specialist",
            "I run a busy restaurant kitchen and I need help getting stubborn stains out of the floor. I’ve tried every cleaner and method I know, but "
                + "the marks just won’t budge. I’m looking for someone reliable who can finish the job efficiently and would be willing to pay €125.",
            Category.CLEANING, "2026-07-10", JobStatus.PENDING);
        createJobRequestImage(-117L, -106L, "test-images/dirtyFloor.jpg", "image/jpeg", 0);

        createJobRequest(-107L, -104L, -106L, "Pool Pipe Plumber Needed to Breathe Life Back into My Old Pool",
            "I’ve got a backyard pool that’s been sitting dry for two years and now its pipes need a total refresh. I’m looking for someone who knows "
                + "their way around pool plumbing, can swap out old pipes, check fittings for leaks and get everything flowing smoothly again. No need for fancy "
                + "pitches—just solid work, good tools and a can‑do attitude. I’m happy to pay around €800 for the complete job. Let me know if you’re up for it!",
            Category.PLUMBING, "2026-07-20", JobStatus.PENDING);
        createJobRequestImage(-118L, -107L, "test-images/pool.jpg", "image/jpeg", 0);

        createJobRequest(-108L, -106L, -109L, "This is SPAM!!!",
            "You have been spammed",
            Category.ELECTRICAL, "2026-07-20", JobStatus.PENDING);




        createJobRequest(-2000L, -1007L, -1110L, "Painting the Wall orange",
            "I always had these boring white walls in my room. Now i would like to have them painted in orange!!",
            Category.PAINTING, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-2000L, -2000L, "test-images/wallPainting.jpg", "image/jpeg", 0);

        createJobRequest(-2001L, -1007L, -1110L, "Ceiling cover up",
            "After some home renovation the pipes at the ceiling are left exposed. I need a carpenter to cover them up.",
            Category.CARPENTRY, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-2001L, -2001L, "test-images/ceiling.jpg", "image/jpeg", 0);

        createJobRequest(-2002L, -1007L, -1110L, "Wiring Up Solar Panels",
            "I got new solar panels on my roof but they are not jet connected to anything. I need an electrician for that purpose.",
            Category.ELECTRICAL, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-2002L, -2002L, "test-images/solarPanel.jpg", "image/jpeg", 0);

        createJobRequest(-3000L, -1008L, -1111L, "Help me demolish this old Current Tower ",
            "It shouldn't be connected to anything no more but i still would like a professional to handle the job.",
            Category.ELECTRICAL, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-3000L, -3000L, "test-images/cables.jpg", "image/jpeg", 0);

        createJobRequest(-3001L, -1008L, -1111L, "I need a Roof",
            "We had this rain cover build for outside but now we would like to but a roof on top of it so we don't get wet when it rains.",
            Category.ROOFING, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-3001L, -3001L, "test-images/rainCover.jpg", "image/jpeg", 0);

        createJobRequest(-3002L, -1008L, -1111L, "I got some Water Damage in my Cellar",
            "We had this rain cover build for outside but now we would like to but a roof on top of it so we don't get wet when it rains.",
            Category.CARPENTRY, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-3002L, -3002L, "test-images/waterDam.jpg", "image/jpeg", 0);

        createJobRequest(-3003L, -1008L, -1111L, "Replacing old Door",
            "Can you switch out my old door wit a new one? I would pay a lot of money.",
            Category.RENOVATION, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-3003L, -3003L, "test-images/door.jpg", "image/jpeg", 0);

        createJobRequest(-3004L, -1008L, -1111L, "Repairing my Car",
            "I had an accident. Now my car is broken. Can you fix it?",
            Category.OTHER, "2026-01-11", JobStatus.PENDING);
        createJobRequestImage(-3004L, -3004L, "test-images/car.jpg", "image/jpeg", 0);

        // For rating workers
        createJobRequest(-1101L, -101L, -101L, "Historic plumbing overhaul", "Replace original 19th-century piping system with modern equivalents", Category.PLUMBING, "2026-06-15", JobStatus.HIDDEN);
        createJobRequest(-1102L, -102L, -104L, "Cathedral lighting", "Install energy-efficient lighting system", Category.ELECTRICAL, "2026-03-01", JobStatus.HIDDEN);
        createJobRequest(-1103L, -103L, -105L, "Spa plumbing", "Replace thermal bath fixtures", Category.PLUMBING, "2026-01-10", JobStatus.HIDDEN);
        createJobRequest(-1104L, -104L, -106L, "Bell tower electronics repair", "Restore historical bell electronics", Category.ELECTRICAL, "2026-05-20", JobStatus.HIDDEN);

        // For rating customers and chat
        createJobRequest(-1105L, -101L, -101L, "Facade repainting", "Restore original color scheme to exterior walls and trim", Category.PAINTING, "2026-11-30", JobStatus.HIDDEN);
        createJobRequest(-1106L, -101L, -101L, "Stone cleaning", "Pressure wash exterior facade", Category.CLEANING, "2026-09-15", JobStatus.HIDDEN);
        createJobRequest(-1107L, -102L, -104L, "Pool maintenance", "Weekly chemical balance checks", Category.CLEANING, "2026-12-01", JobStatus.HIDDEN);
        createJobRequest(-1108L, -102L, -104L, "Repair wall paintings", "Repair cracked medieval paintings", Category.PAINTING, "2026-10-05", JobStatus.HIDDEN);

        entityManager.flush();
        LOGGER.info("JobRequests with images created.");

        LOGGER.debug("Creating JobOffers...");

        // JobOffers for Small Bathroom Renovation Helper (Request -101)
        createJobOffer(-201L, -101L, -109L, 550.0f,
            "I have extensive renovation experience and can start next week. Attention to detail guaranteed.",
            "PENDING");
        createJobOffer(-202L, -101L, -110L, 600.0f,
            "I specialize in small-bathroom makeovers and can complete it within 5 days. References available.",
            "PENDING");
        createJobOffer(-203L, -101L, -111L, 800.0f,
            "I would do a high quality renovation. If €800 is too much for you we can always use less expensive materials.",
            "PENDING");

        // JobOffers for Friendly Laptop Reassembly Helper (Request -102)
        createJobOffer(-205L, -102L, -111L, 120.0f,
            "I’ve reassembled dozens of laptops; I’ll bring all necessary tools and test it on-site.",
            "PENDING");

        // JobOffers for Balcony Plant Sitter (Request -103)
        createJobOffer(-206L, -103L, -110L, 200.0f,
            "Reliable plant-lover—daily check-ins, photos, and watering schedule included.",
            "PENDING");
        createJobOffer(-207L, -103L, -109L, 180.0f,
            "Happy to care for your plants—I’ll water, prune, and send daily updates.",
            "PENDING");

        // JobOffers for Cable Repair (Request -104)
        createJobOffer(-208L, -104L, -111L, 350.0f,
            "Certified electrician—will dig, re-lay, and test to ensure safety.",
            "PENDING");
        createJobOffer(-209L, -104L, -109L, 300.0f,
            "Quick and neat trenching service; includes post-work cleanup.",
            "PENDING");

        // JobOffers for Help Needed to Erase a Dog Graffiti (Request -105)
        createJobOffer(-210L, -105L, -110L, 150.0f,
            "Experienced in graffiti removal—gentle cleaning to protect your paint.",
            "PENDING");

        // JobOffers for Kitchen Floor Cleaning Specialist (Request -106)
        // no offers

        // JobOffers for Pool Pipe Plumber (Request -107)
        createJobOffer(-211L, -107L, -111L, 800.0f,
            "Pool plumbing specialist—full pipe replacement and leak testing included.",
            "PENDING");

        // For rating workers
        // worker -109L:
        createJobOffer(-1201L, -1101L, -109L, 500.00f, "Spa plumbing fixtures replaced", "HIDDEN");
        createJobOffer(-1202L, -1102L, -109L, 1490.00f, "Cathedral lighting installed", "HIDDEN");
        // worker -110L:
        createJobOffer(-1203L, -1103L, -110L, 700.00f, "Historic plumbing overhaul completed", "HIDDEN");
        createJobOffer(-1204L, -1104L, -110L, 970.00f, "Bell tower electronics restored", "HIDDEN");

        // For rating customers
        // -101L:
        createJobOffer(-1205L, -1105L, -109L, 500.00f, "Done", "HIDDEN");
        createJobOffer(-1206L, -1106L, -110L, 1490.00f, "Done", "HIDDEN");
        // -102L:
        createJobOffer(-1207L, -1107L, -109L, 500.00f, "Done", "HIDDEN");
        createJobOffer(-1208L, -1108L, -110L, 1490.00f, "Done", "HIDDEN");

        entityManager.flush();
        LOGGER.info("JobOffers created.");

        LOGGER.debug("Creating Ratings...");

        // Customers rating workers
        createRating(-1L, -101L, -109L, -1101L, 4, "Good plumbing work, but arrived late");
        createRating(-2L, -102L, -109L, -1102L, 5, "Executed perfectly");
        createRating(-3L, -103L, -110L, -1103L, 2, "Very unfriendly, had to do most of the work myself");
        createRating(-4L, -104L, -110L, -1104L, 3, "Overpriced!!");

        // Workers rating customers
        createRating(-5L, -109L, -101L, -1105L, 1, "Did not want to pay the full sum");
        createRating(-6L, -110L, -101L, -1106L, 3, "Very poor communication");
        createRating(-7L, -109L, -102L, -1107L, 2, "Very unfriendly, had to do most of the work myself");
        createRating(-8L, -110L, -102L, -1108L, 3, "Overpriced!!");

        entityManager.flush();
        LOGGER.info("Ratings created.");

        LOGGER.debug("Creating Chats and ChatMessages...");

        createChat(-100L, -101L, -109L, -1105L);
        createChatMessage(-100L, -100L, -109L, MessageType.TEXT,
            "Hello, I’m here about the facade repainting.", null, null, true, false, LocalDateTime.now().minusMinutes(10));
        createChatMessage(-99L, -100L, -101L, MessageType.TEXT,
            "Can you start next Monday?", null, null, true, true, LocalDateTime.now().minusMinutes(9));
        createChatMessage(-98L, -100L, -109L, MessageType.TEXT,
            "Yes.", null, null, true, false, LocalDateTime.now().minusMinutes(8));
        createChatMessage(-97L, -100L, -101L, MessageType.TEXT,
            "Great. Will you bring all the materials? Do I have to prepare anything? If so just let me know", null, null, true, false, LocalDateTime.now().minusMinutes(7));
        createChatMessage(-96L, -100L, -109L, MessageType.TEXT,
            "I will bring everything myself.", null, null, true, false, LocalDateTime.now().minusMinutes(6));
        createChatMessage(-95L, -100L, -101L, MessageType.TEXT,
            "Perfect. What’s your cost estimate?", null, null, true, true, LocalDateTime.now().minusMinutes(5));
        createChatMessage(-94L, -100L, -109L, MessageType.TEXT,
            "It’ll be €1200 for the full facade.", null, null, true, false, LocalDateTime.now().minusMinutes(4));
        createChatMessage(-93L, -100L, -101L, MessageType.TEXT,
            "That's way to much!!!", null, null, true, false, LocalDateTime.now().minusMinutes(3));

        createChat(-101L, -101L, -110L, -1106L);
        createChatMessage(-92L, -101L, -110L, MessageType.TEXT,
            "Hi, I saw your offer and wanted to ask you about some details.", null, null, true, false, LocalDateTime.now().minusMinutes(8));
        createChatMessage(-91L, -101L, -101L, MessageType.TEXT,
            "Sure, what’s the issue?", null, null, true, false, LocalDateTime.now().minusMinutes(7));
        createChatMessage(-90L, -101L, -110L, MessageType.TEXT,
            "How long will it take?", null, null, true, false, LocalDateTime.now().minusMinutes(6));
        createChatMessage(-89L, -101L, -110L, MessageType.TEXT,
            "I won't be home for the whole next month so i would like it to be done next week.", null, null, true, false, LocalDateTime.now().minusMinutes(5));
        createChatMessage(-88L, -101L, -101L, MessageType.TEXT,
            "I can come by tomorrow morning to check if thats possible.", null, null, true, false, LocalDateTime.now().minusMinutes(4));
        createChatMessage(-87L, -101L, -110L, MessageType.TEXT,
            "Tomorrow works. What time?", null, null, true, false, LocalDateTime.now().minusMinutes(3));
        createChatMessage(-86L, -101L, -101L, MessageType.TEXT,
            "How about 9AM?", null, null, true, false, LocalDateTime.now().minusMinutes(2));

        createChat(-102L, -102L, -109L, -1107L);
        createChatMessage(-85L, -102L, -109L, MessageType.TEXT,
            "Hello, I want my murals repaired.", null, null, true, false, LocalDateTime.now().minusMinutes(8));
        createChatMessage(-84L, -102L, -109L, MessageType.TEXT,
            "There are some peeling and small cracks in corners.", null, null, true, false, LocalDateTime.now().minusMinutes(7));
        createChatMessage(-83L, -102L, -102L, MessageType.TEXT,
            "I’ll need two days and special sealant.", null, null, true, false, LocalDateTime.now().minusMinutes(6));
        createChatMessage(-82L, -102L, -109L, MessageType.TEXT,
            "Sounds good. Can we talk about the price? €450 seems like a lot.", null, null, true, false, LocalDateTime.now().minusMinutes(5));
        createChatMessage(-81L, -102L, -102L, MessageType.TEXT,
            "€400 total. That's the lowest i can offer.", null, null, true, false, LocalDateTime.now().minusMinutes(4));

        entityManager.flush();
        LOGGER.info("Chats and ChatMessages created.");

        LOGGER.debug("Creating Reports...");

        // Closed Reports: (Long id, Long reporterId, Long targetId, Long jobRequestId, ReportType type, String reason, boolean isOpen)
        createReport(-100L, -111L, -102L, -103L, null, ReportType.JOB_REQUEST, "The description contains offensive language", false);
        createReport(-101L, -111L, -106L, -108L, null, ReportType.JOB_REQUEST, "I saw multiple spam-jobs like this one created by the same user.", true);
        createReport(-102L, -109L, -101L, -1105L, -93L, ReportType.MESSAGE, "He thinks im overpriced even though i made a fair offer!", true);


        entityManager.flush();
        LOGGER.info("Reports created.");

        LOGGER.debug("Creating Licenses...");

        // Worker -109:
        createLicense(-101L, -109L, "stark_engineering.pdf", "Advanced Mechanical Engineering License", "APPROVED");
        createLicense(-102L, -109L, "electrician_cert.pdf", "Certified Electrician License", "PENDING");

        // Worker -110:
        createLicense(-103L, -110L, "connor_plumbing.pdf", "Certified Plumbing & Pipefitting License", "APPROVED");
        createLicense(-105L, -110L, "not_valid.pdf", "I never got my Diploma", "REJECTED");

        // Worker -111:
        createLicense(-104L, -111L, "hunt_security.pdf", "Executive Protection Specialist Certification", "PENDING");

        entityManager.flush();
        LOGGER.info("Licenses created.");

        LOGGER.info("Data generation completed.");
    }

    private Geolocation geocode(String countryCode, String postalCode) {
        if (postalCode == null || countryCode == null) {
            return Geolocation.empty();
        }

        try {
            Thread.sleep(250);

            String whereClause = String.format("postal_code='%s' and country_code='%s'", postalCode, countryCode);

            String url = UriComponentsBuilder
                .fromHttpUrl("https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/geonames-postal-code@public/records")
                .queryParam("where", whereClause)
                .queryParam("limit", 1) // only need the first result
                .build()
                .toUriString();

            //LOGGER.info("Calling Geonames API: {}", url);
            String jsonResponse = restTemplate.getForObject(url, String.class);

            if (jsonResponse != null) {
                GeonamesApiResponse apiResponse = objectMapper.readValue(jsonResponse, GeonamesApiResponse.class);
                if (apiResponse != null && apiResponse.getResults() != null && !apiResponse.getResults().isEmpty()) {
                    GeonamesResult result = apiResponse.getResults().get(0);
                    if (result != null && result.getCoordinates() != null) {
                        return new Geolocation(result.getCoordinates().getLat(), result.getCoordinates().getLon(), result.getPlaceName());
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            LOGGER.warn("Could not fetch data from Geonames API for postalCode {}: {} {}", postalCode, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (IOException e) {
            LOGGER.error("Failed to parse Geonames API response for postalCode {}: {}", postalCode, e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted during geocoding delay", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during geocoding for postalCode {}: {}", postalCode, e.getMessage());
        }

        LOGGER.warn("Geocoding failed for postalCode {}, country {}'. No coordinates will be set.", postalCode, countryCode);
        return new Geolocation(null, null, null);
    }

    private void createUser(Long id, String username, String password, String firstName, String lastName, String countryCode,
                            String postalCode, Role role, String email) {
        Geolocation location = geocode(countryCode, postalCode);

        entityManager.createNativeQuery(INSERT_USER_SQL)
            .setParameter("id", id)
            .setParameter("username", username)
            .setParameter("password", passwordEncoder.encode(password))
            .setParameter("email", email)
            .setParameter("role", role.toString())
            .setParameter("firstName", firstName)
            .setParameter("lastName", lastName)
            .setParameter("countryCode", countryCode)
            .setParameter("postalCode", postalCode)
            .setParameter("area", location.area())
            .setParameter("latitude", location.latitude())
            .setParameter("longitude", location.longitude())
            .setParameter("banned", false)
            .executeUpdate();
    }

    private void createProperty(Long id, Long customerId, String address, String countryCode, String postalCode) {
        Geolocation location = geocode(countryCode, postalCode);

        entityManager.createNativeQuery(INSERT_PROPERTY_SQL)
            .setParameter("id", id)
            .setParameter("customerId", customerId)
            .setParameter("address", address)
            .setParameter("countryCode", countryCode)
            .setParameter("postalCode", postalCode)
            .setParameter("area", location.area())
            .setParameter("latitude", location.latitude())
            .setParameter("longitude", location.longitude())
            .executeUpdate();
    }

    private void createJobRequest(Long id, Long customerId, Long propertyId, String title,
                                  String description, Category category, String deadline, JobStatus status) {
        entityManager.createNativeQuery(INSERT_JOB_REQUEST_SQL)
            .setParameter("id", id)
            .setParameter("customerId", customerId)
            .setParameter("propertyId", propertyId)
            .setParameter("title", title)
            .setParameter("description", description)
            .setParameter("category", category.toString())
            .setParameter("deadline", LocalDate.parse(deadline))
            .setParameter("status", status.toString())
            .executeUpdate();
    }

    private void createJobRequestImage(Long id, Long jobRequestId, String imagePath, String imageType, int position) {
        entityManager.createNativeQuery(INSERT_JOB_REQUEST_IMAGE_SQL)
            .setParameter("id", id)
            .setParameter("jobRequestId", jobRequestId)
            .setParameter("image", loadImageAsBytes(imagePath))
            .setParameter("imageType", imageType)
            .setParameter("displayPosition", position)
            .executeUpdate();
    }

    private byte[] loadImageAsBytes(String imagePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(imagePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Image not found: " + imagePath);
            }
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image: " + imagePath, e);
        }
    }

    private void createLicense(Long id, Long workerId, String filename, String description, String status) {
        byte[] dummyFile = new byte[] {0x25, 0x50, 0x44, 0x46}; // PDF header bytes

        entityManager.createNativeQuery(INSERT_LICENSE_SQL)
            .setParameter("id", id)
            .setParameter("workerId", workerId)
            .setParameter("filename", filename)
            .setParameter("description", description)
            .setParameter("file", dummyFile)
            .setParameter("mediaType", "application/pdf")
            .setParameter("status", status)
            .setParameter("uploadTime", LocalDateTime.now())
            .executeUpdate();
    }

    private void createJobOffer(Long id, Long jobRequestId, Long workerId, float price, String comment, String status) {
        entityManager.createNativeQuery(INSERT_JOB_OFFER_SQL)
            .setParameter("id", id)
            .setParameter("jobRequestId", jobRequestId)
            .setParameter("workerId", workerId)
            .setParameter("price", price)
            .setParameter("comment", comment)
            .setParameter("status", status)
            .executeUpdate();
    }

    private void createRating(Long id, Long fromUserId, Long toUserId, Long jobRequestId, int stars, String comment) {
        entityManager.createNativeQuery(INSERT_RATING_SQL)
            .setParameter("id", id)
            .setParameter("fromUserId", fromUserId)
            .setParameter("toUserId", toUserId)
            .setParameter("jobRequestId", jobRequestId)
            .setParameter("stars", stars)
            .setParameter("comment", comment)
            .setParameter("createdAt", LocalDateTime.now())
            .executeUpdate();
    }

    private void createChat(Long id, Long customerId, Long workerId, Long jobRequestId) {
        entityManager.createNativeQuery(INSERT_CHAT_SQL)
            .setParameter("id", id)
            .setParameter("customerId", customerId)
            .setParameter("workerId", workerId)
            .setParameter("jobRequestId", jobRequestId)
            .setParameter("createdAt", LocalDateTime.now())
            .executeUpdate();
    }

    private void createChatMessage(Long id, Long chatId, Long senderId, MessageType messageType, String message, String mediaName,
                                   String mediaUrl, boolean read, boolean edited, LocalDateTime timestamp) {
        entityManager.createNativeQuery(INSERT_CHAT_MESSAGE_SQL)
            .setParameter("id", id)
            .setParameter("chatId", chatId)
            .setParameter("senderId", senderId)
            .setParameter("messageType", messageType.toString())
            .setParameter("message", message)
            .setParameter("mediaName", mediaName)
            .setParameter("mediaUrl", mediaUrl)
            .setParameter("read", read)
            .setParameter("edited", edited)
            .setParameter("timestamp", timestamp)
            .executeUpdate();
    }

    private void createReport(Long id, Long reporterId, Long targetId, Long jobRequestId, Long chatMessage, ReportType type, String reason, boolean isOpen) {
        entityManager.createNativeQuery(INSERT_REPORT_SQL)
            .setParameter("id", id)
            .setParameter("reporterId", reporterId)
            .setParameter("targetId", targetId)
            .setParameter("jobRequestId", jobRequestId)
            .setParameter("chatMessage", chatMessage)
            .setParameter("type", type.toString())
            .setParameter("reason", reason)
            .setParameter("isOpen", isOpen)
            .setParameter("reportedAt", LocalDateTime.now())
            .executeUpdate();
    }
}