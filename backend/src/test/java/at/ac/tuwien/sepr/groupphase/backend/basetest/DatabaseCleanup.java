package at.ac.tuwien.sepr.groupphase.backend.basetest;

import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleanup {

    private final RatingRepository ratingRepository;
    private final JobOfferRepository jobOfferRepository;
    private final JobRequestRepository jobRequestRepository;
    private final UserRepository userRepository;
    private final LicenseRepository licenseRepository;
    private final PropertyRepository propertyRepository;
    private final ReportRepository reportRepository;
    private final JobRequestImageRepository jobRequestImageRepository;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;




    @Autowired
    public DatabaseCleanup(
        RatingRepository ratingRepository,
        JobOfferRepository jobOfferRepository,
        JobRequestRepository jobRequestRepository,
        UserRepository userRepository,
        LicenseRepository licenseRepository,
        PropertyRepository propertyRepository,
        ReportRepository reportRepository,
        JobRequestImageRepository jobRequestImageRepository,
        ChatRepository chatRepository,
        ChatMessageRepository chatMessageRepository
    ) {
        this.ratingRepository = ratingRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.jobRequestRepository = jobRequestRepository;
        this.userRepository = userRepository;
        this.licenseRepository = licenseRepository;
        this.propertyRepository = propertyRepository;
        this.reportRepository = reportRepository;
        this.jobRequestImageRepository = jobRequestImageRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRepository = chatRepository;
    }

    public void clearAll() {
        ratingRepository.deleteAll();
        jobRequestImageRepository.deleteAll();
        jobOfferRepository.deleteAll();
        reportRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRepository.deleteAll();
        jobRequestRepository.deleteAll();
        licenseRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
    }

}

