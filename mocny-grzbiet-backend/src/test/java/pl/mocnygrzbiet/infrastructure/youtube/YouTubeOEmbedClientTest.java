package pl.mocnygrzbiet.infrastructure.youtube;

import org.junit.jupiter.api.Test;
import pl.mocnygrzbiet.domain.training.YouTubeMetadata;
import pl.mocnygrzbiet.domain.training.YouTubeMetadata.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Parsing only — fetch() goes over the network and stays out of unit tests. A missing or JSON-null
 * field has to come back as Java null, not as an empty string or the text "null": that is what
 * decides between a title the form pre-fills and "YouTube did not tell us".
 */
class YouTubeOEmbedClientTest {

    @Test
    void shouldReadTitleAndAuthorWhenBothArePresent() {
        YouTubeMetadata meta = YouTubeOEmbedClient.parse(
            "{\"title\":\"Martwy ciąg — technika\",\"author_name\":\"Mocny Grzbiet\",\"type\":\"video\"}");

        assertEquals(Status.OK, meta.status());
        assertEquals("Martwy ciąg — technika", meta.title());
        assertEquals("Mocny Grzbiet", meta.authorName());
    }

    @Test
    void shouldLeaveAuthorEmptyWhenItIsMissingOrNull() {
        assertNull(YouTubeOEmbedClient.parse("{\"title\":\"Przysiad\"}").authorName());
        assertNull(YouTubeOEmbedClient.parse("{\"title\":\"Przysiad\",\"author_name\":null}").authorName());
    }

    @Test
    void shouldReportUnknownWhenTitleIsMissingNullOrBlank() {
        assertEquals(Status.UNKNOWN, YouTubeOEmbedClient.parse("{\"author_name\":\"x\"}").status());
        assertEquals(Status.UNKNOWN, YouTubeOEmbedClient.parse("{\"title\":null}").status());
        assertEquals(Status.UNKNOWN, YouTubeOEmbedClient.parse("{\"title\":\"   \"}").status());
    }
}
