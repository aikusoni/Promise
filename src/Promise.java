import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashSet;

/**
 * JAVA Promise
 * 
 * Example :
 * 
 * Promise.begin("Hello")
 *  .onThen((p, i) -> {
 *      p.resolve((String)i + ", "); // "Hello, "
 *  })
 *  .onThen((p, i) -> {
 *      p.resolve((String)i + "World"); // "Hello, World"
 *  })
 *  .onThen((p, i) -> {
 *      p.resolve((String)i + "!"); // "Hello, World!"
 *  })
 *  .onThen((p, i) -> {
 *      p.reject(new Exception((String)i));
 *  })
 *  .onCatch((error) -> {
 *      System.out.println("" + error.toString());
 *  });
 * 
 * The above JAVA code is similar to the below JavaScript Code.
 * 
 * new Promise((resolve, reject) => {
 *      resolve('Hello');   // 'Hello'
 * })
 * .then((i) => {
 *      return i + ', ';    // 'Hello, '
 * })
 * .then((i) => {
 *      return i + 'World'; // 'Hello, World'
 * })
 * .then((i) => {
 *      return i + '!';     // 'Hello, World!'
 * })
 * .then((i) => {
 *      throw i;
 * })
 * .catch((err) => {
 *      console.log('' + i);
 * });
 */
public class Promise {
    private ObservableTaskResult prevTaskResult;
    private ConcurrentLinkedQueue<TaskDelegator> taskDelegators = new ConcurrentLinkedQueue<TaskDelegator>();
    private ConcurrentLinkedQueue<ErrorTask> errorTasks = new ConcurrentLinkedQueue<ErrorTask>();

    public interface Task {
        void run(Publisher publisher, Object input);
    }

    public interface ErrorTask {
        void run(Throwable errror);
    }

    public static Promise begin(Object input) {
        TaskDelegator taskDelegator = new TaskDelegator(new Task() {
            @Override
            public void run(Publisher publisher, Object input) {
                publisher.resolve(input);
            }
        });
        Promise promise = new Promise(taskDelegator.getTaskResult());
        taskDelegator.run(input);
        return promise;
    }

    public static Promise onBegin(Object input, Task task) {
        TaskDelegator taskDelegator = new TaskDelegator(task);
        Promise promise = new Promise(taskDelegator.getTaskResult());
        taskDelegator.run(input);
        return promise;
    }

    public Promise onThen(Task task) {
        TaskDelegator taskDelegator = new TaskDelegator(task);
        Promise promise = new Promise(taskDelegator.getTaskResult());
        promise(taskDelegator);
        return promise;
    }

    public Promise onCatch(ErrorTask errorTask) {
        promise(errorTask);
        return this;
    }
    
    private Promise(ObservableTaskResult prevTaskResult) {
        this.prevTaskResult = prevTaskResult;
        this.prevTaskResult.attach(new Observer() {
            @Override
            public void onChanged() {
                checkAndRunPromised();
            }
        });
    }

    private void promise(TaskDelegator taskDelegator) {
        taskDelegators.offer(taskDelegator);
        checkAndRunPromised();
    }

    private void promise(ErrorTask errorTask) {
        errorTasks.offer(errorTask);
        checkAndRunPromised();
    }

    private void checkAndRunPromised() {
        if (this.prevTaskResult.getTaskState() == TaskState.COMPLETE) {
            while (this.taskDelegators.peek() != null) {
                TaskDelegator taskDelegator = this.taskDelegators.poll();
                taskDelegator.run(this.prevTaskResult.getNextInput());
            }
        } else if (this.prevTaskResult.getTaskState() == TaskState.ERROR) {
            while (this.errorTasks.peek() != null) {
                ErrorTask errorTask = this.errorTasks.poll();
                errorTask.run(this.prevTaskResult.getError());
            }
            while (this.taskDelegators.peek() != null) {
                TaskDelegator taskDelegator = taskDelegators.poll();
                taskDelegator.error(this.prevTaskResult.getError());
            }
        }
    }

    public static class Publisher {
        private ObservableTaskResult taskResult;
        Publisher(ObservableTaskResult taskResult) {
            this.taskResult = taskResult;
        }

        void resolve(Object nextInput) {
            taskResult.setCompleted(nextInput);
        }

        void reject(Throwable error) {
            taskResult.setError(error);
        }
    }

    private enum TaskState {
        AWAIT,
        COMPLETE,
        ERROR
    }

    private static abstract class Observable {
        private HashSet<Observer> observers = new HashSet<Observer>();
        
        protected void notifyChanged() {
            for (Observer observer : observers) {
                observer.onChanged();
            }
        }

        void attach(Observer observer) {
            observers.add(observer);
        }

        void detach(Observer observer) {
            if (observers.contains(observer)) {
                observers.remove(observer);
            }
        }
    }

    private interface Observer {
        public void onChanged();
    }

    private static class ObservableTaskResult extends Observable {
        private TaskState taskState = TaskState.AWAIT;
        private Object nextInput = null;
        private Throwable error = null;

        synchronized void setCompleted(Object nextInput) {
            if (taskState == TaskState.AWAIT) {
                taskState = TaskState.COMPLETE;
                this.nextInput = nextInput;
                notifyChanged();
            }
        }

        synchronized void setError(Throwable error) {
            if (taskState == TaskState.AWAIT) {
                taskState = TaskState.ERROR;
                this.error = error;
                notifyChanged();
            }
        }

        synchronized TaskState getTaskState() {
            return taskState;
        }

        synchronized Object getNextInput() {
            return nextInput;
        }

        synchronized Throwable getError() {
            return error;
        }
    } 

    private static class TaskDelegator {
        private Task task; 
        private ObservableTaskResult taskResult;

        TaskDelegator(Task task) {
            this.task = task;
            this.taskResult = new ObservableTaskResult();
        }

        void run(Object input) {
            try {
                this.task.run(new Publisher(taskResult), input);
            } catch (Throwable err) {
                error(err);
            }
        }

        void error(Throwable error) {
            this.taskResult.setError(error);
        }
        
        ObservableTaskResult getTaskResult() {
            return this.taskResult;
        }
    }
}