package com.wc.prediction.wcprediction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/superadmin/db")
public class DbConfigController {

    private static final List<Map<String, Object>> TABLE_META = List.of(
        Map.of("name", "wc_matches",  "label", "Matches",  "pk", "id"),
        Map.of("name", "wc_teams",    "label", "Teams",    "pk", "id"),
        Map.of("name", "wc_players",  "label", "Players",  "pk", "id")
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/tables")
    public ResponseEntity<List<Map<String, Object>>> getTables() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> meta : TABLE_META) {
            String table = (String) meta.get("name");
            List<String> cols = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ? ORDER BY ordinal_position",
                String.class, table
            );
            Map<String, Object> entry = new LinkedHashMap<>(meta);
            entry.put("columns", cols);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/table/{name}")
    public ResponseEntity<?> getRows(@PathVariable String name,
                                     @RequestParam(defaultValue = "") String q,
                                     @RequestParam(defaultValue = "0") int offset,
                                     @RequestParam(defaultValue = "50") int limit) {
        if (!isAllowed(name)) return ResponseEntity.badRequest().body(Map.of("error", "Table not allowed"));
        String where = q.isBlank() ? "" : buildSearch(name, q);
        String sql = "SELECT * FROM " + name + where + " ORDER BY id LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, limit, offset);
        int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + name + where, Integer.class);
        return ResponseEntity.ok(Map.of("rows", rows, "total", total));
    }

    @PatchMapping("/table/{name}/{id}")
    public ResponseEntity<?> updateRow(@PathVariable String name,
                                       @PathVariable String id,
                                       @RequestBody Map<String, Object> fields) {
        if (!isAllowed(name)) return ResponseEntity.badRequest().body(Map.of("error", "Table not allowed"));
        if (fields.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No fields to update"));
        String pk = pkFor(name);
        StringBuilder sql = new StringBuilder("UPDATE " + name + " SET ");
        List<Object> params = new ArrayList<>();
        fields.forEach((col, val) -> {
            if (!col.equals(pk)) {
                if (!params.isEmpty()) sql.append(", ");
                sql.append(col).append(" = ?");
                params.add(val);
            }
        });
        sql.append(" WHERE ").append(pk).append(" = ?");
        params.add(id);
        int updated = jdbcTemplate.update(sql.toString(), params.toArray());
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @DeleteMapping("/table/{name}/{id}")
    public ResponseEntity<?> deleteRow(@PathVariable String name, @PathVariable String id) {
        if (!isAllowed(name)) return ResponseEntity.badRequest().body(Map.of("error", "Table not allowed"));
        String pk = pkFor(name);
        int deleted = jdbcTemplate.update("DELETE FROM " + name + " WHERE " + pk + " = ?", id);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    private boolean isAllowed(String name) {
        return TABLE_META.stream().anyMatch(m -> m.get("name").equals(name));
    }

    private String pkFor(String name) {
        return TABLE_META.stream()
            .filter(m -> m.get("name").equals(name))
            .map(m -> (String) m.get("pk"))
            .findFirst().orElse("id");
    }

    private String buildSearch(String table, String q) {
        String escaped = q.replace("'", "''");
        Map<String, String[]> searchCols = Map.of(
            "wc_matches", new String[]{"team_a", "team_b", "venue", "stage", "match_no"},
            "wc_teams",   new String[]{"team_name", "short_name"},
            "wc_players", new String[]{"player_name", "team", "position"}
        );
        String[] cols = searchCols.getOrDefault(table, new String[]{});
        if (cols.length == 0) return "";
        StringJoiner sj = new StringJoiner(" OR ");
        for (String col : cols) sj.add("CAST(" + col + " AS TEXT) ILIKE '%" + escaped + "%'");
        return " WHERE " + sj;
    }
}
