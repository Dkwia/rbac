package rbac.util;

import java.util.ArrayList;
import java.util.List;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }
        List<String[]> safeRows = rows == null ? List.of() : rows;
        int columns = headers.length;
        int[] widths = new int[columns];

        for (int i = 0; i < columns; i++) {
            widths[i] = headers[i] == null ? 0 : headers[i].length();
        }
        for (String[] row : safeRows) {
            if (row == null) continue;
            for (int i = 0; i < columns && i < row.length; i++) {
                String cell = row[i] == null ? "" : row[i];
                widths[i] = Math.max(widths[i], cell.length());
            }
        }

        StringBuilder sb = new StringBuilder();
        String border = buildBorder(widths);
        sb.append(border).append("\n");
        sb.append(buildRow(headers, widths)).append("\n");
        sb.append(border).append("\n");
        for (String[] row : safeRows) {
            String[] safe = row == null ? new String[columns] : row;
            sb.append(buildRow(safe, widths)).append("\n");
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatBox(String text) {
        String content = text == null ? "" : text;
        String[] lines = content.split("\\R", -1);
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, line.length());
        }
        String border = "+" + "-".repeat(max + 2) + "+";
        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n");
        for (String line : lines) {
            sb.append("| ").append(padRight(line, max)).append(" |").append("\n");
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatHeader(String text) {
        String content = text == null ? "" : text.trim();
        return "\n== " + content + " ==";
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (maxLength <= 0) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        String value = text == null ? "" : text;
        if (value.length() >= length) return value;
        return value + " ".repeat(length - value.length());
    }

    public static String padLeft(String text, int length) {
        String value = text == null ? "" : text;
        if (value.length() >= length) return value;
        return " ".repeat(length - value.length()) + value;
    }

    private static String buildBorder(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        return sb.toString();
    }

    private static String buildRow(String[] cells, int[] widths) {
        List<String> safe = new ArrayList<>();
        for (int i = 0; i < widths.length; i++) {
            String value = (cells != null && i < cells.length && cells[i] != null) ? cells[i] : "";
            safe.add(padRight(value, widths[i]));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < safe.size(); i++) {
            sb.append(" ").append(safe.get(i)).append(" |");
        }
        return sb.toString();
    }
}
