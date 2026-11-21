package pl.mocnygrzbiet.api.dev;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.mocnygrzbiet.config.AdminEmailConfig;
import pl.mocnygrzbiet.domain.training.*;
import pl.mocnygrzbiet.domain.user.User;
import pl.mocnygrzbiet.domain.user.UserRepository;
import pl.mocnygrzbiet.domain.user.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The seeder can leave out the demo roster, so a manual run through the test instruction starts
 * where the coach starts on a fresh server: his own account and nothing else. Found in manual
 * testing: the instruction has the tester invite Ania, Kasia and Ola, and the seeded accounts with
 * the same addresses were already there.
 */
class DevDataSeederTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AdminEmailConfig adminEmails = mock(AdminEmailConfig.class);
    private final PersonalTrainingRepository trainings = mock(PersonalTrainingRepository.class);
    private final ExerciseVideoRepository videos = mock(ExerciseVideoRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private DevDataSeeder seeder(boolean demoData) {
        when(adminEmails.getAdminEmails()).thenReturn(Set.of("coach@example.test"));
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hash");
        // A non-empty library skips the plan, which these mocks would not survive.
        when(videos.count()).thenReturn(1L);
        return new DevDataSeeder(users, encoder, adminEmails, trainings, mock(TrainingCommentRepository.class),
                mock(AthleteGoalRepository.class), videos, mock(AthleteMaterialRepository.class),
                mock(MaterialQuestionRepository.class), mock(CoachDayOffRepository.class),
                mock(TrainingTemplateRepository.class), jdbc, demoData);
    }

    private List<String> savedEmails() {
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users, atLeastOnce()).save(saved.capture());
        return saved.getAllValues().stream().map(User::getEmail).distinct().toList();
    }

    @Test
    void shouldSeedOnlyTheCoachWhenDemoDataIsOff() {
        seeder(false).run();

        assertEquals(List.of("coach@example.test"), savedEmails());
        verifyNoInteractions(trainings, jdbc);
    }

    @Test
    void shouldGiveTheCoachAnAdminAccountWhenDemoDataIsOff() {
        seeder(false).run();

        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals(UserRole.ADMIN, saved.getValue().getRole());
    }

    @Test
    void shouldSeedTheRosterByDefault() {
        seeder(true).run();

        List<String> emails = savedEmails();
        assertEquals(8, emails.size(), "coach plus seven test accounts, found " + emails);
    }
}
