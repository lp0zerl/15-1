package repository;

import org.springframework.stereotype.Repository;
import ru.hogwarts.school.UserProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUsername(String username);
    List<UserProfile> findByFirstNameAndLastName(String firstName, String lastName);
    Optional<UserProfile> findByTelegramId(String telegramId);
}
