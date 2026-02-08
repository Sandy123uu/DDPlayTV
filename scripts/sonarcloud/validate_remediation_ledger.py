#!/usr/bin/env python3
"""Validate remediation ledger rows against US3 data-model constraints."""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


REQUIRED_COLUMNS = {
    "task_id",
    "issue_key",
    "rule_key",
    "priority",
    "target_outcome",
    "task_status",
    "source_scope",
    "module_path",
    "file_path",
    "line",
    "assignee",
    "reviewer",
    "decision_reason",
    "evidence_links",
    "baseline_analysis_key",
    "current_analysis_key",
    "created_at",
    "updated_at",
    "closed_at",
    "notes",
}

ALLOWED_PRIORITIES = {"P1", "P2", "P3"}
ALLOWED_OUTCOMES = {"FIX", "ACCEPT_RISK", "DEFER", "FALSE_POSITIVE", "EXEMPT"}
ALLOWED_TASK_STATUSES = {
    "NEW",
    "TRIAGED",
    "IN_PROGRESS",
    "READY_FOR_REVIEW",
    "VERIFIED",
    "CLOSED",
    "REJECTED",
}
ALLOWED_SOURCE_SCOPE = {"FIRST_PARTY", "THIRD_PARTY"}
ALLOWED_REVIEW_CONCLUSION = {"APPROVED", "REJECTED"}

REQUIRES_REASON = {"DEFER", "FALSE_POSITIVE", "EXEMPT", "ACCEPT_RISK"}
SONAR_LINK_PATTERN = re.compile(r"sonarcloud\.io", re.IGNORECASE)
CODE_LINK_PATTERN = re.compile(r"(github\.com/.+/(pull|commit))|(^commit:)", re.IGNORECASE)


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def default_tasks_csv() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/tracking/remediation_tasks.csv"


def default_exemptions_md() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/tracking/exemptions.md"


def default_output_path() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/us3/remediation-ledger-validation.json"


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate remediation task ledger CSV")
    parser.add_argument("--tasks-csv", type=Path, default=default_tasks_csv(), help="Path to remediation_tasks.csv")
    parser.add_argument(
        "--exemptions-md",
        type=Path,
        default=default_exemptions_md(),
        help="Path to exemptions markdown ledger",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=default_output_path(),
        help="Validation report output JSON path",
    )
    parser.add_argument(
        "--allow-empty",
        action="store_true",
        help="Allow empty CSV (header only) without failing.",
    )
    return parser.parse_args(argv)


def normalize_enum(raw: Any) -> str:
    return str(raw or "").strip().upper()


def split_links(raw: str) -> list[str]:
    tokens: list[str] = []
    for part in re.split(r"[|,]", raw):
        value = part.strip()
        if value:
            tokens.append(value)
    return tokens


def parse_notes(raw: str) -> dict[str, str]:
    notes: dict[str, str] = {}
    for chunk in raw.split(";"):
        token = chunk.strip()
        if not token or "=" not in token:
            continue
        key, value = token.split("=", 1)
        notes[key.strip().lower()] = value.strip()
    return notes


def parse_markdown_table(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []

    lines = [line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip().startswith("|")]
    if len(lines) < 2:
        return []

    headers: list[str] | None = None
    rows: list[dict[str, str]] = []

    for line in lines:
        cols = [cell.strip() for cell in line.strip("|").split("|")]
        if not cols:
            continue

        is_separator = all(set(cell) <= {"-", ":"} and cell for cell in cols)
        if headers is None:
            headers = [col.lower() for col in cols]
            continue
        if is_separator:
            continue
        if len(cols) != len(headers):
            continue

        rows.append({headers[idx]: cols[idx] for idx in range(len(headers))})

    return rows


def load_exemption_issue_keys(exemptions_md: Path) -> set[str]:
    keys: set[str] = set()
    for row in parse_markdown_table(exemptions_md):
        issue_key = row.get("issue_key", "").strip()
        review_conclusion = normalize_enum(row.get("review_conclusion", ""))
        if not issue_key or issue_key == "(pending)":
            continue
        if review_conclusion and review_conclusion not in ALLOWED_REVIEW_CONCLUSION:
            continue
        keys.add(issue_key)
    return keys


def load_tasks(tasks_csv: Path) -> tuple[list[dict[str, str]], set[str]]:
    if not tasks_csv.exists():
        raise FileNotFoundError(f"tasks csv not found: {tasks_csv}")

    with tasks_csv.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames is None:
            raise ValueError("tasks csv has no header")

        header = {name.strip() for name in reader.fieldnames if name is not None}
        rows = [dict(row) for row in reader]

    return rows, header


def validate_ledger(tasks_csv: Path, exemptions_md: Path | None = None, allow_empty: bool = False) -> dict[str, Any]:
    rows, header = load_tasks(tasks_csv)

    missing_columns = sorted(REQUIRED_COLUMNS - header)
    errors: list[str] = []
    warnings: list[str] = []

    if missing_columns:
        errors.append(f"missing required columns: {', '.join(missing_columns)}")

    if not rows and not allow_empty:
        errors.append("ledger has no rows (at least one remediation task row is required)")

    exemption_issue_keys = load_exemption_issue_keys(exemptions_md) if exemptions_md is not None else set()

    by_outcome: Counter[str] = Counter()
    by_status: Counter[str] = Counter()

    for index, row in enumerate(rows, start=2):
        row_ref = f"row {index}"

        issue_key = str(row.get("issue_key") or "").strip()
        priority = normalize_enum(row.get("priority"))
        outcome = normalize_enum(row.get("target_outcome"))
        task_status = normalize_enum(row.get("task_status"))
        source_scope = normalize_enum(row.get("source_scope"))
        decision_reason = str(row.get("decision_reason") or "").strip()
        evidence_links = split_links(str(row.get("evidence_links") or ""))
        baseline_analysis_key = str(row.get("baseline_analysis_key") or "").strip()
        assignee = str(row.get("assignee") or "").strip()
        reviewer = str(row.get("reviewer") or "").strip()
        closed_at = str(row.get("closed_at") or "").strip()
        notes = parse_notes(str(row.get("notes") or ""))

        if issue_key == "":
            errors.append(f"{row_ref}: issue_key is required")
        if priority not in ALLOWED_PRIORITIES:
            errors.append(f"{row_ref}: priority must be one of {sorted(ALLOWED_PRIORITIES)}")
        if outcome not in ALLOWED_OUTCOMES:
            errors.append(f"{row_ref}: target_outcome must be one of {sorted(ALLOWED_OUTCOMES)}")
        if task_status not in ALLOWED_TASK_STATUSES:
            errors.append(f"{row_ref}: task_status must be one of {sorted(ALLOWED_TASK_STATUSES)}")
        if source_scope not in ALLOWED_SOURCE_SCOPE:
            errors.append(f"{row_ref}: source_scope must be one of {sorted(ALLOWED_SOURCE_SCOPE)}")

        if assignee == "":
            errors.append(f"{row_ref}: assignee is required")
        if reviewer == "":
            errors.append(f"{row_ref}: reviewer is required")
        if baseline_analysis_key == "":
            errors.append(f"{row_ref}: baseline_analysis_key is required")

        if outcome in REQUIRES_REASON and decision_reason == "":
            errors.append(f"{row_ref}: decision_reason is required when target_outcome={outcome}")

        if source_scope == "THIRD_PARTY" and outcome != "EXEMPT":
            errors.append(f"{row_ref}: THIRD_PARTY rows must use target_outcome=EXEMPT")

        if source_scope == "THIRD_PARTY" and issue_key and issue_key not in exemption_issue_keys:
            errors.append(f"{row_ref}: THIRD_PARTY issue_key must exist in exemptions ledger ({issue_key})")

        if task_status == "CLOSED":
            if not evidence_links:
                errors.append(f"{row_ref}: CLOSED rows require evidence_links")
            else:
                has_sonar = any(SONAR_LINK_PATTERN.search(link) for link in evidence_links)
                has_code = any(CODE_LINK_PATTERN.search(link) for link in evidence_links)
                if not has_sonar:
                    errors.append(f"{row_ref}: CLOSED rows must include a Sonar issue/hotspot link")
                if not has_code:
                    errors.append(f"{row_ref}: CLOSED rows must include PR/commit evidence link")

        if task_status == "CLOSED" and closed_at == "":
            warnings.append(f"{row_ref}: task_status=CLOSED but closed_at is empty")
        if task_status != "CLOSED" and closed_at != "":
            warnings.append(f"{row_ref}: task_status={task_status} but closed_at is populated")

        if outcome == "ACCEPT_RISK":
            accepted_by = notes.get("accepted_by", "").strip()
            review_conclusion = normalize_enum(notes.get("review_conclusion", ""))
            if accepted_by == "":
                errors.append(f"{row_ref}: ACCEPT_RISK rows require notes accepted_by=<owner>")
            if review_conclusion not in ALLOWED_REVIEW_CONCLUSION:
                errors.append(
                    f"{row_ref}: ACCEPT_RISK rows require notes review_conclusion in {sorted(ALLOWED_REVIEW_CONCLUSION)}"
                )

        by_outcome[outcome or "(empty)"] += 1
        by_status[task_status or "(empty)"] += 1

    return {
        "meta": {
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "tasksCsv": str(tasks_csv.resolve()),
            "exemptionsMd": str(exemptions_md.resolve()) if exemptions_md is not None else None,
        },
        "summary": {
            "totalRows": len(rows),
            "errorCount": len(errors),
            "warningCount": len(warnings),
        },
        "stats": {
            "byOutcome": dict(sorted(by_outcome.items())),
            "byStatus": dict(sorted(by_status.items())),
            "exemptionIssueCount": len(exemption_issue_keys),
        },
        "errors": errors,
        "warnings": warnings,
    }


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    try:
        result = validate_ledger(args.tasks_csv.resolve(), args.exemptions_md.resolve(), allow_empty=args.allow_empty)
    except Exception as exc:
        sys.stderr.write(f"[validate_remediation_ledger] failed: {exc}\n")
        return 1

    output_path = args.output.resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    sys.stdout.write(f"[validate_remediation_ledger] output: {output_path}\n")
    sys.stdout.write(
        "[validate_remediation_ledger] rows={rows} errors={errors} warnings={warnings}\n".format(
            rows=result["summary"]["totalRows"],
            errors=result["summary"]["errorCount"],
            warnings=result["summary"]["warningCount"],
        )
    )

    return 1 if result["summary"]["errorCount"] > 0 else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
