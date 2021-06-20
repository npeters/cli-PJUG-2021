///usr/bin/env jbang "$0" "$@" ; exit $?
//
//DEPS info.picocli:picocli:4.6.1 info.picocli:picocli-codegen:4.6.1
//DEPS org.fusesource.jansi:jansi:2.3.2
//DEPS org.jline:jline:3.17.1
//JAVA 16
//JAVA_OPTIONS --enable-preview
//JAVAC_OPTIONS --enable-preview --release 16

import org.fusesource.jansi.Ansi;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.Display;
import picocli.CommandLine;

import java.io.IOException;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@CommandLine.Command(helpCommand = true)
public class TaskCommand implements Callable<Void> {

    interface Render<S extends Stat> {
        List<String> render(S stat);
    }

    interface Stat {
        boolean completed();
    }

    // ADT:Abstract data type
    record TaskList(String label, List<Task> tasks) implements Stat {

        public TaskList copyTaskList(int index, Task newTask) {
            List<Task> newList = new ArrayList<>(tasks);
            newList.set(index, newTask);
            return new TaskList(this.label, Collections.unmodifiableList(newList));
        }

        @Override
        public boolean completed() {
            return tasks.stream().allMatch(t -> t.status().complete);
        }

    }

    public sealed interface Task<T extends  Task<?>> {
        TaskStatus status();

        String label();

        T withStatus(TaskStatus newStatus);
    }

    record SimpleTask(String label, TaskStatus status) implements Task<SimpleTask> {
        public  SimpleTask withStatus(TaskStatus newStatus) {
            return new SimpleTask(this.label, newStatus);
        }
    }

    record Log(String label, TaskStatus status) {
    }

    record LogsTask(String label, TaskStatus status, List<Log> logs) implements Task<LogsTask> {
        public LogsTask withStatus(TaskStatus newStatus) {
            return new LogsTask(this.label, newStatus, this.logs);
        }
    }

    record ProgressBar(String label, int progress) {
    }

    record ProgressBarsTask(String label, TaskStatus status, List<ProgressBar> progressBars) implements Task<ProgressBarsTask> {

        public ProgressBarsTask withStatus(TaskStatus newStatus) {
            return new ProgressBarsTask(this.label, newStatus, this.progressBars);
        }

        public ProgressBarsTask withProgress(String label, int progress) {
            List<ProgressBar> newList = new ArrayList<>(this.progressBars);
            newList.add(new ProgressBar(label, Math.min(100, progress)));
            return new ProgressBarsTask(this.label, this.status, Collections.unmodifiableList(newList));
        }

        public ProgressBarsTask withProgress(int index, String label, int progress) {
            List<ProgressBar> newList = new ArrayList<>(this.progressBars);
            newList.set(index, new ProgressBar(label, Math.min(100, progress)));
            return new ProgressBarsTask(this.label, this.status, Collections.unmodifiableList(newList));
        }

    }

    enum TaskStatus {
        OPEN(false), PENDING(false), SUCCESS(true), ERROR(true);

        public final boolean complete;

        TaskStatus(boolean complete) {
            this.complete = complete;
        }
    }

    public Void call() throws Exception {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        var processors = new TaskProcessors();

        executor.execute(processors);

        renderLoop(processors.statRef, new TaskListRender());
        executor.shutdown();

        return null;

    }

    private <S extends Stat> void renderLoop(AtomicReference<S> statRef, Render<S> render) throws Exception {
        Display display = buildDisplay();

        S previousStat = statRef.get();
        boolean waitTask = true;
        while (waitTask || !previousStat.equals(statRef.get())) {

            S stat = statRef.get();

            List<String> lines = render.render(stat);

            lines.add(""); // look nicer
            lines.add(""); // look nicer

            waitTask = !stat.completed();

            display.updateAnsi(lines, -1);

            previousStat = stat;
            Thread.sleep(100);

        }
    }

    public static class TaskListRender implements Render<TaskList> {

        @Override
        public List<String> render(TaskList stat) {
            return stat.tasks.stream().flatMap(task -> {
                Stream<String> lines;
                // TODO JEP https://openjdk.java.net/jeps/406 Pattern Matching for switch ...
                if (task instanceof LogsTask logsTask) {
                    lines = renderLogsTask(logsTask);
                } else if (task instanceof SimpleTask simpleTask) {
                    lines = renderSimpleTask(simpleTask);
                } else if (task instanceof ProgressBarsTask progessBarsTask) {
                    lines = renderProgressBarsTask(progessBarsTask);
                } else {
                    throw new IllegalStateException();
                }

                return lines;
            }).collect(Collectors.toList());
        }

        Stream<String> renderSimpleTask(SimpleTask simpleTask) {
            return Stream.of(renderTaskLabel(simpleTask));
        }

        final String TAB = "      ";

        private String renderTaskLabel(Task<?> task) {
            String pattern = switch (task.status()) {
                case OPEN -> ansi().fg(Ansi.Color.DEFAULT).a("  ○ %s \n").reset().toString();
                case PENDING -> ansi().bold().fgBright(Ansi.Color.WHITE).a("  ● %s \n").reset().toString();
                case SUCCESS -> ansi().fgGreen().bold().a("  ✔ %s \n").reset().toString();
                case ERROR -> ansi().fgBrightRed().bold().a("  ✗︎ %s \n").reset().toString();
            };
            return String.format(pattern, task.label());
        }

        Stream<String> renderLogsTask(LogsTask task) {
            return Stream.concat(
                    Stream.of(renderTaskLabel(task)),
                    tail(task.logs.stream(), 3)
                     .map(l -> switch (l.status) {
                          case ERROR -> ansi().fgBrightRed().a(TAB + l.label).reset().toString();
                          default -> ansi().fgRgb(192, 192, 192).a(TAB + l.label).reset().toString();
                     })
		 );
        }

        Stream<String> renderProgressBarsTask(ProgressBarsTask task) {

            if (task.progressBars.isEmpty()) {
                return Stream.of(renderTaskLabel(task));
            } else {
                float ratio = 2f;
                return Stream.concat(
			Stream.of(
		 	  renderTaskLabel(task)), 
			  task.progressBars.stream()
                             .map(pb -> String.format(
                                        TAB + "[%s>%s] %02d%% %s", 
                                        "=".repeat(Math.round(pb.progress / ratio)),
                                        "-".repeat((Math.round(100 / ratio) - Math.round(pb.progress / ratio))),
                                        pb.progress,
                                pb.label)));
            }

        }

    }

    public static class TaskProcessors implements Runnable {
        AtomicReference<TaskList> statRef;

        public TaskProcessors() {
            final TaskList taskList = new TaskList("App",
                    List.of(new LogsTask("Task Install", TaskStatus.OPEN, List.of()),
                            new LogsTask("Task Compile", TaskStatus.OPEN, List.of()),
                            new LogsTask("Task Test", TaskStatus.OPEN, List.of()),
                            new ProgressBarsTask("Task Deploy", TaskStatus.OPEN, List.of())

                    ));

            this.statRef = new AtomicReference<>(taskList);
        }

        public Log log(String l) {
            return new Log(l, TaskStatus.SUCCESS);
        }

        // fake Task Execution
        public void run() {
            {
                int index = 0;
                var stat = statRef.get();
                LogsTask task = (LogsTask) statRef.get().tasks.get(index);

                statRef.set(stat.copyTaskList(index,
                        new LogsTask(task.label(), TaskStatus.PENDING, List.of(log("[10:01] Install Java 16")))));
                pauseShort();
                statRef.set(stat.copyTaskList(index, new LogsTask(task.label(), TaskStatus.PENDING,
                        List.of(log("[10:01] Install Java 16"), log("[10:02] Install Gradle"))

                )));
                pauseShort();
                statRef.set(stat.copyTaskList(index,
                        new LogsTask(task.label, TaskStatus.PENDING, List.of(log("[10:01] Install Java 16"),
                                log("[10:02] Install Gradle"), log("[10:03] Install Helm")))));
                pauseShort();
                statRef.set(stat.copyTaskList(index, new LogsTask(task.label, TaskStatus.SUCCESS, List
                        .of(log("[10:01] Install Java 16"), log("[10:02] Install Gradle"), log("[10:03] Install Helm"))

                )));
                pauseShort();
                statRef.set(stat.copyTaskList(index, new LogsTask(task.label, TaskStatus.SUCCESS, List.of())));
            }
            {
                int index = 1;
                var stat = statRef.get();
                var task = statRef.get().tasks.get(index);
                statRef.set(stat.copyTaskList(index, new LogsTask(task.label(), TaskStatus.PENDING, List.of())));
                pauseShort();
                pauseShort();
                statRef.set(stat.copyTaskList(index, new LogsTask(task.label(), TaskStatus.SUCCESS, List.of())));
            }

            {
                int index = 2;
                var stat = statRef.get();
                var task = statRef.get().tasks.get(index);

                pauseShort();
                List<Log> logs = IntStream.range(1, 20).mapToObj(
                        i -> log(String.format("[10:%02d] test src/test/fr/canal/catalog/Catalog%02dTest.java", i, i)))
                        .collect(Collectors.toList());
                logs.add(new Log("[10:21] test src/test/fr/canal/catalog/Catalog21Test.java", TaskStatus.ERROR));

                statRef.set(stat.copyTaskList(index, new LogsTask(task.label(), TaskStatus.PENDING,
                        Collections.unmodifiableList(logs.subList(0, 3)))));
                pauseShort();

                statRef.set(stat.copyTaskList(index, new LogsTask(task.label(), TaskStatus.PENDING,
                        Collections.unmodifiableList(logs.subList(0, 10)))));
                pauseShort();

                statRef.set(stat.copyTaskList(index, new LogsTask(task.label(), TaskStatus.PENDING,
                        Collections.unmodifiableList(logs.subList(0, 15)))));
                pauseShort();
                statRef.set(stat.copyTaskList(index,
                        new LogsTask(task.label(), TaskStatus.PENDING, Collections.unmodifiableList(logs))));
                pauseShort();
                statRef.set(stat.copyTaskList(index,
                        new LogsTask(task.label(), TaskStatus.SUCCESS, Collections.unmodifiableList(logs))));
            }

            pauseShort();
            statRef.set(statRef.get().copyTaskList(2, statRef.get().tasks.get(2).withStatus(TaskStatus.SUCCESS)));

            {
                int index = 3;
                statRef.set(statRef.get().copyTaskList(index,
                        ((ProgressBarsTask) statRef.get().tasks.get(3)).withProgress("docker push catalog", 2)
                                .withProgress("docker push conso", 5).withProgress("docker push perso", 15)));
                pauseShort();

                for (int i = 0; i < 11; i++) {
                    statRef.set(statRef.get().copyTaskList(index,
                            ((ProgressBarsTask) statRef.get().tasks.get(3))
                                    .withProgress(0, "docker push catalog", 20 + (i * 10))
                                    .withProgress(1, "docker push conso", 5 + (i * 10))
                                    .withProgress(2, "docker push perso", 15 + (i * 10))

                    ));
                    pauseShort();
                }
                statRef.set(statRef.get().copyTaskList(index,
                        ((ProgressBarsTask) statRef.get().tasks.get(3)).withStatus(TaskStatus.SUCCESS)));
                pauseShort();
            }

        }

    }

    protected Display buildDisplay() throws IOException {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();

        Display display = new Display(terminal, false);
        Size size = terminal.getSize();
        display.resize(size.getRows(), size.getColumns());
        return display;
    }

    public static <T> Stream<T> tail(Stream<T> stream, int n) {
        Deque<T> result = new ArrayDeque<>(n);
        stream.forEachOrdered(x -> {
            if (result.size() == n) {
                result.pop();
            }
            result.add(x);
        });
        return result.stream();
    }


    public static void pauseShort() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TaskCommand()).execute(args);
        System.exit(exitCode);
    }

}
