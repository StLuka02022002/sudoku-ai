package luka.teum.image_service.algorithm;

import lombok.Getter;
import lombok.Setter;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Algorithms {

    public interface Solver {
        Point[] solve(Mat data);

        default String getName() {
            return this.getClass().getSimpleName();
        }
    }

    private final List<AlgorithmConfig> algorithmConfigs;
    private final ExecutorService executorService;

    private ImageAlgorithm.PrepareProcess prepareProcess;
    private final boolean parallelExecution;

    public Algorithms(List<AlgorithmConfig> algorithmConfigs) {
        this(algorithmConfigs, true, Runtime.getRuntime().availableProcessors());
    }

    public Algorithms(List<AlgorithmConfig> algorithmConfigs, boolean parallelExecution, int threadPoolSize) {
        this.algorithmConfigs = new ArrayList<>(Objects.requireNonNull(algorithmConfigs, "Algorithm configs cannot be null"));
        this.parallelExecution = parallelExecution;

        if (parallelExecution) {
            this.executorService = Executors.newFixedThreadPool(
                    Math.max(1, threadPoolSize),
                    this.createThreadFactory()
            );
        } else {
            this.executorService = null;
        }
    }

    public Algorithms(ImageAlgorithm.PrepareProcess prepareProcess) {
        this(createDefaultAlgorithms(prepareProcess));
    }

    private static List<AlgorithmConfig> createDefaultAlgorithms(ImageAlgorithm.PrepareProcess prepareProcess) {
        List<AlgorithmConfig> configs = new ArrayList<>();
        ImageAlgorithm rectangleAlgorithm = new RectangleAlgorithm();
        ImageAlgorithm cornerAlgorithm = new CornerAlgorithm();
        rectangleAlgorithm.setPrepareProcess(prepareProcess);
        cornerAlgorithm.setPrepareProcess(prepareProcess);

        configs.add(new AlgorithmConfig("Rectangle_Default", rectangleAlgorithm::algorithm));
        configs.add(new AlgorithmConfig("Rectangle_V1", data -> rectangleAlgorithm.algorithm(data, RectangleAlgorithm.V1)));
        configs.add(new AlgorithmConfig("Rectangle_V2", data -> rectangleAlgorithm.algorithm(data, RectangleAlgorithm.V2)));
        configs.add(new AlgorithmConfig("Rectangle_V3", data -> rectangleAlgorithm.algorithm(data, RectangleAlgorithm.V3)));
        configs.add(new AlgorithmConfig("Rectangle_V4", data -> rectangleAlgorithm.algorithm(data, RectangleAlgorithm.V4)));

        configs.add(new AlgorithmConfig("Corner_Default", cornerAlgorithm::algorithm));
        configs.add(new AlgorithmConfig("Corner_V1", data -> cornerAlgorithm.algorithm(data, CornerAlgorithm.V1)));
        configs.add(new AlgorithmConfig("Corner_V2", data -> cornerAlgorithm.algorithm(data, CornerAlgorithm.V2)));
        configs.add(new AlgorithmConfig("Corner_V3", data -> cornerAlgorithm.algorithm(data, CornerAlgorithm.V3)));
        configs.add(new AlgorithmConfig("Corner_V4", data -> cornerAlgorithm.algorithm(data, CornerAlgorithm.V4)));

        return configs;
    }

    public Point[][] algorithm(Mat data) {
        AlgorithmResult[] results = this.executeAlgorithms(data);
        return Arrays.stream(results)
                .filter(AlgorithmResult::hasValidPoints)
                .map(AlgorithmResult::getPoints)
                .toArray(Point[][]::new);
    }

    public AlgorithmResult[] executeAlgorithms(Mat data) {
        Objects.requireNonNull(data, "Input data cannot be null");

        if (data.empty()) {
            throw new IllegalArgumentException("Input matrix is empty");
        }

        List<AlgorithmConfig> enabledConfigs = this.algorithmConfigs.stream()
                .filter(AlgorithmConfig::isEnabled)
                .collect(Collectors.toList());

        if (enabledConfigs.isEmpty()) {
            return new AlgorithmResult[0];
        }

        if (this.parallelExecution) {
            return this.executeAlgorithmsParallel(data, enabledConfigs);
        } else {
            return this.executeAlgorithmsSequential(data, enabledConfigs);
        }
    }

    private AlgorithmResult[] executeAlgorithmsSequential(Mat data, List<AlgorithmConfig> configs) {
        AlgorithmResult[] results = new AlgorithmResult[configs.size()];
        Mat clonedData = data.clone();

        try {
            for (int i = 0; i < configs.size(); i++) {
                AlgorithmConfig config = configs.get(i);
                results[i] = this.executeSingleAlgorithm(clonedData, config);
            }
        } finally {
            clonedData.release();
        }

        return results;
    }

    private AlgorithmResult[] executeAlgorithmsParallel(Mat data, List<AlgorithmConfig> configs) {
        List<Mat> clonedDataList = Collections.nCopies(configs.size(), data.clone());

        try {
            List<Future<AlgorithmResult>> futures = IntStream.range(0, configs.size())
                    .mapToObj(i -> {
                        AlgorithmConfig config = configs.get(i);
                        Mat clonedData = clonedDataList.get(i);
                        return this.executorService.submit(() ->
                                this.executeSingleAlgorithm(clonedData, config)
                        );
                    })
                    .toList();

            AlgorithmResult[] results = new AlgorithmResult[configs.size()];
            for (int i = 0; i < futures.size(); i++) {
                try {
                    results[i] = futures.get(i).get(
                            configs.get(i).getTimeoutMs(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (TimeoutException e) {
                    results[i] = new AlgorithmResult(
                            configs.get(i).getName(),
                            "Algorithm timeout after " + configs.get(i).getTimeoutMs() + "ms",
                            configs.get(i).getTimeoutMs()
                    );
                } catch (ExecutionException e) {
                    results[i] = new AlgorithmResult(
                            configs.get(i).getName(),
                            "Algorithm failed: " + e.getCause().getMessage(),
                            0
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    results[i] = new AlgorithmResult(
                            configs.get(i).getName(),
                            "Algorithm interrupted",
                            0
                    );
                }
            }

            return results;

        } finally {
            clonedDataList.forEach(Mat::release);
        }
    }

    private AlgorithmResult executeSingleAlgorithm(Mat data, AlgorithmConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            Point[] points = config.getSolver().solve(data);
            long executionTime = System.currentTimeMillis() - startTime;

            return new AlgorithmResult(config.getName(), points, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new AlgorithmResult(
                    config.getName(),
                    "Algorithm error: " + e.getMessage(),
                    executionTime
            );
        }
    }

    public List<String> getAlgorithmNames() {
        return this.algorithmConfigs.stream()
                .map(AlgorithmConfig::getName)
                .collect(Collectors.toList());
    }

    public void enableAlgorithm(String algorithmName, boolean enabled) {
        this.algorithmConfigs.stream()
                .filter(config -> config.getName().equals(algorithmName))
                .findFirst()
                .ifPresent(config -> {
                    int index = this.algorithmConfigs.indexOf(config);
                    if (index != -1) {
                        AlgorithmConfig newConfig = new AlgorithmConfig(
                                config.getName(),
                                config.getSolver(),
                                enabled,
                                config.getTimeoutMs()
                        );
                        this.algorithmConfigs.set(index, newConfig);
                    }
                });
    }

    public void setAlgorithmTimeout(String algorithmName, int timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }

        this.algorithmConfigs.stream()
                .filter(config -> config.getName().equals(algorithmName))
                .findFirst()
                .ifPresent(config -> {
                    int index = this.algorithmConfigs.indexOf(config);
                    if (index != -1) {
                        AlgorithmConfig newConfig = new AlgorithmConfig(
                                config.getName(),
                                config.getSolver(),
                                config.isEnabled(),
                                timeoutMs
                        );
                        this.algorithmConfigs.set(index, newConfig);
                    }
                });
    }

    public void addAlgorithm(AlgorithmConfig config) {
        this.algorithmConfigs.add(Objects.requireNonNull(config, "Algorithm config cannot be null"));
    }

    public void removeAlgorithm(String algorithmName) {
        this.algorithmConfigs.removeIf(config -> config.getName().equals(algorithmName));
    }

    public Statistics getStatistics(Mat data) {
        AlgorithmResult[] results = this.executeAlgorithms(data);
        return new Statistics(results);
    }

    @Setter
    @Getter
    public static class AlgorithmConfig {
        private final String name;
        private final Solver solver;
        private final boolean enabled;
        private final int timeoutMs;

        public AlgorithmConfig(String name, Solver solver, boolean enabled, int timeoutMs) {
            this.name = name;
            this.solver = solver;
            this.enabled = enabled;
            this.timeoutMs = timeoutMs;
        }

        public AlgorithmConfig(String name, Solver solver) {
            this(name, solver, true, 2 * 60 * 1000);
        }

    }

    @Setter
    @Getter
    public static class AlgorithmResult {
        private final String algorithmName;
        private final Point[] points;
        private final long executionTimeMs;
        private final boolean success;
        private final String errorMessage;

        public AlgorithmResult(String algorithmName, Point[] points, long executionTimeMs) {
            this.algorithmName = algorithmName;
            this.points = points != null ? points.clone() : new Point[0];
            this.executionTimeMs = executionTimeMs;
            this.success = true;
            this.errorMessage = null;
        }

        public AlgorithmResult(String algorithmName, String errorMessage, long executionTimeMs) {
            this.algorithmName = algorithmName;
            this.points = new Point[0];
            this.executionTimeMs = executionTimeMs;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public boolean hasValidPoints() {
            return success && points != null && points.length == 4;
        }

        @Override
        public String toString() {
            return "AlgorithmResult{" +
                    "algorithmName='" + algorithmName + '\'' +
                    ", success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    ", points=" + Arrays.toString(points) +
                    '}';
        }
    }


    @Setter
    @Getter
    public static class Statistics {
        private final AlgorithmResult[] results;
        private final int totalAlgorithms;
        private final int successfulAlgorithms;
        private final int algorithmsWithValidPoints;
        private final double averageExecutionTime;
        private final AlgorithmResult fastestAlgorithm;
        private final AlgorithmResult bestAlgorithm; // с наибольшим количеством валидных точек

        public Statistics(AlgorithmResult[] results) {
            this.results = results != null ? results.clone() : new AlgorithmResult[0];
            this.totalAlgorithms = this.results.length;
            this.successfulAlgorithms = (int) Arrays.stream(this.results)
                    .filter(AlgorithmResult::isSuccess)
                    .count();
            this.algorithmsWithValidPoints = (int) Arrays.stream(this.results)
                    .filter(AlgorithmResult::hasValidPoints)
                    .count();
            this.averageExecutionTime = Arrays.stream(this.results)
                    .mapToLong(AlgorithmResult::getExecutionTimeMs)
                    .average()
                    .orElse(0);
            this.fastestAlgorithm = Arrays.stream(this.results)
                    .filter(AlgorithmResult::isSuccess)
                    .min(Comparator.comparingLong(AlgorithmResult::getExecutionTimeMs))
                    .orElse(null);
            this.bestAlgorithm = Arrays.stream(this.results)
                    .filter(AlgorithmResult::hasValidPoints)
                    .max(Comparator.comparingInt(r -> r.getPoints().length))
                    .orElse(null);
        }

        @Override
        public String toString() {
            return String.format(
                    "Statistics{total=%d, successful=%d, validPoints=%d, avgTime=%.2fms}",
                    totalAlgorithms, successfulAlgorithms, algorithmsWithValidPoints, averageExecutionTime
            );
        }
    }

    private ThreadFactory createThreadFactory() {
        return r -> {
            Thread thread = new Thread(r, "Algorithm-Executor-" + System.currentTimeMillis());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        };
    }

    public void shutdown() {
        if (this.executorService != null) {
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}