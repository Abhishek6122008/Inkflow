package com.inkflow.ot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A single Quill Delta operation: insert, delete, or retain. Mirrors the
 * semantics of dart_quill_delta's Operation class so the server-side OT
 * transform/compose logic matches the Flutter client exactly.
 */
public final class Operation {

    public enum Kind { INSERT, DELETE, RETAIN }

    private final Kind kind;
    private final int length;
    private final Object data;
    private final Map<String, Object> attributes;

    private Operation(Kind kind, int length, Object data, Map<String, Object> attributes) {
        this.kind = kind;
        this.length = length;
        this.data = data;
        this.attributes = (attributes == null || attributes.isEmpty()) ? null : attributes;
    }

    public static Operation insert(Object data, Map<String, Object> attributes) {
        int length = data instanceof String s ? s.length() : 1;
        return new Operation(Kind.INSERT, length, data, attributes);
    }

    public static Operation delete(int length) {
        return new Operation(Kind.DELETE, length, "", null);
    }

    public static Operation retain(int length, Map<String, Object> attributes) {
        return new Operation(Kind.RETAIN, length, "", attributes);
    }

    public Kind kind() {
        return kind;
    }

    public int length() {
        return length;
    }

    public Object data() {
        return data;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public boolean isInsert() {
        return kind == Kind.INSERT;
    }

    public boolean isDelete() {
        return kind == Kind.DELETE;
    }

    public boolean isRetain() {
        return kind == Kind.RETAIN;
    }

    public boolean isPlain() {
        return attributes == null || attributes.isEmpty();
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public boolean hasSameAttributes(Operation other) {
        boolean thisEmpty = attributes == null || attributes.isEmpty();
        boolean otherEmpty = other.attributes == null || other.attributes.isEmpty();
        if (thisEmpty && otherEmpty) {
            return true;
        }
        return Objects.equals(attributes, other.attributes);
    }

    /** Returns a copy of this operation with new {@code length}/{@code data}, preserving kind+attributes. */
    Operation withLengthAndData(int newLength, Object newData) {
        return new Operation(kind, newLength, newData, attributes);
    }

    @SuppressWarnings("unchecked")
    public static Operation fromJson(Map<String, Object> json) {
        if (json.containsKey("insert")) {
            Object data = json.get("insert");
            Map<String, Object> attrs = (Map<String, Object>) json.get("attributes");
            return insert(data, attrs);
        } else if (json.containsKey("delete")) {
            int length = ((Number) json.get("delete")).intValue();
            return delete(length);
        } else if (json.containsKey("retain")) {
            int length = ((Number) json.get("retain")).intValue();
            Map<String, Object> attrs = (Map<String, Object>) json.get("attributes");
            return retain(length, attrs);
        }
        throw new IllegalArgumentException("Invalid Delta operation JSON: " + json);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        switch (kind) {
            case INSERT -> json.put("insert", data);
            case DELETE -> json.put("delete", length);
            case RETAIN -> json.put("retain", length);
        }
        if (attributes != null) {
            json.put("attributes", attributes);
        }
        return json;
    }

    @Override
    public String toString() {
        return kind + "(" + (isInsert() ? data : length) + (attributes != null ? " " + attributes : "") + ")";
    }
}
