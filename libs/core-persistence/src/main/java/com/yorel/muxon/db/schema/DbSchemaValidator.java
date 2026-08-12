/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.db.schema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DbSchemaValidator {
  private static final Pattern CREATE_TABLE =
      Pattern.compile("(?im)^\\s*CREATE\\s+TABLE\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
  private static final Pattern CREATE_VIEW =
      Pattern.compile(
          "(?im)^\\s*CREATE\\s+OR\\s+REPLACE\\s+VIEW\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s+AS\\b");

  private DbSchemaValidator() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException(
          "Usage: DbSchemaValidator <db-schema.yaml> <oss-migrations-dir>");
    }
    Path schemaPath = Path.of(args[0]);
    Path migrationsDir = Path.of(args[1]);

    YamlIndex yamlIndex = indexYaml(schemaPath);

    ParseResult parsed = parseSql(migrationsDir);

    assertSuperset("tables", yamlIndex.tables, parsed.tables);
    assertSuperset("views", yamlIndex.views, parsed.views);
  }

  private static YamlIndex indexYaml(Path schemaPath) throws IOException {
    // Minimal YAML indexer for the specific shape we use:
    //
    // tables:
    //   table_name:
    //     columns:
    //       ...
    // views:
    //   view_name:
    //     columns:
    //       ...
    //
    // We only need the top-level table/view names, so we avoid pulling in a YAML parser dependency.
    Set<String> tables = new HashSet<>();
    Set<String> views = new HashSet<>();

    Section section = Section.NONE;
    for (String line : Files.readAllLines(schemaPath, StandardCharsets.UTF_8)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }

      if (!line.startsWith(" ") && trimmed.equals("tables:")) {
        section = Section.TABLES;
        continue;
      }
      if (!line.startsWith(" ") && trimmed.equals("views:")) {
        section = Section.VIEWS;
        continue;
      }

      // Table/view keys are indented by two spaces: "  name:"
      if (line.startsWith("  ") && !line.startsWith("    ") && trimmed.endsWith(":")) {
        String key = trimmed.substring(0, trimmed.length() - 1).trim();
        if (!key.isEmpty()) {
          if (section == Section.TABLES) {
            tables.add(key);
          } else if (section == Section.VIEWS) {
            views.add(key);
          }
        }
      }
    }

    return new YamlIndex(tables, views);
  }

  private static ParseResult parseSql(Path migrationsDir) throws IOException {
    Set<String> tables = new HashSet<>();
    Set<String> views = new HashSet<>();

    if (!Files.exists(migrationsDir)) {
      throw new IllegalArgumentException("Migrations dir does not exist: " + migrationsDir);
    }

    try (var paths = Files.list(migrationsDir)) {
      for (Path p : (Iterable<Path>) paths::iterator) {
        if (!p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql")) {
          continue;
        }
        String sql = Files.readString(p, StandardCharsets.UTF_8);
        Matcher tm = CREATE_TABLE.matcher(sql);
        while (tm.find()) {
          tables.add(tm.group(1));
        }
        Matcher vm = CREATE_VIEW.matcher(sql);
        while (vm.find()) {
          views.add(vm.group(1));
        }
      }
    }
    return new ParseResult(tables, views);
  }

  private static void assertSuperset(String kind, Set<String> yamlKeys, Set<String> sqlKeys) {
    Set<String> missing = new HashSet<>(sqlKeys);
    missing.removeAll(yamlKeys);
    if (!missing.isEmpty()) {
      throw new IllegalStateException("db-schema.yaml is missing " + kind + ": " + missing);
    }
  }

  private record ParseResult(Set<String> tables, Set<String> views) {}

  private enum Section {
    NONE,
    TABLES,
    VIEWS
  }

  private record YamlIndex(Set<String> tables, Set<String> views) {}
}
