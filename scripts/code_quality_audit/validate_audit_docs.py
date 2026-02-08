#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

from audit_docs_lib import (
    MODULE_STATUS_BEGIN,
    MODULE_STATUS_END,
    module_to_filename,
    parse_module_groups,
    parse_module_id_prefixes,
    parse_settings_modules,
    read_module_status_block_lines,
    read_text,
    repo_root_from_script,
)


ALLOWED_MODULE_STATUS = {"Todo", "Doing", "Review", "Done", "Observing"}


@dataclass(frozen=True)
class Issue:
    kind: str  # ERROR / WARN
    message: str


def _parse_module_status_table(block_lines: list[str]) -> tuple[list[str], list[dict[str, str]]]:
    rows = [l for l in block_lines if l.strip().startswith("|")]
    if len(rows) < 2:
        return [], []

    header = [c.strip() for c in rows[0].strip().strip("|").split("|")]
    data_rows: list[dict[str, str]] = []
    for row in rows[2:]:
        cols = [c.strip() for c in row.strip().strip("|").split("|")]
        if len(cols) != len(header):
            continue
        data_rows.append(dict(zip(header, cols, strict=True)))
    return header, data_rows


def _validate_module_status(
    repo_root: Path,
    modules: list[str],
    module_to_prefix: dict[str, str],
    module_status_path: Path,
) -> tuple[list[Issue], dict[str, dict[str, str]]]:
    issues: list[Issue] = []
    block_lines = read_module_status_block_lines(module_status_path)
    if not block_lines:
        issues.append(Issue("ERROR", f"module_status.md missing markers: {MODULE_STATUS_BEGIN} / {MODULE_STATUS_END}"))
        return issues, {}

    header, rows = _parse_module_status_table(block_lines)
    required_cols = {"模块", "分组", "ID 前缀", "Owner", "Status", "报告"}
    if not required_cols.issubset(set(header)):
        issues.append(Issue("ERROR", f"module_status.md table header missing columns: {sorted(required_cols - set(header))}"))
        return issues, {}

    by_module: dict[str, dict[str, str]] = {}
    for r in rows:
        module = r.get("模块", "")
        if not module.startswith(":"):
            continue
        if module in by_module:
            issues.append(Issue("ERROR", f"module_status.md duplicated module row: {module}"))
        by_module[module] = r

        owner = r.get("Owner", "").strip()
        status = r.get("Status", "").strip()
        prefix = r.get("ID 前缀", "").strip()
        report = r.get("报告", "").strip()

        if not owner:
            issues.append(Issue("ERROR", f"module_status.md missing Owner for module: {module}"))
        if status not in ALLOWED_MODULE_STATUS:
            issues.append(Issue("ERROR", f"module_status.md invalid Status '{status}' for module: {module}"))

        expected_prefix = module_to_prefix.get(module)
        if expected_prefix is None:
            issues.append(Issue("ERROR", f"module_id_prefixes.yaml missing prefix for module: {module}"))
        elif prefix != expected_prefix:
            issues.append(Issue("ERROR", f"module_status.md prefix mismatch for {module}: '{prefix}' != '{expected_prefix}'"))

        expected_report = f"document/code_quality_audit/modules/{module_to_filename(module)}"
        if report != expected_report:
            issues.append(Issue("ERROR", f"module_status.md report path mismatch for {module}: '{report}' != '{expected_report}'"))
        else:
            abs_report = repo_root / report
            if not abs_report.exists():
                issues.append(Issue("ERROR", f"module report missing on disk: {report}"))

    missing = [m for m in modules if m not in by_module]
    extra = [m for m in by_module.keys() if m not in set(modules)]
    if missing:
        issues.append(Issue("ERROR", f"module_status.md missing modules from settings.gradle.kts: {missing}"))
    if extra:
        issues.append(Issue("WARN", f"module_status.md contains extra modules not in settings.gradle.kts: {extra}"))

    return issues, by_module


def _validate_module_report_required_fields(module_path: str, report_text: str) -> list[Issue]:
    issues: list[Issue] = []

    required_lines = {
        f"- 模块：{module_path}": "Missing '- 模块：...'",
        "- 负责人：": "Missing '- 负责人：...'",
        "- 日期：": "Missing '- 日期：...'",
        "- 范围：": "Missing '- 范围：...'",
    }
    for needle, msg in required_lines.items():
        if needle not in report_text:
            issues.append(Issue("ERROR", f"{module_path}: {msg}"))

    if "YYYY-MM-DD" in report_text:
        issues.append(Issue("ERROR", f"{module_path}: placeholder 'YYYY-MM-DD' not replaced"))
    if "<modulePath>" in report_text or "<PREFIX>" in report_text or "<owner>" in report_text:
        issues.append(Issue("ERROR", f"{module_path}: template placeholders not fully replaced"))

    for section in ["## 1)", "## 2)", "## 3)", "## 4)", "## 5)", "## 6)"]:
        if section not in report_text:
            issues.append(Issue("ERROR", f"{module_path}: missing section heading '{section}'"))

    return issues


def _validate_ids(module_path: str, report_text: str, expected_prefix: str) -> list[Issue]:
    issues: list[Issue] = []

    def parse_table_ids(section_title: str, next_title: str | None, expected_kind: str) -> list[str]:
        start = report_text.find(section_title)
        if start == -1:
            return []
        end = report_text.find(next_title, start) if next_title else -1
        section = report_text[start:] if end == -1 else report_text[start:end]
        lines = [l for l in section.splitlines() if l.strip().startswith("|")]
        if len(lines) < 3:
            return []
        header = [c.strip() for c in lines[0].strip().strip("|").split("|")]
        try:
            id_idx = header.index("ID")
        except ValueError:
            return []
        ids: list[str] = []
        for row in lines[2:]:
            cols = [c.strip() for c in row.strip().strip("|").split("|")]
            if len(cols) <= id_idx:
                continue
            cell = cols[id_idx]
            m = re.match(r"^([A-Z0-9_]+)-([FT])(\d{3})$", cell)
            if not m:
                continue
            prefix, kind, num = m.group(1), m.group(2), m.group(3)
            if kind != expected_kind:
                continue
            ids.append(f"{prefix}-{kind}{num}")
        return ids

    finding_ids = parse_table_ids("## 3) Findings", "## 4)", "F")
    task_ids = parse_table_ids("## 4) Refactor Tasks", "## 5)", "T")

    if not finding_ids and not task_ids:
        issues.append(
            Issue(
                "WARN",
                f"{module_path}: no Finding/Task IDs detected in tables (expected '{expected_prefix}-F###'/'{expected_prefix}-T###')",
            )
        )
        return issues

    def check_ids(ids: list[str], kind: str) -> None:
        wrong = [i for i in ids if not i.startswith(f"{expected_prefix}-{kind}")]
        if wrong:
            issues.append(Issue("ERROR", f"{module_path}: {kind}-IDs in table use wrong prefix (expected {expected_prefix}): {sorted(set(wrong))}"))
        dup = sorted({i for i in ids if ids.count(i) > 1})
        if dup:
            issues.append(Issue("ERROR", f"{module_path}: duplicated {kind}-IDs in table: {dup}"))

    check_ids(finding_ids, "F")
    check_ids(task_ids, "T")

    return issues


def _validate_findings_tasks_crossref(module_path: str, report_text: str, expected_prefix: str) -> list[Issue]:
    issues: list[Issue] = []

    finding_ids = set(re.findall(rf"\b{re.escape(expected_prefix)}-F\d{{3}}\b", report_text))

    tasks_section_start = report_text.find("## 4) Refactor Tasks")
    if tasks_section_start == -1:
        return issues
    tasks_text = report_text[tasks_section_start:]

    task_rows = [l for l in tasks_text.splitlines() if l.strip().startswith("|")]
    if len(task_rows) < 3:
        return issues

    header = [c.strip() for c in task_rows[0].strip().strip("|").split("|")]
    try:
        related_idx = header.index("关联 Finding")
        id_idx = header.index("ID")
    except ValueError:
        return issues

    for row in task_rows[2:]:
        cols = [c.strip() for c in row.strip().strip("|").split("|")]
        if len(cols) <= max(related_idx, id_idx):
            continue
        task_id = cols[id_idx]
        related = cols[related_idx]
        if not task_id.startswith(f"{expected_prefix}-T"):
            continue
        related_ids = re.findall(rf"\b{re.escape(expected_prefix)}-F\d{{3}}\b", related)
        if not related_ids:
            issues.append(Issue("WARN", f"{module_path}: task {task_id} has no related Finding ID"))
            continue
        missing = [rid for rid in related_ids if rid not in finding_ids]
        if missing:
            issues.append(Issue("ERROR", f"{module_path}: task {task_id} references missing Finding IDs: {missing}"))

    return issues


def validate(repo_root: Path) -> tuple[list[Issue], dict[str, int]]:
    issues: list[Issue] = []

    settings_gradle = repo_root / "settings.gradle.kts"
    modules = parse_settings_modules(settings_gradle)
    if not modules:
        return [Issue("ERROR", f"no modules found in {settings_gradle}")], {}

    config_groups = repo_root / "document" / "code_quality_audit" / "config" / "module_groups.yaml"
    config_prefixes = repo_root / "document" / "code_quality_audit" / "config" / "module_id_prefixes.yaml"

    groups = parse_module_groups(config_groups)
    module_to_group: dict[str, str] = {}
    for g in groups:
        for m in g.modules:
            if m in module_to_group:
                issues.append(Issue("ERROR", f"module_groups.yaml duplicated module: {m}"))
            module_to_group[m] = g.name

    module_to_prefix = parse_module_id_prefixes(config_prefixes)

    global_dir = repo_root / "document" / "code_quality_audit" / "global"
    module_status_path = global_dir / "module_status.md"

    status_issues, status_rows = _validate_module_status(
        repo_root=repo_root,
        modules=modules,
        module_to_prefix=module_to_prefix,
        module_status_path=module_status_path,
    )
    issues.extend(status_issues)

    reports_dir = repo_root / "document" / "code_quality_audit" / "modules"
    for module_path in modules:
        expected_prefix = module_to_prefix.get(module_path, "UNKNOWN")
        report_path = reports_dir / module_to_filename(module_path)
        if not report_path.exists():
            issues.append(Issue("ERROR", f"{module_path}: module report file missing: {report_path}"))
            continue
        report_text = read_text(report_path)

        issues.extend(_validate_module_report_required_fields(module_path, report_text))
        if expected_prefix != "UNKNOWN":
            issues.extend(_validate_ids(module_path, report_text, expected_prefix))
            issues.extend(_validate_findings_tasks_crossref(module_path, report_text, expected_prefix))
        else:
            issues.append(Issue("ERROR", f"{module_path}: UNKNOWN prefix (module_id_prefixes.yaml missing?)"))

        if module_path not in module_to_group:
            issues.append(Issue("ERROR", f"{module_path}: not assigned to any group in module_groups.yaml"))

    counts = {
        "modules_total": len(modules),
        "issues_error": sum(1 for i in issues if i.kind == "ERROR"),
        "issues_warn": sum(1 for i in issues if i.kind == "WARN"),
        "status_rows": len(status_rows),
    }
    return issues, counts


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Validate code quality audit docs consistency.")
    parser.add_argument(
        "--repo-root",
        type=str,
        default="",
        help="Repository root path (auto-detected if omitted).",
    )
    args = parser.parse_args(argv)

    repo_root = Path(args.repo_root).resolve() if args.repo_root else repo_root_from_script(Path(__file__))
    issues, counts = validate(repo_root)

    errors = [i for i in issues if i.kind == "ERROR"]
    warns = [i for i in issues if i.kind == "WARN"]

    print("Validate audit docs: " + ("PASS" if not errors else "FAIL"))
    print(f"- Repo root: {repo_root}")
    print(f"- Modules: {counts.get('modules_total', 0)}")
    print(f"- Issues: errors={len(errors)}, warns={len(warns)}")

    if warns:
        print("\nWarnings:")
        for w in warns:
            print(f"- {w.message}")

    if errors:
        print("\nErrors:")
        for e in errors:
            print(f"- {e.message}")

    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
