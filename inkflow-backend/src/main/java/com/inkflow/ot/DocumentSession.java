package com.inkflow.ot;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * In-memory canonical OT state for a single open document: the current
 * content as a Delta, its version, and a bounded history of recently
 * applied ops (so an incoming op can be transformed against every op
 * committed since the client's baseVersion). Single-instance only — see
 * docs/architecture.md "Open decisions"; Redis-backed state is needed before
 * running more than one backend instance.
 */
public final class DocumentSession {

    private static final int MAX_HISTORY = 200;

    private Delta content;
    private long version;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();

    private record HistoryEntry(long version, Delta op) {
    }

    public DocumentSession(Delta initialContent, long initialVersion) {
        this.content = initialContent;
        this.version = initialVersion;
    }

    public synchronized long version() {
        return version;
    }

    public synchronized Delta content() {
        return content;
    }

    /**
     * Transforms {@code incomingOp} against every op applied since
     * {@code baseVersion}, applies the result, bumps the version, and
     * records it in history. Returns the transformed op (what actually got
     * applied, and what should be broadcast) together with the new version.
     */
    public synchronized AppliedOp apply(Delta incomingOp, long baseVersion) {
        Delta transformed = incomingOp;
        for (HistoryEntry entry : history) {
            if (entry.version() > baseVersion) {
                transformed = entry.op().transform(transformed, true);
            }
        }

        content = content.compose(transformed);
        version += 1;
        history.addLast(new HistoryEntry(version, transformed));
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }

        return new AppliedOp(transformed, version);
    }

    public record AppliedOp(Delta op, long version) {
    }
}
