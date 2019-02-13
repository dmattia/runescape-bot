package util.common;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ActivityCollector implements Collector<Activity, Activity.Builder, Activity.Builder> {
    @Override
    public Supplier<Activity.Builder> supplier() {
        return () -> Activity.newBuilder();
    }

    @Override
    public BiConsumer<Activity.Builder, Activity> accumulator() {
        return (builder, activity) -> builder.addSubActivity(activity);
    }

    @Override
    public BinaryOperator<Activity.Builder> combiner() {
        return (first, second) -> first.addSubActivity(second.build());
    }

    @Override
    public Function<Activity.Builder, Activity.Builder> finisher() {
        return builder -> builder;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
