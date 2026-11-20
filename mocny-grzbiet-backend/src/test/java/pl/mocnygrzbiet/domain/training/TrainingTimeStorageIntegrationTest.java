package pl.mocnygrzbiet.domain.training;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.mocnygrzbiet.BaseIntegrationTest;
import pl.mocnygrzbiet.domain.user.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A training's hour is a wall-clock time and lands in the TIME column exactly as entered.
 * <p>
 * Found in manual testing: with {@code hibernate.jdbc.time_zone: UTC} Hibernate bound a {@code LocalTime}
 * through a UTC calendar, so 18:00 was stored as 17:00 (the JVM runs on Europe/Warsaw, as the test task
 * pins it). Reading back through Hibernate undid the shift, so the screen looked right — but every plain
 * SQL read was an hour early: the GDPR export handed the client wrong hours, and the change-mail queue,
 * written with plain JDBC, disagreed with the entities about the same training.
 */
class TrainingTimeStorageIntegrationTest extends BaseIntegrationTest {

    @Autowired private PersonalTrainingRepository trainings;
    @Autowired private TrainingDeletionRepository deletions;
    @Autowired private JdbcTemplate jdbc;

    private PersonalTraining training() {
        User user = new User("czas-" + UUID.randomUUID() + "@mocnygrzbiet.test", "Ala", "Testowa", null);
        user.markEmailVerified();
        user.setAthlete(true);
        user = userRepository.save(user);
        PersonalTraining t = new PersonalTraining(user, TrainingKind.TRAINING, LocalDate.of(2027, 3, 10), "Siła", true);
        t.edit(LocalDate.of(2027, 3, 10), LocalTime.of(18, 0), LocalTime.of(19, 30), "Siła", null, null, true);
        return trainings.saveAndFlush(t);
    }

    @Test
    void shouldStoreATrainingHourAsEntered() {
        PersonalTraining saved = training();

        assertEquals(LocalTime.of(18, 0), jdbc.queryForObject(
                "SELECT start_time FROM personal_trainings WHERE id = ?", LocalTime.class, saved.getId()));
        assertEquals(LocalTime.of(19, 30), jdbc.queryForObject(
                "SELECT end_time FROM personal_trainings WHERE id = ?", LocalTime.class, saved.getId()));
    }

    @Test
    void shouldStoreTheHourOfADeletedTrainingAsEntered() {
        TrainingDeletion deletion = deletions.saveAndFlush(new TrainingDeletion(training(), true));

        assertEquals(LocalTime.of(18, 0), jdbc.queryForObject(
                "SELECT start_time FROM training_deletions WHERE id = ?", LocalTime.class, deletion.getId()));
    }

    @Test
    void shouldReadBackAnHourWrittenWithPlainSql() {
        // The other direction: a row written outside Hibernate (as the change-mail queue does) must read
        // as the same hour through the entity.
        PersonalTraining saved = training();
        jdbc.update("UPDATE personal_trainings SET start_time = '07:15' WHERE id = ?", saved.getId());

        assertEquals(LocalTime.of(7, 15), trainings.findById(saved.getId()).orElseThrow().getStartTime());
    }
}
