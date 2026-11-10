import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public class Main {
    public static <T extends Ticket> T oldest(List<T> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return null;
        }
        T oldestTicket = tickets.get(0);
        for (T ticket : tickets) {
            if (ticket.getCreatedAt().isBefore(oldestTicket.getCreatedAt())) {
                oldestTicket = ticket;
            }
        }
        return oldestTicket;
    }

    public static void printTriageBoard(List<Ticket> tickets) {
        for (Ticket ticket : tickets) {
            System.out.println(ticket);
            System.out.println("Effort estimate: " + ticket.estimateEffortHours() + " hours");
            if (ticket instanceof Escalatable e) {
                System.out.println("Escalation badge: " + e.escalationBadge());
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Path txtFile = Path.of("tickets.txt");
        Path jsonFile = Path.of("tickets.json");
        Path reportFile = Path.of("daily-report.txt");

        try {
            BugTicket bug = new BugTicket(
                "Crash on checkout",
                "Checkout button throws NullPointerException when cart has discount items and user clicks pay now",
                "Alice",
                Priority.CRITICAL,
                Severity.HIGH,
                "1. Add discount item to cart 2. Click checkout"
            );
            bug.setAssignee("Bob");
            bug.addTag("checkout");
            bug.addTag("backend");
            bug.escalate();

            FeatureRequestTicket feature = new FeatureRequestTicket(
                "Dark mode support",
                "Add dark theme to UI to reduce eye strain during nighttime shifts for customer service agents",
                "Alice",
                Priority.MEDIUM,
                12,
                "Reduces eye strain for night shift"
            );
            feature.setAssignee("Charlie");
            feature.addTag("ui");
            feature.addVote();

            AccessRequestTicket access = new AccessRequestTicket(
                "Database production read access",
                "Read-only credentials for SQL troubleshooting during incident triage in production cluster",
                "Dave",
                Priority.HIGH,
                "Prod-DB",
                "READ"
            );
            access.addTag("security");
            access.escalate();

            Repository<Ticket> repo = new Repository<>();
            repo.add(bug);
            repo.add(feature);
            repo.add(access);

            System.out.println("=== TRIAGE BOARD (SAFE DISPLAY DESCRIPTIONS) ===");
            printTriageBoard(repo.all());

            System.out.println("=== TICKET ANALYTICS ===");
            Map<Priority, Long> priorityCounts = TicketAnalytics.countByPriority(repo.all());
            System.out.println("Count by Priority: " + priorityCounts);

            Map<String, List<Ticket>> assigneeGroups = TicketAnalytics.groupByAssignee(repo.all());
            System.out.println("Grouped by Assignee: " + assigneeGroups.keySet());

            System.out.println("Average Effort: " + TicketAnalytics.averageEffortHours(repo.all()) + "h");
            System.out.println("Total Effort (reduce): " + TicketAnalytics.totalEffortHours(repo.all()) + "h");

            Optional<Ticket> oldestOpen = TicketAnalytics.oldestOpenTicket(repo.all());
            System.out.println("Oldest Open Ticket: " + oldestOpen.map(Ticket::getTitle).orElse("None"));

            String allTags = TicketAnalytics.tagLine(repo.all());
            System.out.println("All Tags: " + allTags);
            System.out.println();

            System.out.println("=== REPOSITORY SAFE LOOKUPS (Optional Conventions) ===");
            String bugTitle = repo.findByIdSafe(bug.getId())
                .map(Ticket::getTitle)
                .orElse("Untitled");
            System.out.println("Mapped title: " + bugTitle);

            repo.findByIdSafe(999).ifPresentOrElse(
                t -> System.out.println("Found: " + t),
                () -> System.out.println("Safe lookup for id 999: Ticket was not found.")
            );

            Ticket directFound = repo.findById(access.getId());
            System.out.println("Direct findById: " + directFound.getTitle());
            System.out.println();

            System.out.println("=== PRIORITY QUEUES ===");
            PriorityQueue<Ticket> naturalQueue = new PriorityQueue<>();
            for (Ticket t : repo.all()) {
                naturalQueue.offer(t);
            }
            System.out.println("Next served (Natural Priority): " + naturalQueue.poll());

            PriorityQueue<Ticket> customQueue = new PriorityQueue<>(TicketComparators.BY_ASSIGNEE_THEN_PRIORITY);
            for (Ticket t : repo.all()) {
                customQueue.offer(t);
            }
            System.out.println("Next served (By Assignee): " + customQueue.poll());
            System.out.println();

            System.out.println("=== LEGACY PIPE-DELIMITED FILE (tickets.txt) ===");
            TicketFileLoader.save(repo.all(), txtFile);
            List<Ticket> migratedTickets = TicketFileLoader.load(txtFile);
            System.out.println("Loaded " + migratedTickets.size() + " tickets from " + txtFile);
            System.out.println();

            System.out.println("=== ONE-TIME MIGRATION TO JSON (tickets.json) ===");
            TicketJsonStore.save(migratedTickets, jsonFile);
            System.out.println("Successfully migrated to " + jsonFile);

            List<Ticket> jsonLoadedTickets = TicketJsonStore.load(jsonFile);
            System.out.println("Loaded " + jsonLoadedTickets.size() + " tickets from " + jsonFile + ":");
            for (Ticket t : jsonLoadedTickets) {
                System.out.println("  " + t);
            }
            System.out.println();

            System.out.println("=== DAILY REPORT GENERATION ===");
            TicketFileLoader.generateReport(jsonLoadedTickets, reportFile);
            System.out.println("Daily report written to " + reportFile);
            System.out.println();

            System.out.println("=== DEMONSTRATING UNCHECKED EXCEPTION HANDLING ===");
            repo.findByIdSafe(1001).orElseThrow(() -> new TicketNotFoundException(1001));

        } catch (TicketNotFoundException e) {
            System.err.println("Lookup Error: " + e.getMessage());
        } catch (InvalidTicketDataException e) {
            System.err.println("Data Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Application Error: " + e.getMessage());
        }
    }
}
