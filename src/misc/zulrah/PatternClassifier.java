package misc.zulrah;

import org.rspeer.runetek.api.scene.Npcs;
import skills.Combat;
import util.Activities;
import util.common.Activity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PatternClassifier {
    private static List<PatternPhase> pattern1 = Arrays.asList(
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_1)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_1)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setActivity(Activity.of(() -> System.out.println("Jad phase starting range")))
                    .build(), // JAD Range
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_WEST)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_WEST)
                    .useDefaultActivity()
                    .build());

    private static List<PatternPhase> pattern2 = Arrays.asList(
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_INSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setActivity(Activity.of(() -> System.out.println("Jad phase starting range")))
                    .build(),  // JAD Range
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_WEST)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_WEST)
                    .useDefaultActivity()
                    .build());

    private static List<PatternPhase> pattern3 = Arrays.asList(
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_WEST)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE) // FIX
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_OUTSIDE) // FIX
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setActivity(Activity.of(() -> System.out.println("Jad phase starting mage")))
                    .build()); // JAD Mage

    private static List<PatternPhase> pattern4 = Arrays.asList(
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MELEE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setPlayerPosition(PlayerPosition.SE_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.SOUTH)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.WEST)
                    .setPlayerPosition(PlayerPosition.SW_PILLAR_OUTSIDE)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build(),
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.RANGED)
                    .setZulrahPosition(ZulrahPosition.EAST)
                    .setActivity(Activity.of(() -> System.out.println("Jad phase starting mage")))
                    .build(), // JAD Mage
            PatternPhase.newBuilder()
                    .setZulrahStyle(ZulrahStyle.MAGE)
                    .setZulrahPosition(ZulrahPosition.NORTH)
                    .setPlayerPosition(PlayerPosition.NORTH_EAST_2)
                    .useDefaultActivity()
                    .build());

    private static int longestMatchingSubArray(List<PatternPhase> pattern, List<ZulrahState> history) {
        List<ZulrahState> patternStates = pattern.stream()
                .map(PatternPhase::getZulrahState)
                .collect(Collectors.toList());

        return IntStream.range(0, history.size())
                .mapToObj(startIndex -> history.subList(startIndex, history.size()))
                .filter(subList -> Collections.indexOfSubList(patternStates, subList) == 0)
                .mapToInt(subList -> subList.size())
                .max()
                .orElse(0);
    }

    /**
     * What I really want here is the Longest-Common-Subsequence problem with the caveat that the last elements in the
     * visited Zulrah States must be a part of the subsequence. This is a significant reduction in complexity, from
     * NP-Hard to a sequence check on all subsequences containing the last element where the longest exact match
     * is the pattern, with preference to subsequences that occur at the beginning of a pattern.
     */
    public static Optional<PatternPhase> classifyPattern() {
        List<ZulrahState> seenPhases = PhaseClassifier.checkPhase();
        return Stream.of(pattern1, pattern2, pattern3, pattern4)
                .filter(pattern -> longestMatchingSubArray(pattern, seenPhases) != 0)
                .sorted((p1, p2) -> longestMatchingSubArray(p2, seenPhases) - longestMatchingSubArray(p1, seenPhases))
                .findFirst()
                .map(pattern -> pattern.get(longestMatchingSubArray(pattern, seenPhases) - 1));
    }

    public static class PatternPhase {
        private ZulrahStyle zulrahStyle;
        private ZulrahPosition zulrahPosition;
        private Optional<PlayerPosition> playerPosition;
        private Optional<Activity> customActivity;

        private PatternPhase(ZulrahStyle zulrahStyle,
                             ZulrahPosition zulrahPosition,
                             Optional<Activity> activity,
                             Optional<PlayerPosition> playerPosition) {
            this.zulrahStyle = zulrahStyle;
            this.zulrahPosition = zulrahPosition;
            this.customActivity = activity;
            this.playerPosition = playerPosition;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Activity getActivity() {
            Activity defaultActivity = Activity.newBuilder()
                    // .addSubActivity(() -> zulrahStyle.getActivity().run())
                    .addSubActivity(() -> playerPosition
                            .map(PlayerPosition::getPosition)
                            .map(Activities::moveToExactly)
                            .ifPresent(Activity::run))
                    .addSubActivity(Combat.attack("Zulrah"))
                    .build();

            return customActivity.orElse(defaultActivity);
        }

        private ZulrahState getZulrahState() {
            return new ZulrahState(zulrahStyle, zulrahPosition);
        }

        private static class Builder {
            private ZulrahStyle zulrahStyle;
            private ZulrahPosition zulrahPosition;
            private Optional<Activity> customActivity;
            private Optional<PlayerPosition> playerPosition;

            private Builder() {
                this.zulrahStyle = ZulrahStyle.UNKNOWN;
                this.zulrahPosition = ZulrahPosition.UNKNOWN;
                this.customActivity = Optional.empty();
                this.playerPosition = Optional.empty();
            }

            private Builder setZulrahStyle(ZulrahStyle zulrahStyle) {
                this.zulrahStyle = zulrahStyle;
                return this;
            }

            private Builder setZulrahPosition(ZulrahPosition zulrahPosition) {
                this.zulrahPosition = zulrahPosition;
                return this;
            }

            private Builder setPlayerPosition(PlayerPosition position) {
                this.playerPosition = Optional.of(position);
                return this;
            }

            private Builder setActivity(Activity activity) {
                this.customActivity = Optional.of(activity);
                return this;
            }

            private Builder useDefaultActivity() {
                this.customActivity = Optional.empty();
                return this;
            }

            private PatternPhase build() {
                return new PatternPhase(zulrahStyle, zulrahPosition, customActivity, playerPosition);
            }
        }
    }
}
