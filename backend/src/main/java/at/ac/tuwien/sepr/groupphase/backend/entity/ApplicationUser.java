package at.ac.tuwien.sepr.groupphase.backend.entity;

import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Represents a user in the persistent data store.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobRequests", "properties", "customerChats", "sentJobOffers", "licenses", "workerChats", "ratingsReceived", "password", "pushSubscriptions"})
@EqualsAndHashCode(exclude = {"jobRequests", "properties", "customerChats", "sentJobOffers", "licenses", "workerChats", "ratingsReceived", "pushSubscriptions"})
public class ApplicationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(name = "banned", nullable = false)
    private boolean banned = false;

    private String firstName;

    private String lastName;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<PushSubscription> pushSubscriptions;

    // customer only
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
    @OrderBy("createdAt DESC")
    private List<JobRequest> jobRequests;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
    private List<Property> properties;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
    @OrderBy("createdAt DESC")
    private List<Chat> customerChats;

    // worker only
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "worker")
    @OrderBy("createdAt DESC")
    private List<JobOffer> sentJobOffers;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "worker")
    @OrderBy("uploadTime DESC")
    private List<License> licenses;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "worker")
    @OrderBy("createdAt DESC")
    private List<Chat> workerChats;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "toUser")
    @OrderBy("createdAt DESC")
    private List<Rating> ratingsReceived;

    private String countryCode;
    private String postalCode;
    private String area;
    private Float latitude;
    private Float longitude;
}
