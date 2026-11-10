import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TicketFileHandler {
    public static void save(List<Ticket> tickets, Path file) throws IOException {
        TicketFileLoader.save(tickets, file);
    }

    public static List<Ticket> load(Path file) throws IOException, InvalidTicketDataException {
        return TicketFileLoader.load(file);
    }

    public static void generateReport(List<Ticket> tickets, Path output) throws IOException {
        TicketFileLoader.generateReport(tickets, output);
    }
}
