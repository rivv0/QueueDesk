import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TicketJsonStore {
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private TicketJsonStore() {}

    public static void save(List<Ticket> tickets, Path file) throws InvalidTicketDataException {
        try {
            mapper.writeValue(file.toFile(), tickets);
        } catch (IOException e) {
            throw new InvalidTicketDataException("Failed to save tickets to JSON: " + e.getMessage(), e);
        }
    }

    public static List<Ticket> load(Path file) throws InvalidTicketDataException {
        try {
            return mapper.readValue(file.toFile(), new TypeReference<List<Ticket>>() {});
        } catch (IOException e) {
            throw new InvalidTicketDataException("Failed to load tickets from JSON: " + e.getMessage(), e);
        }
    }
}
