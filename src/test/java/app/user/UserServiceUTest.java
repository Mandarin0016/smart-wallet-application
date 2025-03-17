package app.user;

import app.exception.DomainException;
import app.exception.UsernameAlreadyExistException;
import app.notification.service.NotificationService;
import app.security.AuthenticationMetadata;
import app.subscription.model.Subscription;
import app.subscription.service.SubscriptionService;
import app.user.model.Country;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.RegisterRequest;
import app.web.dto.UserEditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 1. Create the test class
// 2. Annotate the class with @ExtendWith(MockitoExtension.class)
// 3. Get the class you want to test
// 4. Get all dependencies of that class and annotate them with @Mock
// 5. Inject all those dependencies to the class we test with annotation @InjectMocks

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private WalletService walletService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    @ParameterizedTest
    @MethodSource("userRolesArguments")
    void whenChangeUserRole_theCorrectRoleIsAssigned(UserRole currentUserRole, UserRole expectedUserRole) {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .role(currentUserRole)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchRole(userId);

        // Then
        assertEquals(expectedUserRole, user.getRole());
    }

    private static Stream<Arguments> userRolesArguments() {

        return Stream.of(
                Arguments.of(UserRole.USER, UserRole.ADMIN),
                Arguments.of(UserRole.ADMIN, UserRole.USER)
        );
    }

    @Test
    void givenExistingUsersInDatabase_whenGetAllUsers_thenReturnThemAll() {

        // Give
        List<User> userList = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(userList);

        // When
        List<User> users = userService.getAllUsers();

        // Then
        assertThat(users).hasSize(2);
    }

    // Switch status method
    @Test
    void givenUserWithStatusActive_whenSwitchStatus_thenUserStatusBecomeInactive() {

        // Given
        User user = User.builder()
                .id(UUID.randomUUID())
                .isActive(true)
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // When
        userService.switchStatus(user.getId());

        // Then
        assertFalse(user.isActive());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenUserWithStatusInactive_whenSwitchStatus_thenUserStatusBecomeActive() {

        // Given
        User user = User.builder()
                .id(UUID.randomUUID())
                .isActive(false)
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // When
        userService.switchStatus(user.getId());

        // Then
        assertTrue(user.isActive());
        verify(userRepository, times(1)).save(user);
    }

    // Register
    // Test 1: When user exist with this username -> exception is thrown
    @Test
    void givenExistingUsername_whenRegister_thenExceptionIsThrown() {

        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("Vik123")
                .password("123123")
                .country(Country.BULGARIA)
                .build();
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(new User()));

        // When & Then
        assertThrows(UsernameAlreadyExistException.class, () -> userService.register(registerRequest));
        verify(userRepository, never()).save(any());
        verify(subscriptionService, never()).createDefaultSubscription(any());
        verify(walletService, never()).initilizeFirstWallet(any());
        verify(notificationService, never()).saveNotificationPreference(any(UUID.class), anyBoolean(), anyString());
    }

    // Test 2: Happy path Registration
    @Test
    void givenHappyPath_whenRegister() {

        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("Vik123")
                .password("123123")
                .country(Country.BULGARIA)
                .build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .build();
        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        when(subscriptionService.createDefaultSubscription(user)).thenReturn(new Subscription());
        when(walletService.initilizeFirstWallet(user)).thenReturn(new Wallet());

        // When
        User registeredUser = userService.register(registerRequest);

        // Then
        assertThat(registeredUser.getSubscriptions()).hasSize(1);
        assertThat(registeredUser.getWallets()).hasSize(1);
        verify(notificationService, times(1)).saveNotificationPreference(user.getId(), false, null);
    }

    // Test 2: When User does not exist - then throws exception
    @Test
    void givenMissingUserFromDatabase_whenLoadUserByUsername_thenExceptionIsThrown() {

        // Given
        String username = "Vik123";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DomainException.class, () -> userService.loadUserByUsername(username));
    }

    // Test 1: When user exist - then return new AuthenticationMetadata
    @Test
    void givenExistingUser_whenLoadUserByUsername_thenReturnCorrectAuthenticationMetadata() {

        // Given
        String username = "Vik123";
        User user = User.builder()
                .id(UUID.randomUUID())
                .isActive(true)
                .password("123123")
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        UserDetails authenticationMetadata = userService.loadUserByUsername(username);

        // Then
        assertInstanceOf(AuthenticationMetadata.class, authenticationMetadata);
        AuthenticationMetadata result = (AuthenticationMetadata) authenticationMetadata;
        assertEquals(user.getId(), result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(user.getPassword(), result.getPassword());
        assertEquals(user.isActive(), result.isActive());
        assertEquals(user.getRole(), result.getRole());
        assertThat(result.getAuthorities()).hasSize(1);
        assertEquals("ROLE_ADMIN", result.getAuthorities().iterator().next().getAuthority());
    }

    // Test Case: When there is no user in the database (repository returns Optional.empty()) -
    // then expect an exception of type DomainException is thrown
    @Test
    void givenMissingUserFromDatabase_whenEditUserDetails_thenExceptionIsThrown() {

        UUID userId = UUID.randomUUID();
        UserEditRequest dto = UserEditRequest.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> userService.editUserDetails(userId, dto));
    }

    // Test Case: When database returns user object -> then change their details from the dto with email address
    // and save notification preference and save the user to the database
    @Test
    void givenExistingUser_whenEditTheirProfileWithActualEmail_thenChangeTheirDetailsSaveNotificationPreferenceAndSaveToDatabase() {

        // Given
        UUID userId = UUID.randomUUID();
        UserEditRequest dto = UserEditRequest.builder()
                .firstName("Viktor")
                .lastName("Aleksandrov")
                .email("vik123@abv.bg")
                .profilePicture("www.image.com")
                .build();
        User user = User.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.editUserDetails(userId, dto);

        // Then
        assertEquals("Viktor", user.getFirstName());
        assertEquals("Aleksandrov", user.getLastName());
        assertEquals("vik123@abv.bg", user.getEmail());
        assertEquals("www.image.com", user.getProfilePicture());
        verify(notificationService, times(1)).saveNotificationPreference(userId, true, dto.getEmail());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenExistingUser_whenEditTheirProfileWithEmptyEmail_thenChangeTheirDetailsSaveNotificationPreferenceAndSaveToDatabase() {

        // Given
        UUID userId = UUID.randomUUID();
        UserEditRequest dto = UserEditRequest.builder()
                .firstName("Viktor")
                .lastName("Aleksandrov")
                .email("")
                .profilePicture("www.image.com")
                .build();
        User user = User.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.editUserDetails(userId, dto);

        // Then
        assertEquals("Viktor", user.getFirstName());
        assertEquals("Aleksandrov", user.getLastName());
        assertEquals("", user.getEmail());
        assertEquals("www.image.com", user.getProfilePicture());
        verify(notificationService, times(1)).saveNotificationPreference(userId, false, null);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenUserWithRoleAdmin_whenSwitchRole_thenUserReceivesUserRole() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchRole(userId);

        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void givenUserWithRoleUser_whenSwitchRole_thenUserReceivesAdminRole() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(UserRole.USER)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchRole(userId);

        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }
}
