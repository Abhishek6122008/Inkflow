package com.inkflow.ot;

/**
 * Walks a {@link Delta}'s operations one slice at a time, splitting
 * operations as needed. Port of dart_quill_delta's DeltaIterator, required
 * to make {@link Delta#transform} / {@link Delta#compose} byte-for-byte
 * compatible with the Flutter client's transform of the same ops.
 */
final class DeltaIterator {

    static final int MAX_LENGTH = 1_073_741_824;

    private final Delta delta;
    private int index = 0;
    private int offset = 0;

    DeltaIterator(Delta delta) {
        this.delta = delta;
    }

    boolean isNextInsert() {
        return nextKind() == Operation.Kind.INSERT;
    }

    boolean isNextDelete() {
        return nextKind() == Operation.Kind.DELETE;
    }

    private Operation.Kind nextKind() {
        return index < delta.size() ? delta.get(index).kind() : null;
    }

    boolean hasNext() {
        return peekLength() < MAX_LENGTH;
    }

    int peekLength() {
        if (index < delta.size()) {
            return delta.get(index).length() - offset;
        }
        return MAX_LENGTH;
    }

    Operation next() {
        return next(MAX_LENGTH);
    }

    Operation next(int length) {
        if (index < delta.size()) {
            Operation op = delta.get(index);
            int currentOffset = offset;
            int actualLength = Math.min(op.length() - currentOffset, length);
            if (actualLength == op.length() - currentOffset) {
                index++;
                offset = 0;
            } else {
                offset += actualLength;
            }
            Object opData;
            if (op.isInsert() && op.data() instanceof String s) {
                opData = s.substring(currentOffset, currentOffset + actualLength);
            } else {
                opData = op.data();
            }
            boolean isNotEmpty = opData instanceof String s ? !s.isEmpty() : true;
            int opLength = opData instanceof String s ? s.length() : 1;
            int opActualLength = isNotEmpty ? opLength : actualLength;
            return op.withLengthAndData(opActualLength, opData);
        }
        return Operation.retain(length, null);
    }
}
