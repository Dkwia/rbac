package rbac;

import java.util.ArrayList;
import java.util.List;

public class MultiThreadSimulation {

    private static final int THREAD_COUNT = 5;
    private static final int TASK_LENGTH = 30;
    private static final int DELAY = 100;

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadNumber = i + 1;
            Thread t = new Thread(new Worker(threadNumber));
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\nAll threads successfully finish their jobs.");
    }

    static class Worker implements Runnable {
        private final int number;

        public Worker(int number) {
            this.number = number;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            long threadId = Thread.currentThread().getId();

            StringBuilder progress = new StringBuilder();

            for (int i = 0; i < TASK_LENGTH; i++) {
                progress.append("#");

                synchronized (System.out) {
                    System.out.printf(
                            "Thread %d (ID=%d): [%-30s]\r\n",
                            number,
                            threadId,
                            progress.toString()
                    );
                }

                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            synchronized (System.out) {
                System.out.printf(
                        "Thread %d (ID=%d): [%-30s] Time: %d ms\n",
                        number,
                        threadId,
                        progress.toString(),
                        totalTime
                );
            }
        }
    }
}
