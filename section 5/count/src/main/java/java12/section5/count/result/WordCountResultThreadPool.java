package java12.section5.count.result;

import java12.section5.count.split.SplitToWords;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WordCountResultThreadPool {

    private BiFunction<Map<String, Integer>, String, Map<String, Integer>> acc = (Map<String, Integer> map, String word) -> {
        String key = word.toLowerCase();
        var count = map.get(key);
        map.put(key, count == null ? 0 : count + 1);
        return map;
    };
    private BinaryOperator<Map<String, Integer>> combiner = (a, b) -> {
        a.putAll(b);
        return a;
    };

    @SuppressWarnings("UnnecessaryLocalVariable")
    public List<String> count(List<String> lines, int limit) throws ExecutionException, InterruptedException {
        var factory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;
        Thread.UncaughtExceptionHandler exceptionHandler = (t, e) -> {
            System.err.println(t.getName());
            e.printStackTrace();
        };
        ForkJoinPool forkJoinPool = new ForkJoinPool(3, factory, exceptionHandler, true);
        Stream<String> linesPool =
                forkJoinPool.submit(lines::parallelStream).get();
        Stream<String> words = new SplitToWords().split(linesPool);
        Map<String, Integer> m = words.reduce(new ConcurrentHashMap<>(), acc, combiner);
        Stream<String> result = m.entrySet().parallelStream()
                .filter((e) -> e.getValue() > limit && e.getKey().length() > 4 && !e.getKey().startsWith("["))
                .map(Map.Entry::getKey).sorted(String::compareToIgnoreCase);
        return result.collect(Collectors.toList());
    }
}
