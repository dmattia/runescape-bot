package misc.zulrah;

import org.rspeer.runetek.api.component.tab.Prayer;
import util.Activities;
import util.common.Activity;

import java.util.Objects;

public class ZulrahState {
    private ZulrahStyle style;
    private ZulrahPosition posiiton;

    public static ZulrahState classify() {
        return new ZulrahState(ZulrahStyle.classify(), ZulrahPosition.classify());
    }

    public boolean isValid() {
        return style != ZulrahStyle.UNKNOWN && posiiton != ZulrahPosition.UNKNOWN;
    }

    ZulrahState(ZulrahStyle style, ZulrahPosition posiiton) {
        this.style = style;
        this.posiiton = posiiton;
    }

    @Override
    public String toString() {
        return "ZulrahState{" +
                "style=" + style +
                ", posiiton=" + posiiton +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZulrahState that = (ZulrahState) o;
        return style == that.style &&
                posiiton == that.posiiton;
    }

    @Override
    public int hashCode() {
        return Objects.hash(style, posiiton);
    }
}
