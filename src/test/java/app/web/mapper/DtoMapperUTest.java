package app.web.mapper;

import app.user.model.User;
import app.web.dto.UserEditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DtoMapperUTest {

    @Test
    void givenHappyPath_whenMappingUserToUserEditRequest(){

        // Given
        User user = User.builder()
                .firstName("Vik")
                .lastName("Aleksandrov")
                .email("vik123@abv.bg")
                .profilePicture("www.image.com")
                .build();

        // When
        UserEditRequest resultDto = DtoMapper.mapUserToUserEditRequest(user);

        // Then
        assertEquals(user.getFirstName(), resultDto.getFirstName());
        assertEquals(user.getLastName(), resultDto.getLastName());
        assertEquals(user.getEmail(), resultDto.getEmail());
        assertEquals(user.getProfilePicture(), resultDto.getProfilePicture());
    }
}
