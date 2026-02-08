#!/usr/bin/env python3
"""Export DEFER/ACCEPT_RISK/FALSE_POSITIVE/EXEMPT issue dispositions from remediation ledger."""

from __future__ import annotations

import argparse
import csv
import json
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


DISPOSITION_OUTCOMES = ("DEFER", "ACCEPT_RISK", "FALSE_POSITIVE", "EXEMPT")
PRIORITY_ORDER = {"P1": 0, "P2": 1, "P3": 2}


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def default_tasks_csv() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/tracking/remediation_tasks.csv"


def default_report_json() -> Path:
    return repo_root() / ".sonarcloud-report/sonarcloud-report.json"


def default_output_json() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/us3/issue-dispositions.json"


def default_output_md() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/us3/high-risk-dispositions.md"


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export issue disposition ledger for audit")
    parser.add_argument("--tasks-csv", type=Path, default=default_tasks_csv(), help="Path to remediation task CSV")
    parser.add_argument("--report", type=Path, default=default_report_json(), help="Path to Sonar report JSON")
    parser.add_argument("--output-json", type=Path, default=default_output_json(), help="Destination JSON file")
    parser.add_argument("--output-md", type=Path, default=default_output_md(), help="Destination markdown file")
    parser.add_argument(
        "--analysis-key",
        type=str,
        default="",
        help="Optional analysis key label for metadata display",
    )
    return parser.parse_args(argv)


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list_of_dict(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        return []
    return [item for item in value if isinstance(item, dict)]


def normalize(raw: Any) -> str:
    return str(raw or "").strip()


def split_links(raw: str) -> list[str]:
    return [chunk.strip() for chunk in raw.replace(",", "|").split("|") if chunk.strip()]


def parse_notes(raw: str) -> dict[str, str]:
    notes: dict[str, str] = {}
    for part in raw.split(";"):
        token = part.strip()
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        notes[key.strip().lower()] = value.strip()
    return notes


def load_csv_rows(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        raise FileNotFoundError(f"tasks csv not found: {path}")
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        return [dict(row) for row in reader]


def load_report_index(path: Path) -> dict[str, dict[str, Any]]:
    if not path.exists():
        return {}

    payload = json.loads(path.read_text(encoding="utf-8"))
    issues = as_list_of_dict(as_dict(payload).get("issues"))
    hotspots = as_list_of_dict(as_dict(payload).get("hotspots"))

    index: dict[str, dict[str, Any]] = {}
    for issue in issues:
        key = normalize(issue.get("key"))
        if not key:
            continue
        index[key] = {
            "kind": "ISSUE",
            "ruleKey": normalize(issue.get("ruleKey")),
            "issueType": normalize(issue.get("type")),
            "severity": normalize(issue.get("severity")),
            "impactSeverity": normalize(issue.get("impactSeverity")),
            "status": normalize(issue.get("status")),
            "path": normalize(issue.get("path") or issue.get("component")),
            "line": issue.get("line"),
        }

    for hotspot in hotspots:
        key = normalize(hotspot.get("key"))
        if not key:
            continue
        index[key] = {
            "kind": "HOTSPOT",
            "ruleKey": normalize(hotspot.get("ruleKey")),
            "issueType": "SECURITY_HOTSPOT",
            "severity": normalize(hotspot.get("vulnerabilityProbability")),
            "impactSeverity": normalize(hotspot.get("vulnerabilityProbability")),
            "status": normalize(hotspot.get("status")),
            "path": normalize(hotspot.get("path") or hotspot.get("component")),
            "line": hotspot.get("line"),
        }

    return index


def is_high_risk(row: dict[str, Any]) -> bool:
    outcome = normalize(row.get("targetOutcome")).upper()
    impact = normalize(row.get("impactSeverity")).upper()
    issue_type = normalize(row.get("issueType")).upper()
    priority = normalize(row.get("priority")).upper()

    if issue_type in {"VULNERABILITY", "SECURITY_HOTSPOT"}:
        return True
    if impact == "HIGH":
        return True
    if priority == "P1":
        return True
    return outcome == "ACCEPT_RISK"


def build_disposition_rows(tasks: list[dict[str, str]], report_index: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []

    for task in tasks:
        target_outcome = normalize(task.get("target_outcome")).upper()
        if target_outcome not in DISPOSITION_OUTCOMES:
            continue

        issue_key = normalize(task.get("issue_key"))
        notes = parse_notes(normalize(task.get("notes")))
        metadata = report_index.get(issue_key, {})

        row = {
            "taskId": normalize(task.get("task_id")),
            "issueKey": issue_key,
            "priority": normalize(task.get("priority")).upper(),
            "targetOutcome": target_outcome,
            "taskStatus": normalize(task.get("task_status")).upper(),
            "decisionReason": normalize(task.get("decision_reason")),
            "sourceScope": normalize(task.get("source_scope")).upper(),
            "modulePath": normalize(task.get("module_path")),
            "filePath": normalize(task.get("file_path")),
            "line": normalize(task.get("line")),
            "assignee": normalize(task.get("assignee")),
            "reviewer": normalize(task.get("reviewer")),
            "acceptedBy": notes.get("accepted_by", ""),
            "reviewConclusion": notes.get("review_conclusion", "").upper(),
            "baselineAnalysisKey": normalize(task.get("baseline_analysis_key")),
            "currentAnalysisKey": normalize(task.get("current_analysis_key")),
            "evidenceLinks": split_links(normalize(task.get("evidence_links"))),
            "ruleKey": normalize(task.get("rule_key")) or normalize(metadata.get("ruleKey")),
            "issueType": normalize(metadata.get("issueType")),
            "severity": normalize(metadata.get("severity")),
            "impactSeverity": normalize(metadata.get("impactSeverity")),
            "issueStatus": normalize(metadata.get("status")),
            "detectedPath": normalize(metadata.get("path")),
            "detectedLine": metadata.get("line"),
        }
        row["highRisk"] = is_high_risk(row)

        rows.append(row)

    rows.sort(
        key=lambda item: (
            PRIORITY_ORDER.get(normalize(item.get("priority")).upper(), 9),
            normalize(item.get("targetOutcome")),
            normalize(item.get("issueKey")),
        )
    )
    return rows


def build_output_payload(rows: list[dict[str, Any]], analysis_key: str) -> dict[str, Any]:
    by_outcome: Counter[str] = Counter(normalize(row.get("targetOutcome")) for row in rows)
    high_risk_count = sum(1 for row in rows if bool(row.get("highRisk")))

    return {
        "meta": {
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "analysisKey": analysis_key,
            "dispositionOutcomes": list(DISPOSITION_OUTCOMES),
        },
        "summary": {
            "total": len(rows),
            "highRiskTotal": high_risk_count,
            "byOutcome": dict(sorted(by_outcome.items())),
        },
        "items": rows,
    }


def render_markdown(payload: dict[str, Any]) -> str:
    rows = as_list_of_dict(payload.get("items"))
    summary = as_dict(payload.get("summary"))
    meta = as_dict(payload.get("meta"))

    lines: list[str] = []
    lines.append("# 遗留高风险问题处置登记")
    lines.append("")
    lines.append(f"- 生成时间：{meta.get('generatedAt', '')}")
    lines.append(f"- 分析线标识：`{meta.get('analysisKey', '')}`")
    lines.append(f"- 处置总数：{summary.get('total', 0)}")
    lines.append(f"- 高风险条目：{summary.get('highRiskTotal', 0)}")
    lines.append("")

    lines.append("## 处置分布")
    lines.append("")
    lines.append("| Outcome | Count |")
    lines.append("|---------|-------|")

    by_outcome = as_dict(summary.get("byOutcome"))
    if by_outcome:
        for outcome, count in sorted(by_outcome.items()):
            lines.append(f"| `{outcome}` | {count} |")
    else:
        lines.append("| `(none)` | 0 |")

    lines.append("")
    lines.append("## 条目明细")
    lines.append("")
    lines.append("| # | issueKey | Outcome | Priority | 高风险 | 理由 | 接受人 | 复核结论 | 证据 |")
    lines.append("|---|----------|---------|----------|--------|------|--------|----------|------|")

    if rows:
        for idx, row in enumerate(rows, start=1):
            evidence_links = row.get("evidenceLinks")
            if isinstance(evidence_links, list) and evidence_links:
                evidence = "<br>".join(f"`{link}`" for link in evidence_links)
            else:
                evidence = "-"
            lines.append(
                "| {idx} | `{issue_key}` | `{outcome}` | `{priority}` | {high_risk} | {reason} | {accepted_by} | {review} | {evidence} |".format(
                    idx=idx,
                    issue_key=row.get("issueKey", ""),
                    outcome=row.get("targetOutcome", ""),
                    priority=row.get("priority", ""),
                    high_risk="是" if row.get("highRisk") else "否",
                    reason=(row.get("decisionReason") or "-").replace("|", "\\|"),
                    accepted_by=(row.get("acceptedBy") or "-").replace("|", "\\|"),
                    review=(row.get("reviewConclusion") or "-").replace("|", "\\|"),
                    evidence=evidence,
                )
            )
    else:
        lines.append("| 1 | `(none)` | - | - | - | - | - | - | - |")

    lines.append("")
    lines.append("> 说明：`DEFER/ACCEPT_RISK/FALSE_POSITIVE/EXEMPT` 均由台账驱动导出；其中 `ACCEPT_RISK` 必须包含接受人与复核结论。"
    )
    lines.append("")

    return "\n".join(lines)


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    try:
        tasks = load_csv_rows(args.tasks_csv.resolve())
        report_index = load_report_index(args.report.resolve())
        rows = build_disposition_rows(tasks, report_index)
        payload = build_output_payload(rows, args.analysis_key.strip())
    except Exception as exc:
        sys.stderr.write(f"[export_issue_dispositions] failed: {exc}\n")
        return 1

    output_json = args.output_json.resolve()
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    output_md = args.output_md.resolve()
    output_md.parent.mkdir(parents=True, exist_ok=True)
    output_md.write_text(render_markdown(payload), encoding="utf-8")

    sys.stdout.write(f"[export_issue_dispositions] json: {output_json}\n")
    sys.stdout.write(f"[export_issue_dispositions] markdown: {output_md}\n")
    sys.stdout.write(
        f"[export_issue_dispositions] dispositions={payload['summary']['total']} highRisk={payload['summary']['highRiskTotal']}\n"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
