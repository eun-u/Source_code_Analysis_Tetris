package kr.ac.jbnu.se.tetris.network.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 중복 키와 잘못된 UTF-8 및 과도한 중첩을 거부하는 제한된 JSON 처리 */
public final class StrictJson {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_STRING_CHARS = 8192;

    private StrictJson() { }

    public static Object parse(byte[] utf8) throws IOException {
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(utf8)).toString();
        } catch (CharacterCodingException error) {
            throw new IOException("Malformed UTF-8", error);
        }
        Parser parser = new Parser(json);
        Object value = parser.value(0);
        parser.spaces();
        if (parser.position != json.length()) throw new IOException("Trailing JSON content");
        return value;
    }

    public static byte[] stringify(Object value) throws IOException {
        StringBuilder json = new StringBuilder();
        append(json, value, 0);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void append(StringBuilder out, Object value, int depth) throws IOException {
        if (depth > MAX_DEPTH) throw new IOException("JSON nesting limit exceeded");
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            quoted(out, (String) value);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Map) {
            appendObject(out, (Map<?, ?>) value, depth);
        } else if (value instanceof List) {
            appendArray(out, (List<?>) value, depth);
        } else {
            throw new IOException("Unsupported JSON value");
        }
    }

    private static void appendObject(StringBuilder out, Map<?, ?> map, int depth) throws IOException {
        if (map.size() > MAX_ENTRIES) throw new IOException("JSON object too large");
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String)) throw new IOException("JSON key must be text");
            if (!first) out.append(',');
            first = false;
            quoted(out, (String) entry.getKey());
            out.append(':');
            append(out, entry.getValue(), depth + 1);
        }
        out.append('}');
    }

    private static void appendArray(StringBuilder out, List<?> list, int depth) throws IOException {
        if (list.size() > MAX_ENTRIES) throw new IOException("JSON array too large");
        out.append('[');
        for (int index = 0; index < list.size(); index++) {
            if (index > 0) out.append(',');
            append(out, list.get(index), depth + 1);
        }
        out.append(']');
    }

    private static void quoted(StringBuilder out, String value) throws IOException {
        if (value.length() > MAX_STRING_CHARS) throw new IOException("JSON string too long");
        out.append('"');
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (Character.isHighSurrogate(c)) {
                if (!hasLowSurrogateAt(value, index + 1)) throw new IOException("Unpaired Unicode surrogate");
                out.append(c).append(value.charAt(++index));
            } else if (Character.isLowSurrogate(c)) {
                throw new IOException("Unpaired Unicode surrogate");
            } else {
                appendEscaped(out, c);
            }
        }
        out.append('"');
    }

    private static boolean hasLowSurrogateAt(CharSequence value, int index) {
        return index < value.length() && Character.isLowSurrogate(value.charAt(index));
    }

    /** 따옴표·역슬래시·제어 문자만 이스케이프하고 나머지는 그대로 쓴다. */
    private static void appendEscaped(StringBuilder out, char c) {
        if (c == '"' || c == '\\') {
            out.append('\\').append(c);
        } else if (c < 0x20) {
            String hex = Integer.toHexString(c);
            out.append("\\u");
            for (int pad = hex.length(); pad < 4; pad++) out.append('0');
            out.append(hex);
        } else {
            out.append(c);
        }
    }

    private static final class Parser {
        private final String text;
        private int position;

        private Parser(String text) { this.text = text; }

        private void spaces() {
            while (position < text.length()) {
                char c = text.charAt(position);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') return;
                position++;
            }
        }

        private Object value(int depth) throws IOException {
            if (depth > MAX_DEPTH) throw new IOException("JSON nesting limit exceeded");
            spaces();
            if (position == text.length()) throw new IOException("Unexpected end of JSON");
            char c = text.charAt(position);
            if (c == '{') return object(depth + 1);
            if (c == '[') return array(depth + 1);
            if (c == '"') return string();
            if (c == 't') { literal("true"); return Boolean.TRUE; }
            if (c == 'f') { literal("false"); return Boolean.FALSE; }
            if (c == 'n') { literal("null"); return null; }
            if (c == '-' || (c >= '0' && c <= '9')) return number();
            throw new IOException("Invalid JSON value");
        }

        private Map<String, Object> object(int depth) throws IOException {
            position++;
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            spaces();
            if (take('}')) return result;
            while (true) {
                spaces();
                if (position >= text.length() || text.charAt(position) != '"') {
                    throw new IOException("JSON object key is required");
                }
                String key = string();
                spaces();
                if (!take(':')) throw new IOException("JSON object separator is required");
                Object value = value(depth);
                if (result.containsKey(key) || result.size() >= MAX_ENTRIES) {
                    throw new IOException("Duplicate or excessive JSON object key");
                }
                result.put(key, value);
                spaces();
                if (take('}')) return result;
                if (!take(',')) throw new IOException("JSON object comma is required");
            }
        }

        private List<Object> array(int depth) throws IOException {
            position++;
            List<Object> result = new ArrayList<Object>();
            spaces();
            if (take(']')) return result;
            while (true) {
                result.add(value(depth));
                if (result.size() > MAX_ENTRIES) throw new IOException("JSON array too large");
                spaces();
                if (take(']')) return result;
                if (!take(',')) throw new IOException("JSON array comma is required");
            }
        }

        private String string() throws IOException {
            position++;
            StringBuilder value = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') {
                    if (value.length() > MAX_STRING_CHARS) throw new IOException("JSON string too long");
                    validateSurrogates(value);
                    return value.toString();
                }
                if (c == '\\') {
                    c = escape();
                } else if (c < 0x20) {
                    throw new IOException("Unescaped control character");
                }
                value.append(c);
                if (value.length() > MAX_STRING_CHARS) throw new IOException("JSON string too long");
            }
            throw new IOException("Unterminated JSON string");
        }

        /** 역슬래시 다음의 이스케이프 한 개를 해석한다. */
        private char escape() throws IOException {
            if (position == text.length()) throw new IOException("Incomplete JSON escape");
            char escaped = text.charAt(position++);
            switch (escaped) {
                case 'u': return unicodeEscape();
                case '"':
                case '\\':
                case '/': return escaped;
                case 'b': return '\b';
                case 'f': return '\f';
                case 'n': return '\n';
                case 'r': return '\r';
                case 't': return '\t';
                default: throw new IOException("Invalid JSON escape");
            }
        }

        private char unicodeEscape() throws IOException {
            if (position + 4 > text.length()) throw new IOException("Incomplete Unicode escape");
            int codePoint = 0;
            for (int digitIndex = 0; digitIndex < 4; digitIndex++) {
                char hex = text.charAt(position + digitIndex);
                // Character.digit은 전각 숫자도 받으므로 ASCII 16진수만 허용한다.
                if (!isAsciiHex(hex)) throw new IOException("Invalid Unicode escape");
                codePoint = (codePoint << 4) | Character.digit(hex, 16);
            }
            position += 4;
            return (char) codePoint;
        }

        private boolean isAsciiHex(char hex) {
            return (hex >= '0' && hex <= '9') || (hex >= 'a' && hex <= 'f') || (hex >= 'A' && hex <= 'F');
        }

        private Long number() throws IOException {
            int start = position;
            if (take('-') && position == text.length()) throw new IOException("Invalid JSON number");
            if (take('0')) {
                if (position < text.length() && Character.isDigit(text.charAt(position))) {
                    throw new IOException("Leading zero in JSON number");
                }
            } else {
                integerDigits();
            }
            if (position < text.length() && isFractionOrExponent(text.charAt(position))) {
                throw new IOException("Non-integer JSON number");
            }
            try { return Long.valueOf(text.substring(start, position)); }
            catch (NumberFormatException error) { throw new IOException("JSON integer overflow", error); }
        }

        /** 0이 아닌 첫 자리부터 이어지는 10진 숫자열을 읽는다. */
        private void integerDigits() throws IOException {
            if (position >= text.length() || text.charAt(position) < '1' || text.charAt(position) > '9') {
                throw new IOException("Invalid JSON number");
            }
            while (position < text.length() && text.charAt(position) >= '0' && text.charAt(position) <= '9') {
                position++;
            }
        }

        private boolean isFractionOrExponent(char suffix) {
            return suffix == '.' || suffix == 'e' || suffix == 'E';
        }

        private void literal(String value) throws IOException {
            if (!text.startsWith(value, position)) throw new IOException("Invalid JSON literal");
            position += value.length();
        }

        private boolean take(char c) {
            if (position < text.length() && text.charAt(position) == c) {
                position++;
                return true;
            }
            return false;
        }

        private void validateSurrogates(CharSequence value) throws IOException {
            for (int index = 0; index < value.length(); index++) {
                char c = value.charAt(index);
                if (Character.isHighSurrogate(c)) {
                    if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(++index))) {
                        throw new IOException("Unpaired Unicode surrogate");
                    }
                } else if (Character.isLowSurrogate(c)) {
                    throw new IOException("Unpaired Unicode surrogate");
                }
            }
        }
    }
}
