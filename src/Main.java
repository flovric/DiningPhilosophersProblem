import java.util.Random;
import mpi.MPI;
import mpi.Status;


/**
 * Created by Filip on 8.4.2018..
 */
public class Main {

    private static final int MAX_THINKING_TIME = 20;     // in seconds
    private static final int MAX_EATING_TIME = 10;       // in seconds
    private static final int FORK_REQUEST = 8;
    private static final int FORK_SENT = 9;


    private static int rank;
    private static int size;
    private static int leftNeighbor;
    private static int rightNeighbor;
    private static Fork leftFork;
    private static Fork rightFork;

    private static boolean needToSendLeftFork = false;
    private static boolean needToSendRightFork = false;
    private static boolean wasLeftFirst = true;

    private static int[] buffer = new int[]{0};
    private static Status status;


    public static void main(String[] args) throws InterruptedException {
        // beginning of MPI initialization
        MPI.Init(args);
        rank = MPI.COMM_WORLD.Rank();
        size = MPI.COMM_WORLD.Size();

        int numberOfPhilosophers = size;

        // Initialize state of philosophers and forks


        if (rank == 0) {    // first philosopher has both forks
            leftNeighbor = numberOfPhilosophers - 1;
            rightNeighbor = 1;

            leftFork = new Fork();
            rightFork = new Fork();
        }
        else if (rank == numberOfPhilosophers - 1) {  // last one doesn't have fork in the beginning
            leftNeighbor = rank - 1;
            rightNeighbor = 0;

            leftFork = null;
            rightFork = null;
        }
        else {
            leftNeighbor = rank - 1;
            rightNeighbor = rank + 1;

            leftFork = null;
            rightFork = new Fork();
        }
        // end of initialization

        Random r = new Random();
        int thinkingTime;
        int eatingTime;

        while (true) {
            // Philosopher is thinking
            thinkingTime = r.nextInt(MAX_THINKING_TIME);
            printMessage("Mislim", rank);

            for(int i = 0; i < thinkingTime; i++) {
                checkRequests();
                Thread.sleep(1000);
            }

            if (leftFork != null && rightFork != null)
                printMessage("Imam vec obe", rank);


            // Philosopher is hungry
            while (leftFork == null || rightFork == null) {
                if (leftFork == null) {
                    printMessage("Trazim lijevu", rank);
                    MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.INT, leftNeighbor, FORK_REQUEST);
                }

                // wait for left fork
                do {
                    // check first if anybody needs forks
                    checkRequests();

                    // check your left fork
                    status = MPI.COMM_WORLD.Iprobe(leftNeighbor, FORK_SENT);
                    if (status != null && status.tag == FORK_SENT && status.source == leftNeighbor) {
                        MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, leftNeighbor, FORK_SENT);
                        leftFork = new Fork(true);
                        printMessage("Dobio lijevu", rank);
                    }
                } while(leftFork == null);

                if (rightFork == null) {
                    printMessage("Trazim desnu", rank);
                    MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.INT, rightNeighbor, FORK_REQUEST);
                }
                // wait for right fork
                do {
                    // check first if anybody needs forks
                    checkRequests();

                    // check your right fork
                    status = MPI.COMM_WORLD.Iprobe(rightNeighbor, FORK_SENT);
                    if (status != null && status.tag == FORK_SENT && status.source == rightNeighbor) {
                        MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, rightNeighbor, FORK_SENT);
                        rightFork = new Fork(true);
                        printMessage("Dobio desnu", rank);
                    }
                } while(rightFork == null);
            }

            // Philosopher is eating
            printMessage("Jedem", rank);
            eatingTime = r.nextInt(MAX_EATING_TIME);

            // soil the forks
            leftFork.soilTheFork();
            rightFork.soilTheFork();

            // need to check if there were requests for forks
            if(wasLeftFirst) {
                if (needToSendLeftFork)
                    sendLeftFork();
                if (needToSendRightFork)
                    sendRightFork();
            }
            else  {
                if (needToSendRightFork)
                    sendRightFork();
                if (needToSendLeftFork)
                    sendLeftFork();
            }
            needToSendLeftFork = false;
            needToSendRightFork = false;
            wasLeftFirst = true;

            for(int i = 0; i < eatingTime; i++){
                Thread.sleep(1000);
            }
        }

        //MPI.Finalize();
    }

    private static void checkRequests() {
        status = MPI.COMM_WORLD.Iprobe(leftNeighbor, FORK_REQUEST);
        if (status != null && status.tag == FORK_REQUEST && status.source == leftNeighbor) {
            if (leftFork != null) { // need to send my left fork
                if(!leftFork.isClean()) {
                    MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, leftNeighbor, FORK_REQUEST);
                    leftFork.cleanTheFork();
                    MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.INT, leftNeighbor, FORK_SENT);
                    leftFork = null;
                }
                else {
                    needToSendLeftFork = true;
                    wasLeftFirst = needToSendRightFork  ? false : true;

                }
            }
        }

        status = MPI.COMM_WORLD.Iprobe(rightNeighbor, FORK_REQUEST);
        if (status != null && status.tag == FORK_REQUEST && status.source == rightNeighbor) {
            if (rightFork != null) {
                if(!rightFork.isClean()) {
                    MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, rightNeighbor, FORK_REQUEST);
                    rightFork.cleanTheFork();
                    MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.INT, rightNeighbor, FORK_SENT);
                    rightFork = null;
                }
                else {
                    needToSendRightFork = true;
                    wasLeftFirst = needToSendLeftFork  ? true : false;
                }
            }
        }
    }

    private static void printMessage(String message, int rank){
        printSpaces(rank);
        System.out.println((rank+1) + ":" + message);
    }

    private static void printSpaces(int rank) {
        int i;
        for(i=0; i<=rank; i++) {
            if(i != 0)
                System.out.print("\t\t\t\t\t");
        }
    }

    private static void sendLeftFork() {
        MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, leftNeighbor, FORK_REQUEST);
        leftFork.cleanTheFork();
        MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.INT, leftNeighbor, FORK_SENT);
        leftFork = null;
    }
    private static void sendRightFork() {
        MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, rightNeighbor, FORK_REQUEST);
        rightFork.cleanTheFork();
        MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.INT, rightNeighbor, FORK_SENT);
        rightFork = null;
    }
}