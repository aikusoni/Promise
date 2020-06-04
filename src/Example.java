public class Example {
    public static void main(String[] args) {
        testUsage();
        test1();
        test2();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {

        }
    }

    private static void testUsage() {
        Promise.begin("Hello")
            .onThen((p, i) -> {
                p.resolve((String)i + ", ");
            })
            .onThen((p, i) -> {
                p.resolve((String)i + "World");
            })
            .onThen((p, i) -> {
                p.resolve((String)i + "!");
            })
            .onThen((p, i) -> {
                p.reject(new Exception((String)i));
            })
            .onCatch((error) -> {
                System.out.println("" + error.toString());
            });
        
        ///// output
        // java.lang.Exception: Hello, World!
    }

    private static void test1() {
        System.out.println("-test1-");
        Promise
            .begin(1)
            .onThen((p, i) -> {
                System.out.println((int)i);
                p.resolve((int)i + 1);
            })
            .onThen((p, i) -> {
                System.out.println((int)i);
                p.resolve((int)i + 1);
            })
            .onCatch((error) -> {
                System.out.println(error.toString());
            });
        
        ///// output
        // 1
        // 2
    }

    private static void test2() {
        System.out.println("-test2-");
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

        ///// output
        // task#1 : 1
        // task#2 : 2
        // task#2-1 : 3
        // task#2-2 : 3
        // error : java.lang.Exception: task#2-2 error!
        // task#2-1-1 : 4
    }
}
