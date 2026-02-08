from __future__ import annotations

import csv
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


def load_module() -> object:
    module_path = Path(__file__).resolve().parents[1] / "validate_remediation_ledger.py"
    spec = importlib.util.spec_from_file_location("validate_remediation_ledger_under_test", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {module_path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


MODULE = load_module()


class ValidateRemediationLedgerTest(unittest.TestCase):
    def write_csv(self, path: Path, rows: list[dict[str, str]]) -> None:
        fieldnames = [
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
        ]
        with path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(rows)

    def write_exemptions(self, path: Path, issue_key: str) -> None:
        path.write_text(
            "\n".join(
                [
                    "| exemption_id | issue_key | module_path | file_path | reason_type | rationale | reviewer | review_conclusion | evidence_links | created_at |",
                    "|--------------|-----------|-------------|-----------|-------------|-----------|----------|-------------------|----------------|------------|",
                    f"| EX-001 | {issue_key} | repository/danmaku | repository/danmaku/README.md | THIRD_PARTY_SOURCE | 上游闭源 AAR | quality-reviewer | APPROVED | https://sonarcloud.io | 2026-02-08 |",
                    "",
                ]
            ),
            encoding="utf-8",
        )

    def test_validate_ledger_passes_for_valid_rows(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            tasks_csv = temp_path / "remediation_tasks.csv"
            exemptions_md = temp_path / "exemptions.md"

            self.write_csv(
                tasks_csv,
                [
                    {
                        "task_id": "US1-T018",
                        "issue_key": "AZw4PUuMpBg_nGQ6xCmp",
                        "rule_key": "kotlin:S6418",
                        "priority": "P1",
                        "target_outcome": "FIX",
                        "task_status": "CLOSED",
                        "source_scope": "FIRST_PARTY",
                        "module_path": "bilibili_component",
                        "file_path": "bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt",
                        "line": "13",
                        "assignee": "dev-a",
                        "reviewer": "reviewer-a",
                        "decision_reason": "",
                        "evidence_links": "https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUuMpBg_nGQ6xCmp|https://github.com/okami-horo/DDPlayTV/pull/1",
                        "baseline_analysis_key": "07c4be1f-f37a-418a-926f-2a13a7a15f86",
                        "current_analysis_key": "",
                        "created_at": "2026-02-08T10:00:00Z",
                        "updated_at": "2026-02-08T11:00:00Z",
                        "closed_at": "2026-02-08T11:00:00Z",
                        "notes": "",
                    },
                    {
                        "task_id": "US3-T046-A",
                        "issue_key": "AZw4PUrvpBg_nGQ6xCml",
                        "rule_key": "kotlin:S3776",
                        "priority": "P3",
                        "target_outcome": "DEFER",
                        "task_status": "TRIAGED",
                        "source_scope": "FIRST_PARTY",
                        "module_path": "bilibili_component",
                        "file_path": "bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt",
                        "line": "650",
                        "assignee": "dev-b",
                        "reviewer": "reviewer-b",
                        "decision_reason": "拆分风险过高，延期到下一轮复杂度治理",
                        "evidence_links": "https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCml",
                        "baseline_analysis_key": "07c4be1f-f37a-418a-926f-2a13a7a15f86",
                        "current_analysis_key": "",
                        "created_at": "2026-02-08T12:00:00Z",
                        "updated_at": "2026-02-08T12:30:00Z",
                        "closed_at": "",
                        "notes": "",
                    },
                    {
                        "task_id": "US3-T047-A",
                        "issue_key": "TP-ISSUE-001",
                        "rule_key": "java:S1135",
                        "priority": "P3",
                        "target_outcome": "EXEMPT",
                        "task_status": "CLOSED",
                        "source_scope": "THIRD_PARTY",
                        "module_path": "repository/danmaku",
                        "file_path": "repository/danmaku/DanmakuFlameMaster.aar",
                        "line": "",
                        "assignee": "dev-c",
                        "reviewer": "reviewer-c",
                        "decision_reason": "第三方封装目录，仅做豁免记录",
                        "evidence_links": "https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=TP-ISSUE-001|https://github.com/okami-horo/DDPlayTV/pull/3",
                        "baseline_analysis_key": "07c4be1f-f37a-418a-926f-2a13a7a15f86",
                        "current_analysis_key": "",
                        "created_at": "2026-02-08T12:40:00Z",
                        "updated_at": "2026-02-08T12:55:00Z",
                        "closed_at": "2026-02-08T12:55:00Z",
                        "notes": "",
                    },
                    {
                        "task_id": "US3-T047-B",
                        "issue_key": "HOTSPOT-MEDIUM-001",
                        "rule_key": "kotlin:S4787",
                        "priority": "P3",
                        "target_outcome": "ACCEPT_RISK",
                        "task_status": "CLOSED",
                        "source_scope": "FIRST_PARTY",
                        "module_path": "core_network_component",
                        "file_path": "core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt",
                        "line": "88",
                        "assignee": "dev-d",
                        "reviewer": "security-reviewer",
                        "decision_reason": "仅影响内网调试链路，已受环境白名单限制",
                        "evidence_links": "https://sonarcloud.io/project/security_hotspots?id=okami-horo_DDPlayTV&hotspots=HOTSPOT-MEDIUM-001|https://github.com/okami-horo/DDPlayTV/pull/2",
                        "baseline_analysis_key": "07c4be1f-f37a-418a-926f-2a13a7a15f86",
                        "current_analysis_key": "",
                        "created_at": "2026-02-08T13:00:00Z",
                        "updated_at": "2026-02-08T13:15:00Z",
                        "closed_at": "2026-02-08T13:15:00Z",
                        "notes": "accepted_by=security-owner;review_conclusion=APPROVED",
                    },
                ],
            )
            self.write_exemptions(exemptions_md, issue_key="TP-ISSUE-001")

            result = MODULE.validate_ledger(tasks_csv, exemptions_md)

            self.assertEqual(result["summary"]["totalRows"], 4)
            self.assertEqual(result["summary"]["errorCount"], 0)
            self.assertEqual(result["summary"]["warningCount"], 0)

    def test_validate_ledger_reports_rule_violations(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            tasks_csv = temp_path / "remediation_tasks.csv"
            exemptions_md = temp_path / "exemptions.md"

            self.write_csv(
                tasks_csv,
                [
                    {
                        "task_id": "BAD-001",
                        "issue_key": "",
                        "rule_key": "kotlin:S3776",
                        "priority": "P9",
                        "target_outcome": "DEFER",
                        "task_status": "CLOSED",
                        "source_scope": "THIRD_PARTY",
                        "module_path": "repository/immersion_bar",
                        "file_path": "repository/immersion_bar/immersionbar.aar",
                        "line": "",
                        "assignee": "",
                        "reviewer": "",
                        "decision_reason": "",
                        "evidence_links": "https://github.com/okami-horo/DDPlayTV/pull/3",
                        "baseline_analysis_key": "",
                        "current_analysis_key": "",
                        "created_at": "",
                        "updated_at": "",
                        "closed_at": "",
                        "notes": "accepted_by=",
                    }
                ],
            )
            self.write_exemptions(exemptions_md, issue_key="TP-ISSUE-002")

            result = MODULE.validate_ledger(tasks_csv, exemptions_md)
            errors = "\n".join(result["errors"])

            self.assertGreater(result["summary"]["errorCount"], 0)
            self.assertIn("issue_key is required", errors)
            self.assertIn("priority must be one of", errors)
            self.assertIn("decision_reason is required", errors)
            self.assertIn("THIRD_PARTY rows must use target_outcome=EXEMPT", errors)
            self.assertIn("baseline_analysis_key is required", errors)


if __name__ == "__main__":
    unittest.main()
