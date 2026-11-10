import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum AccessLevel {
    READ,
    WRITE,
    ADMIN
}

interface Escalatable {
    void escalate();
    boolean isEscalated();
    int escalationLevel();

    default String escalationBadge() {
        return isEscalated() ? "ESCALATED (Level " + escalationLevel() + ")" : "NORMAL";
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BugTicket.class, name = "bug"),
    @JsonSubTypes.Type(value = FeatureRequestTicket.class, name = "feature"),
    @JsonSubTypes.Type(value = AccessRequestTicket.class, name = "access")
})
public abstract class Ticket implements Comparable<Ticket> {
    private static int nextId = 0;

    private final int id;
    private final String title;
    private String description;
    private final String requester;
    private Priority priority;
    private Status status;
    private final LocalDateTime createdAt;
    private String assignee;
    private final Set<String> tags = new HashSet<>();

    public Ticket(String title, String description, String requester, Priority priority) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        if (requester == null || requester.isBlank()) {
            throw new IllegalArgumentException("Requester cannot be null or blank");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        this.id = ++nextId;
        this.title = title.trim();
        this.description = description.trim();
        this.requester = requester.trim();
        this.priority = priority;
        this.status = Status.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Ticket(String title, String requester) {
        this(title, "General request", requester, Priority.LOW);
    }

    @JsonCreator
    protected Ticket(
        @JsonProperty("id") int id,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("requester") String requester,
        @JsonProperty("priority") Priority priority,
        @JsonProperty("status") Status status,
        @JsonProperty("createdAt") LocalDateTime createdAt
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid ticket id");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        if (requester == null || requester.isBlank()) {
            throw new IllegalArgumentException("Requester cannot be null or blank");
        }
        if (priority == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        this.id = id;
        this.title = title.trim();
        this.description = description.trim();
        this.requester = requester.trim();
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        nextId = Math.max(nextId, id);
    }

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonIgnore
    public String getDisplayDescription() {
        if (description == null) {
            return "";
        }
        return description.length() <= 80 ? description : description.substring(0, 80) + "...";
    }

    @JsonProperty("requester")
    public String getRequester() {
        return requester;
    }

    @JsonProperty("priority")
    public Priority getPriority() {
        return priority;
    }

    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("assignee")
    public String getAssignee() {
        return assignee;
    }

    @JsonProperty("tags")
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        this.description = description.trim();
    }

    @JsonProperty("priority")
    public void setPriority(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        this.priority = priority;
    }

    @JsonProperty("status")
    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
    }

    @JsonProperty("assignee")
    public void setAssignee(String assignee) {
        this.assignee = (assignee == null || assignee.isBlank()) ? null : assignee.trim();
    }

    @JsonProperty("tags")
    public void setTags(Set<String> tags) {
        this.tags.clear();
        if (tags != null) {
            for (String tag : tags) {
                addTag(tag);
            }
        }
    }

    public void addTag(String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag.trim().toLowerCase());
        }
    }

    @JsonIgnore
    public long ageInHours() {
        return Duration.between(createdAt, LocalDateTime.now()).toHours();
    }

    public static int totalCreated() {
        return nextId;
    }

    @JsonProperty("type")
    public abstract String getType();

    public abstract double estimateEffortHours();

    @Override
    public int compareTo(Ticket other) {
        int priorityComparison = other.priority.compareTo(this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        return this.createdAt.compareTo(other.createdAt);
    }

    @Override
    public String toString() {
        return "#" + id + " [" + priority + "] " + title + " (" + status + ") - " + getDisplayDescription() + " (Requester: " + requester + ")";
    }
}

class BugTicket extends Ticket implements Escalatable {
    private final Severity severity;
    private final String stepsToReproduce;
    private int escalationLevel = 0;

    public BugTicket(String title, String description, String requester, Priority priority, Severity severity, String stepsToReproduce) {
        super(title, description, requester, priority);
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (stepsToReproduce == null || stepsToReproduce.isBlank()) {
            throw new IllegalArgumentException("Steps to reproduce cannot be blank");
        }
        this.severity = severity;
        this.stepsToReproduce = stepsToReproduce.trim();
    }

    public BugTicket(String title, String requester, Severity severity, String stepsToReproduce) {
        super(title, requester);
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (stepsToReproduce == null || stepsToReproduce.isBlank()) {
            throw new IllegalArgumentException("Steps to reproduce cannot be blank");
        }
        this.severity = severity;
        this.stepsToReproduce = stepsToReproduce.trim();
    }

    public BugTicket(int id, String title, String description, String requester, Priority priority, Status status, LocalDateTime createdAt, Severity severity, String stepsToReproduce) {
        this(id, title, description, requester, priority, status, createdAt, severity, stepsToReproduce, 0);
    }

    @JsonCreator
    public BugTicket(
        @JsonProperty("id") int id,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("requester") String requester,
        @JsonProperty("priority") Priority priority,
        @JsonProperty("status") Status status,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("severity") Severity severity,
        @JsonProperty("stepsToReproduce") String stepsToReproduce,
        @JsonProperty("escalationLevel") int escalationLevel
    ) {
        super(id, title, description, requester, priority, status, createdAt);
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (stepsToReproduce == null || stepsToReproduce.isBlank()) {
            throw new IllegalArgumentException("Steps to reproduce cannot be blank");
        }
        this.severity = severity;
        this.stepsToReproduce = stepsToReproduce.trim();
        this.escalationLevel = Math.max(0, escalationLevel);
    }

    @Override
    public String getType() {
        return "bug";
    }

    @JsonProperty("severity")
    public Severity getSeverity() {
        return severity;
    }

    @JsonProperty("stepsToReproduce")
    public String getStepsToReproduce() {
        return stepsToReproduce;
    }

    @Override
    public void escalate() {
        escalationLevel++;
    }

    @Override
    @JsonIgnore
    public boolean isEscalated() {
        return escalationLevel > 0;
    }

    @Override
    @JsonProperty("escalationLevel")
    public int escalationLevel() {
        return escalationLevel;
    }

    @JsonProperty("escalationLevel")
    public void restoreEscalation(int level) {
        this.escalationLevel = level;
    }

    @Override
    public double estimateEffortHours() {
        return switch (severity) {
            case LOW -> 1.0;
            case MEDIUM -> 3.0;
            case HIGH -> 5.0;
            case CRITICAL -> 8.0;
        };
    }

    @Override
    public String toString() {
        return super.toString() + " [Bug | Severity: " + severity + "]";
    }
}

class FeatureRequestTicket extends Ticket {
    private int votes;
    private final String businessJustification;

    public FeatureRequestTicket(String title, String description, String requester, Priority priority, int votes, String businessJustification) {
        super(title, description, requester, priority);
        if (votes < 0) {
            throw new IllegalArgumentException("Votes cannot be negative");
        }
        if (businessJustification == null || businessJustification.isBlank()) {
            throw new IllegalArgumentException("Business justification cannot be blank");
        }
        this.votes = votes;
        this.businessJustification = businessJustification.trim();
    }

    public FeatureRequestTicket(String title, String requester, int votes, String businessJustification) {
        super(title, requester);
        if (votes < 0) {
            throw new IllegalArgumentException("Votes cannot be negative");
        }
        if (businessJustification == null || businessJustification.isBlank()) {
            throw new IllegalArgumentException("Business justification cannot be blank");
        }
        this.votes = votes;
        this.businessJustification = businessJustification.trim();
    }

    @JsonCreator
    public FeatureRequestTicket(
        @JsonProperty("id") int id,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("requester") String requester,
        @JsonProperty("priority") Priority priority,
        @JsonProperty("status") Status status,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("votes") int votes,
        @JsonProperty("businessJustification") String businessJustification
    ) {
        super(id, title, description, requester, priority, status, createdAt);
        if (votes < 0) {
            throw new IllegalArgumentException("Votes cannot be negative");
        }
        if (businessJustification == null || businessJustification.isBlank()) {
            throw new IllegalArgumentException("Business justification cannot be blank");
        }
        this.votes = votes;
        this.businessJustification = businessJustification.trim();
    }

    @Override
    public String getType() {
        return "feature";
    }

    @JsonProperty("votes")
    public int getVotes() {
        return votes;
    }

    @JsonProperty("businessJustification")
    public String getBusinessJustification() {
        return businessJustification;
    }

    public void addVote() {
        votes++;
    }

    @JsonProperty("votes")
    public void setVotes(int votes) {
        if (votes < 0) {
            throw new IllegalArgumentException("Votes cannot be negative");
        }
        this.votes = votes;
    }

    @Override
    public double estimateEffortHours() {
        return 4.0 + (votes * 0.2);
    }

    @Override
    public String toString() {
        return super.toString() + " [Feature | Votes: " + votes + "]";
    }
}

class AccessRequestTicket extends Ticket implements Escalatable {
    private final String systemName;
    private final String accessLevel;
    private int escalationLevel = 0;

    public AccessRequestTicket(String title, String description, String requester, Priority priority, String systemName, String accessLevel) {
        super(title, description, requester, priority);
        if (systemName == null || systemName.isBlank()) {
            throw new IllegalArgumentException("System name cannot be blank");
        }
        if (accessLevel == null || accessLevel.isBlank()) {
            throw new IllegalArgumentException("Access level cannot be blank");
        }
        this.systemName = systemName.trim();
        this.accessLevel = accessLevel.trim();
    }

    public AccessRequestTicket(String title, String requester, String systemName, String accessLevel) {
        super(title, requester);
        if (systemName == null || systemName.isBlank()) {
            throw new IllegalArgumentException("System name cannot be blank");
        }
        if (accessLevel == null || accessLevel.isBlank()) {
            throw new IllegalArgumentException("Access level cannot be blank");
        }
        this.systemName = systemName.trim();
        this.accessLevel = accessLevel.trim();
    }

    public AccessRequestTicket(int id, String title, String description, String requester, Priority priority, Status status, LocalDateTime createdAt, String systemName, String accessLevel) {
        this(id, title, description, requester, priority, status, createdAt, systemName, accessLevel, 0);
    }

    @JsonCreator
    public AccessRequestTicket(
        @JsonProperty("id") int id,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("requester") String requester,
        @JsonProperty("priority") Priority priority,
        @JsonProperty("status") Status status,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("systemName") String systemName,
        @JsonProperty("accessLevel") String accessLevel,
        @JsonProperty("escalationLevel") int escalationLevel
    ) {
        super(id, title, description, requester, priority, status, createdAt);
        if (systemName == null || systemName.isBlank()) {
            throw new IllegalArgumentException("System name cannot be blank");
        }
        if (accessLevel == null || accessLevel.isBlank()) {
            throw new IllegalArgumentException("Access level cannot be blank");
        }
        this.systemName = systemName.trim();
        this.accessLevel = accessLevel.trim();
        this.escalationLevel = Math.max(0, escalationLevel);
    }

    public AccessRequestTicket(String title, String description, String requester, Priority priority, String systemName, AccessLevel accessLevel) {
        this(title, description, requester, priority, systemName, accessLevel != null ? accessLevel.name() : "READ");
    }

    public AccessRequestTicket(String title, String requester, String systemName, AccessLevel accessLevel) {
        this(title, requester, systemName, accessLevel != null ? accessLevel.name() : "READ");
    }

    public AccessRequestTicket(int id, String title, String description, String requester, Priority priority, Status status, LocalDateTime createdAt, String systemName, AccessLevel accessLevel) {
        this(id, title, description, requester, priority, status, createdAt, systemName, accessLevel != null ? accessLevel.name() : "READ");
    }

    @Override
    public String getType() {
        return "access";
    }

    @JsonProperty("systemName")
    public String getSystemName() {
        return systemName;
    }

    @JsonProperty("accessLevel")
    public String getAccessLevel() {
        return accessLevel;
    }

    @Override
    public void escalate() {
        escalationLevel++;
    }

    @Override
    @JsonIgnore
    public boolean isEscalated() {
        return escalationLevel > 0;
    }

    @Override
    @JsonProperty("escalationLevel")
    public int escalationLevel() {
        return escalationLevel;
    }

    @JsonProperty("escalationLevel")
    public void restoreEscalation(int level) {
        this.escalationLevel = level;
    }

    @Override
    public double estimateEffortHours() {
        return switch (accessLevel.toUpperCase()) {
            case "READ" -> 1.0;
            case "WRITE" -> 2.0;
            case "ADMIN" -> 4.0;
            default -> 1.0;
        };
    }

    @Override
    public String toString() {
        return super.toString() + " [Access | System: " + systemName + " | Level: " + accessLevel + "]";
    }
}
