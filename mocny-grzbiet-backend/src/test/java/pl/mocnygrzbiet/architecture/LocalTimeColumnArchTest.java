package pl.mocnygrzbiet.architecture;

import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every {@code LocalTime} column on an entity is bound with {@link LocalTimeJdbcType}.
 * <p>
 * {@code hibernate.jdbc.time_zone: UTC} is right for instants and wrong for a wall-clock hour: without
 * the annotation Hibernate stores 18:00 as 17:00 and undoes it on read, so the screen looks right while
 * every plain SQL read (GDPR export, change-mail queue) is an hour off. Found in manual testing;
 * {@code TrainingTimeStorageIntegrationTest} shows the effect, this gate keeps the next hour column
 * from repeating it.
 */
class LocalTimeColumnArchTest {

    @Test
    void shouldStoreEveryEntityHourWithoutATimeZoneShift() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> unbound = new ArrayList<>();
        int hourFields = 0;
        for (BeanDefinition entity : scanner.findCandidateComponents("pl.mocnygrzbiet")) {
            for (Field field : Class.forName(entity.getBeanClassName()).getDeclaredFields()) {
                if (field.getType() != LocalTime.class) {
                    continue;
                }
                hourFields++;
                JdbcType type = field.getAnnotation(JdbcType.class);
                if (type == null || type.value() != LocalTimeJdbcType.class) {
                    unbound.add(entity.getBeanClassName() + "." + field.getName());
                }
            }
        }

        // The scan must see the fields it guards, or the gate passes on nothing.
        assertTrue(hourFields >= 3, "expected the training and deletion hour fields, found " + hourFields);
        assertEquals(List.of(), unbound, "Add @JdbcType(LocalTimeJdbcType.class) to these LocalTime columns");
    }
}
