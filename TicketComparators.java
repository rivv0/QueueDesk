import java.util.Comparator;

public final class TicketComparators {
    private TicketComparators() {}

    public static final Comparator<Ticket> BY_ASSIGNEE_THEN_PRIORITY =
        Comparator.comparing(
            Ticket::getAssignee,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ).thenComparing(
            Ticket::getPriority,
            Comparator.reverseOrder()
        );

    public static final Comparator<Ticket> BY_STATUS_THEN_AGE =
        Comparator.comparing(Ticket::getStatus)
                  .thenComparing(Ticket::getCreatedAt);
}

