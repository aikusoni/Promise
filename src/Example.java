public class Example {
    public static void main(String[] args) {
        test();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {

        }
    }

    private static void test() {
        System.out.println("-begin-");
        Promise p2 = Promise.onBegin(1, (p, i) -> {
            // run
            System.out.println("task#1 : " + (int)i);
            p.resolve((int)i + 1);
        })
        .onThen((p, i) -> {
            // run
            System.out.println("task#2 : " + (int)i);
            new Thread(() -> {
                sleep(300);
                p.resolve((int)i + 1);
            }).start();
        })
        .onCatch((e) -> {
            // not run
            System.out.println("error : " + e.toString());
        });

        p2.onThen((p, i) -> {
            // run
            System.out.println("task#2-1 : " + (int)i);            
            new Thread(() -> {
                sleep(700);
                p.resolve((int)i + 1);  
            }).start();
        })
        .onThen((p, i) -> {
            // run
            System.out.println("task#2-1-1 : " + (int)i);
            p.resolve((int)i + 1);  
        })
        .onCatch((e) -> {
            // not run
            System.out.println("error : " + e.toString());
        });

        p2.onThen((p, i) -> {
            // run
            System.out.println("task#2-2 : " + (int)i);
            p.reject(new Exception("task#2-2 error!"));  
        })
        .onThen((p, i) -> {
            // not run cause task#2-2 error
            System.out.println("task#2-2-1 : " + (int)i);
            p.reject(new Exception("task#2-2-1 error!"));  
        })
        .onCatch((e) -> {
            // run
            System.out.println("error : " + e.toString());
        });

        sleep(5000);
        System.out.println("-end-");

        // output
        // -begin-
        // task#1 : 1
        // task#2 : 2
        // task#2-1 : 3
        // task#2-2 : 3
        // error : java.lang.Exception: task#2-2 error!
        // task#2-1-1 : 4
        // -end-
    }
}
