import java.util.Scanner;

public class QueueSimulator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter starting ticket number: ");
        int startTicket = scanner.nextInt();

        System.out.print("Enter customer count to simulate: ");
        int count = scanner.nextInt();

        int served = 0;
        int vip = 0;
        int priorityRecheck = 0;

        for (int i = 1; i <= count; i++) {
            int ticketNumber = startTicket + i - 1;
            System.out.println("Now serving ticket #" + ticketNumber);
            served++;

            if (i % 10 == 0) {
                System.out.println("  -> Flag: Priority recheck");
                priorityRecheck++;
            } else if (i % 5 == 0) {
                System.out.println("  -> Flag: VIP lane");
                vip++;
            }
        }

        System.out.println("--- Simulation Summary ---");
        System.out.println("Total served: " + served);
        System.out.println("Total VIP tickets: " + vip);
        System.out.println("Total priority rechecks: " + priorityRecheck);

        scanner.close();
    }
}
