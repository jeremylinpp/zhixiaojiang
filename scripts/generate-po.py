#!/usr/bin/env python3
"""从建表 SQL 生成 PO 类（com.zhixiaojiang.model.po）。

覆盖 src/main/resources/schema.sql 与 student-portal-schema.sql 两张建表脚本
（与 application.yml 的 spring.sql.init.schema-locations 保持一致）。

表结构变更后重新执行：python3 scripts/generate-po.py
说明：PO 只承载表字段（getter/setter），不放业务逻辑；查询投影类按需手写在 model/po 或直接在 Mapper 里用 Map 返回。
"""
import re
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCHEMAS = [
    ROOT / "zhixiaojiang-server/src/main/resources/schema.sql",
    ROOT / "zhixiaojiang-server/src/main/resources/student-portal-schema.sql",
]
OUT_DIR = ROOT / "zhixiaojiang-server/src/main/java/com/zhixiaojiang/model/po"

TYPE_MAP = {
    "BIGINT": "Long",
    "INT": "Integer",
    "INTEGER": "Integer",
    "SMALLINT": "Integer",
    "TINYINT": "Integer",
    "DECIMAL": "java.math.BigDecimal",
    "NUMERIC": "java.math.BigDecimal",
    "FLOAT": "Double",
    "DOUBLE": "Double",
    "VARCHAR": "String",
    "CHAR": "String",
    "TEXT": "String",
    "JSON": "String",
    "BOOLEAN": "Boolean",
    "DATE": "java.time.LocalDate",
    "DATETIME": "java.time.LocalDateTime",
    "TIMESTAMP": "java.time.LocalDateTime",
}
SKIP_PREFIX = ("PRIMARY KEY", "UNIQUE KEY", "UNIQUE(", "UNIQUE (", "KEY ", "INDEX ", "CONSTRAINT", "FOREIGN KEY")


def class_name(table: str) -> str:
    return "".join(part.capitalize() for part in table.split("_"))


def field_name(column: str) -> str:
    head, *tail = column.split("_")
    return head + "".join(part.capitalize() for part in tail)


def java_type(sql_type: str) -> str:
    base = re.split(r"[(\s]", sql_type.strip())[0].upper()
    return TYPE_MAP.get(base, "String")


def split_columns(body: str) -> list[str]:
    """按顶层逗号切分列定义（括号内的逗号不算）。"""
    parts, depth, current = [], 0, ""
    for char in body:
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current.strip())
            current = ""
        else:
            current += char
    if current.strip():
        parts.append(current.strip())
    return parts


def parse_schema(text: str):
    pattern = re.compile(r"CREATE TABLE IF NOT EXISTS (\w+)\s*\((.*?)\);", re.S)
    for table, body in pattern.findall(text):
        columns = []
        for line in split_columns(body):
            if line.upper().startswith(SKIP_PREFIX):
                continue
            match = re.match(r"(\w+)\s+([A-Za-z]+(?:\s*\([^)]*\))?)", line)
            if not match:
                continue
            column, sql_type = match.group(1), match.group(2)
            columns.append((column, java_type(sql_type)))
        yield table, columns


def render(table: str, columns: list[tuple[str, str]]) -> str:
    cls = class_name(table)
    fields = []
    for column, jtype in columns:
        fields.append((field_name(column), jtype, column))
    lines = [
        "package com.zhixiaojiang.model.po;",
        "",
        f"/** 表 {table} 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */",
        f"public class {cls} {{",
    ]
    for name, jtype, column in fields:
        lines.append(f"    /** 列 {column} */")
        lines.append(f"    private {jtype} {name};")
        lines.append("")
    for name, jtype, _ in fields:
        cap = name[0].upper() + name[1:]
        lines.append(f"    public {jtype} get{cap}() {{")
        lines.append(f"        return {name};")
        lines.append("    }")
        lines.append("")
        lines.append(f"    public void set{cap}({jtype} {name}) {{")
        lines.append(f"        this.{name} = {name};")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    generated = []
    tables = {}
    for schema in SCHEMAS:
        tables.update(parse_schema(schema.read_text(encoding="utf-8")))
    for table, columns in tables.items():
        if not columns:
            continue
        (OUT_DIR / f"{class_name(table)}.java").write_text(render(table, columns), encoding="utf-8")
        generated.append(f"{class_name(table)}({len(columns)} 字段)")
    print(f"已生成 {len(generated)} 个 PO 类 → {OUT_DIR}")
    for row in generated:
        print("  " + row)


if __name__ == "__main__":
    main()
