import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Repository<T extends Ticket> {
    private final List<T> tickets = new ArrayList<>();

    public void add(T ticket) {
        tickets.add(ticket);
    }

    public List<T> all() {
        return Collections.unmodifiableList(tickets);
    }

    public Optional<T> findByIdSafe(int id) {
        for (T ticket : tickets) {
            if (ticket.getId() == id) {
                return Optional.of(ticket);
            }
        }
        return Optional.empty();
    }

    public T findById(int id) {
        return findByIdSafe(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Map<String, List<T>> groupByAssignee() {
        return tickets.stream().collect(Collectors.groupingBy(
                t -> t.getAssignee() == null || t.getAssignee().isBlank() ? "Unassigned" : t.getAssignee()));
    }
}
