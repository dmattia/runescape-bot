package util.common;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ActivityCollector implements Collector<Activity, Activity.Builder, Activity> {
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
    public Function<Activity.Builder, Activity> finisher() {
        return Activity.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    /*
    @Override
    public BiConsumer<Activity, Activity> accumulator() {
        return (first, second) -> first.andThen(second);
    }

    @Override
    public BinaryOperator<Activity> combiner() {
        return (first, second) -> first.andThen(second);
    }

    @Override
    public Function<Activity, Activity> finisher() {
        return activity -> activity;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.IDENTITY_FINISH);
    }
    */
}