package com.inkflow.ot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java port of dart_quill_delta's Delta: a sequence of insert/retain/delete
 * operations. {@link #compose} applies a change delta to a document delta;
 * {@link #transform} resolves two concurrent change deltas against each
 * other (the core of the OT algorithm). Ported line-for-line from
 * flutter_quill's dart_quill_delta package so the server's transform/compose
 * results are identical to what the Flutter client would produce, which is
 * required for both sides to converge on the same document state.
 */
public final class Delta {

    private final List<Operation> operations = new ArrayList<>();
    private int modificationCount = 0;

    public static Delta empty() {
        return new Delta();
    }

    public static Delta fromJson(List<Map<String, Object>> json) {
        Delta delta = new Delta();
        for (Map<String, Object> opJson : json) {
            delta.operations.add(Operation.fromJson(opJson));
        }
        return delta;
    }

    public List<Map<String, Object>> toJson() {
        List<Map<String, Object>> json = new ArrayList<>();
        for (Operation op : operations) {
            json.add(op.toJson());
        }
        return json;
    }

    public int size() {
        return operations.size();
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    Operation get(int index) {
        return operations.get(index);
    }

    public Delta insert(Object data, Map<String, Object> attributes) {
        if (data instanceof String s && s.isEmpty()) {
            return this;
        }
        return push(Operation.insert(data, attributes));
    }

    public Delta delete(int length) {
        if (length <= 0) {
            return this;
        }
        return push(Operation.delete(length));
    }

    public Delta retain(int length, Map<String, Object> attributes) {
        if (length <= 0) {
            return this;
        }
        return push(Operation.retain(length, attributes));
    }

    /** Appends {@code operation}, merging with the current tail where dart_quill_delta would. */
    public Delta push(Operation operation) {
        if (operation.isEmpty()) {
            return this;
        }
        int index = operations.size();
        Operation lastOp = operations.isEmpty() ? null : operations.get(operations.size() - 1);
        if (lastOp != null) {
            if (lastOp.isDelete() && operation.isDelete()) {
                mergeWithTail(operation);
                return this;
            }
            if (lastOp.isDelete() && operation.isInsert()) {
                index -= 1;
                if (index == 0) {
                    operations.add(0, operation);
                    modificationCount++;
                    return this;
                }
            }
            if (lastOp.isInsert() && operation.isInsert()
                    && lastOp.hasSameAttributes(operation)
                    && operation.data() instanceof String && lastOp.data() instanceof String) {
                mergeWithTail(operation);
                return this;
            }
            if (lastOp.isRetain() && operation.isRetain() && lastOp.hasSameAttributes(operation)) {
                mergeWithTail(operation);
                return this;
            }
        }
        if (index == operations.size()) {
            operations.add(operation);
        } else {
            operations.add(index, operation);
        }
        modificationCount++;
        return this;
    }

    private void mergeWithTail(Operation operation) {
        Operation last = operations.get(operations.size() - 1);
        int length = operation.length() + last.length();
        String mergedText = (String) last.data() + (String) operation.data();
        operations.set(operations.size() - 1, last.withLengthAndData(length, mergedText));
        modificationCount++;
    }

    /** Removes a trailing plain retain (no-op tail), matching dart_quill_delta's trim(). */
    public Delta trim() {
        if (!operations.isEmpty()) {
            Operation last = operations.get(operations.size() - 1);
            if (last.isRetain() && last.isPlain()) {
                operations.remove(operations.size() - 1);
            }
        }
        return this;
    }

    private static Map<String, Object> composeAttributes(Map<String, Object> a, Map<String, Object> b, boolean keepNull) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (a != null) result.putAll(a);
        if (b != null) result.putAll(b);
        if (!keepNull) {
            result.values().removeIf(v -> v == null);
        }
        return result.isEmpty() ? null : result;
    }

    private static Map<String, Object> transformAttributes(Map<String, Object> a, Map<String, Object> b, boolean priority) {
        if (a == null) return b;
        if (b == null) return null;
        if (!priority) return b;

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : b.entrySet()) {
            if (!a.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result.isEmpty() ? null : result;
    }

    private Operation composeOperation(DeltaIterator thisIter, DeltaIterator otherIter) {
        if (otherIter.isNextInsert()) {
            return otherIter.next();
        }
        if (thisIter.isNextDelete()) {
            return thisIter.next();
        }

        int length = Math.min(thisIter.peekLength(), otherIter.peekLength());
        Operation thisOp = thisIter.next(length);
        Operation otherOp = otherIter.next(length);

        if (otherOp.isRetain()) {
            Map<String, Object> attributes = composeAttributes(thisOp.attributes(), otherOp.attributes(), thisOp.isRetain());
            if (thisOp.isRetain()) {
                return Operation.retain(thisOp.length(), attributes);
            } else if (thisOp.isInsert()) {
                return Operation.insert(thisOp.data(), attributes);
            }
            throw new IllegalStateException("Unreachable");
        } else {
            if (thisOp.isRetain()) {
                return otherOp;
            }
        }
        return null;
    }

    /** Applies change delta {@code other} on top of this (document) delta. */
    public Delta compose(Delta other) {
        Delta result = new Delta();
        DeltaIterator thisIter = new DeltaIterator(this);
        DeltaIterator otherIter = new DeltaIterator(other);

        while (thisIter.hasNext() || otherIter.hasNext()) {
            Operation newOp = composeOperation(thisIter, otherIter);
            if (newOp != null) {
                result.push(newOp);
            }
        }
        return result.trim();
    }

    private Operation transformOperation(DeltaIterator thisIter, DeltaIterator otherIter, boolean priority) {
        if (thisIter.isNextInsert() && (priority || !otherIter.isNextInsert())) {
            return Operation.retain(thisIter.next().length(), null);
        } else if (otherIter.isNextInsert()) {
            return otherIter.next();
        }

        int length = Math.min(thisIter.peekLength(), otherIter.peekLength());
        Operation thisOp = thisIter.next(length);
        Operation otherOp = otherIter.next(length);

        if (thisOp.isDelete()) {
            return null;
        } else if (otherOp.isDelete()) {
            return otherOp;
        } else {
            return Operation.retain(length, transformAttributes(thisOp.attributes(), otherOp.attributes(), priority));
        }
    }

    /**
     * Transforms {@code other} against this delta. {@code priority} should be
     * true when this delta is considered to have happened first (i.e. when
     * called from the side that already applied its own op and is
     * transforming an incoming concurrent op against it).
     */
    public Delta transform(Delta other, boolean priority) {
        Delta result = new Delta();
        DeltaIterator thisIter = new DeltaIterator(this);
        DeltaIterator otherIter = new DeltaIterator(other);

        while (thisIter.hasNext() || otherIter.hasNext()) {
            Operation newOp = transformOperation(thisIter, otherIter, priority);
            if (newOp != null) {
                result.push(newOp);
            }
        }
        return result.trim();
    }
}
