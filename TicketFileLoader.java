import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TicketFileLoader {
    public static void save(List<Ticket> tickets, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Ticket t : tickets) {
                String detail1 = "";
                String detail2 = "";

                if (t instanceof BugTicket bug) {
                    detail1 = bug.getSeverity().name();
                    detail2 = bug.getStepsToReproduce();
                } else if (t instanceof FeatureRequestTicket feature) {
                    detail1 = String.valueOf(feature.getVotes());
                    detail2 = feature.getBusinessJustification();
                } else if (t instanceof AccessRequestTicket access) {
                    detail1 = access.getSystemName();
                    detail2 = access.getAccessLevel();
                }

                Set<String> sortedTags = new TreeSet<String>();
                if (t.getTags() != null) {
                    sortedTags.addAll(t.getTags());
                }
                String tags = String.join(",", sortedTags);

                int level = 0;
                if (t instanceof Escalatable e) {
                    level = e.escalationLevel();
                }

                String line = String.join("|",
                    t.getType(),
                    String.valueOf(t.getId()),
                    t.getTitle(),
                    t.getDescription(),
                    t.getRequester(),
                    t.getPriority().name(),
                    t.getStatus().name(),
                    t.getCreatedAt().toString(),
                    t.getAssignee() == null ? "" : t.getAssignee(),
                    tags,
                    detail1,
                    detail2,
                    String.valueOf(level)
                );

                writer.write(line);
                writer.newLine();
            }
        }
    }

    public static List<Ticket> load(Path file) throws IOException, InvalidTicketDataException {
        List<Ticket> tickets = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                try {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length != 13) {
                        throw new IllegalArgumentException("Expected 13 fields, got " + parts.length);
                    }

                    for (int i = 0; i < parts.length; i++) {
                        parts[i] = parts[i].trim();
                    }

                    int id = Integer.parseInt(parts[1]);
                    String title = parts[2];
                    String description = parts[3];
                    String requester = parts[4];
                    Priority priority = Priority.valueOf(parts[5].toUpperCase());
                    Status status = Status.valueOf(parts[6].toUpperCase());
                    LocalDateTime createdAt = LocalDateTime.parse(parts[7]);

                    Ticket ticket;
                    switch (parts[0].toLowerCase()) {
                        case "bug" -> {
                            Severity severity = Severity.valueOf(parts[10].toUpperCase());
                            String steps = parts[11];
                            ticket = new BugTicket(id, title, description, requester, priority, status, createdAt, severity, steps);
                        }
                        case "feature" -> {
                            int votes = Integer.parseInt(parts[10]);
                            String justification = parts[11];
                            ticket = new FeatureRequestTicket(id, title, description, requester, priority, status, createdAt, votes, justification);
                        }
                        case "access" -> {
                            String systemName = parts[10];
                            String level = parts[11];
                            ticket = new AccessRequestTicket(id, title, description, requester, priority, status, createdAt, systemName, level);
                        }
                        default -> throw new IllegalArgumentException("Unknown ticket type: " + parts[0]);
                    }

                    if (!parts[8].isBlank()) {
                        ticket.setAssignee(parts[8]);
                    }

                    if (!parts[9].isBlank()) {
                        for (String tag : parts[9].split(",")) {
                            ticket.addTag(tag);
                        }
                    }

                    int escalation = Integer.parseInt(parts[12]);
                    if (ticket instanceof BugTicket bug) {
                        bug.restoreEscalation(escalation);
                    } else if (ticket instanceof AccessRequestTicket access) {
                        access.restoreEscalation(escalation);
                    }

                    tickets.add(ticket);
                } catch (RuntimeException e) {
                    throw new InvalidTicketDataException(lineNumber, e.getMessage(), e);
                }
            }
        }

        return tickets;
    }

    public static void generateReport(List<Ticket> tickets, Path output) throws IOException {
        StringBuilder report = new StringBuilder();

        Map<Status, Long> statusCounts = new EnumMap<>(Status.class);
        Map<Priority, Long> priorityCounts = new EnumMap<>(Priority.class);

        for (Ticket t : tickets) {
            statusCounts.merge(t.getStatus(), 1L, Long::sum);
            priorityCounts.merge(t.getPriority(), 1L, Long::sum);
        }

        report.append("===== END OF DAY REPORT =====\n\n");

        report.append("BY STATUS:\n");
        for (Status s : Status.values()) {
            report.append("  ").append(s).append(": ").append(statusCounts.getOrDefault(s, 0L)).append("\n");
        }

        report.append("\nBY PRIORITY:\n");
        for (Priority p : Priority.values()) {
            report.append("  ").append(p).append(": ").append(priorityCounts.getOrDefault(p, 0L)).append("\n");
        }

        Ticket oldestOpen = tickets.stream()
            .filter(t -> t.getStatus() == Status.OPEN)
            .min(Comparator.comparing(Ticket::getCreatedAt))
            .orElse(null);

        report.append("\nOLDEST OPEN TICKET:\n");
        report.append("  ").append(oldestOpen == null ? "None" : oldestOpen.toString()).append("\n");

        report.append("\nOVERDUE CRITICAL TICKETS (>24 HOURS):\n");
        boolean hasOverdue = false;
        for (Ticket t : tickets) {
            if (t.getPriority() == Priority.CRITICAL && t.getStatus() != Status.RESOLVED && t.getStatus() != Status.CLOSED && t.ageInHours() > 24) {
                report.append("  ").append(t).append(" | Age: ").append(t.ageInHours()).append(" hours\n");
                hasOverdue = true;
            }
        }
        if (!hasOverdue) {
            report.append("  None\n");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write(report.toString());
        }
    }
}

