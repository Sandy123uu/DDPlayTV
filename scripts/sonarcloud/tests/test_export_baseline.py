from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path


def load_module() -> object:
    module_path = Path(__file__).resolve().parents[1] / "export_baseline.py"
    spec = importlib.util.spec_from_file_location("export_baseline_under_test", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {module_path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


MODULE = load_module()


class ExportBaselineTest(unittest.TestCase):
    def test_build_snapshot_contains_required_sections(self) -> None:
        report = {
            "generatedAt": "2026-02-08T10:00:00Z",
            "project": {
                "key": "okami-horo_DDPlayTV",
                "organization": "okami-horo",
                "branch": "feature/us3",
                "latestAnalysis": {"key": "analysis-current"},
            },
            "summary": {
                "issuesTotal": 3,
                "issuesFetched": 3,
                "hotspotsTotal": 2,
                "hotspotsFetched": 2,
                "issuesByFacet": {
                    "types": {"VULNERABILITY": 1, "CODE_SMELL": 2},
                    "impactSeverities": {"HIGH": 2},
                },
            },
            "issues": [
                {
                    "key": "ISSUE-HIGH-1",
                    "ruleKey": "kotlin:S3776",
                    "type": "CODE_SMELL",
                    "severity": "CRITICAL",
                    "impactSeverity": "HIGH",
                    "status": "OPEN",
                    "path": "player_component/src/main/java/Foo.kt",
                    "line": 10,
                },
                {
                    "key": "ISSUE-VULN-1",
                    "ruleKey": "kotlin:S6290",
                    "type": "VULNERABILITY",
                    "severity": "CRITICAL",
                    "impactSeverity": "HIGH",
                    "status": "OPEN",
                    "path": "core_network_component/src/main/java/Bar.kt",
                    "line": 42,
                },
                {
                    "key": "ISSUE-LOW-1",
                    "ruleKey": "kotlin:S1481",
                    "type": "CODE_SMELL",
                    "severity": "MINOR",
                    "impactSeverity": "LOW",
                    "status": "OPEN",
                    "path": "user_component/src/main/java/Baz.kt",
                    "line": 7,
                },
            ],
            "hotspots": [
                {
                    "key": "HOTSPOT-HIGH-1",
                    "ruleKey": "kotlin:S6418",
                    "vulnerabilityProbability": "HIGH",
                    "status": "TO_REVIEW",
                    "path": "bilibili_component/src/main/java/Sec.kt",
                    "line": 8,
                },
                {
                    "key": "HOTSPOT-MEDIUM-1",
                    "ruleKey": "kotlin:S4787",
                    "vulnerabilityProbability": "MEDIUM",
                    "status": "TO_REVIEW",
                    "path": "core_storage_component/src/main/java/Sec2.kt",
                    "line": 30,
                },
            ],
            "qualityGate": {
                "status": "ERROR",
                "conditions": [
                    {
                        "metricKey": "new_coverage",
                        "status": "ERROR",
                        "actualValue": "72.5",
                        "errorThreshold": "80",
                    }
                ],
            },
            "measures": {
                "new_coverage": {"value": "72.5"},
                "new_duplicated_lines_density": {"value": "2.8"},
                "new_security_hotspots_reviewed": {"value": "100"},
                "vulnerabilities": {"value": "1"},
            },
        }

        args = Namespace(
            report=Path("fake-report.json"),
            analysis_key="analysis-baseline",
            top_files_limit=2,
        )

        snapshot = MODULE.build_snapshot(report, args)

        self.assertEqual(snapshot["analysis"]["baselineAnalysisKey"], "analysis-baseline")
        self.assertEqual(snapshot["summary"]["issuesTotal"], 3)
        self.assertEqual(snapshot["summary"]["hotspotsTotal"], 2)
        self.assertEqual(len(snapshot["topIssueFiles"]), 2)
        self.assertEqual(snapshot["highRisk"]["vulnerabilityIssues"][0]["key"], "ISSUE-VULN-1")
        self.assertEqual(snapshot["highRisk"]["highRiskHotspots"][0]["key"], "HOTSPOT-HIGH-1")

    def test_extract_measures_can_fallback_to_quality_gate_conditions(self) -> None:
        report = {
            "qualityGate": {
                "conditions": [
                    {"metricKey": "new_coverage", "actualValue": "88.8"},
                    {
                        "metricKey": "new_duplicated_lines_density",
                        "actualValue": "1.2",
                    },
                ]
            },
            "measures": {},
        }

        measures = MODULE.extract_measures(report)
        self.assertEqual(measures["new_coverage"], 88.8)
        self.assertEqual(measures["new_duplicated_lines_density"], 1.2)

    def test_main_writes_json_and_markdown_outputs(self) -> None:
        report = {
            "generatedAt": "2026-02-08T10:00:00Z",
            "project": {
                "key": "okami-horo_DDPlayTV",
                "organization": "okami-horo",
                "branch": "feature/us3",
                "latestAnalysis": {"key": "analysis-current"},
            },
            "summary": {
                "issuesTotal": 1,
                "issuesFetched": 1,
                "hotspotsTotal": 0,
                "hotspotsFetched": 0,
                "issuesByFacet": {
                    "types": {"VULNERABILITY": 0, "CODE_SMELL": 1},
                    "impactSeverities": {"HIGH": 1},
                },
            },
            "issues": [
                {
                    "key": "ISSUE-HIGH-1",
                    "ruleKey": "kotlin:S3776",
                    "type": "CODE_SMELL",
                    "severity": "CRITICAL",
                    "impactSeverity": "HIGH",
                    "status": "OPEN",
                    "path": "player_component/src/main/java/Foo.kt",
                    "line": 10,
                }
            ],
            "hotspots": [],
            "qualityGate": {"status": "ERROR", "conditions": []},
            "measures": {
                "new_coverage": {"value": "72.5"},
                "new_duplicated_lines_density": {"value": "2.8"},
                "new_security_hotspots_reviewed": {"value": "0"},
                "vulnerabilities": {"value": "0"},
            },
        }

        with tempfile.TemporaryDirectory() as temp_dir:
            report_path = Path(temp_dir) / "report.json"
            output_dir = Path(temp_dir) / "output"
            report_path.write_text(json.dumps(report), encoding="utf-8")

            exit_code = MODULE.main(
                [
                    "--report",
                    str(report_path),
                    "--output-dir",
                    str(output_dir),
                    "--analysis-key",
                    "analysis-baseline",
                ]
            )
            self.assertEqual(exit_code, 0)

            json_path = output_dir / "quality-baseline.json"
            md_path = output_dir / "quality-baseline.md"
            self.assertTrue(json_path.exists())
            self.assertTrue(md_path.exists())

            output_payload = json.loads(json_path.read_text(encoding="utf-8"))
            self.assertEqual(output_payload["analysis"]["baselineAnalysisKey"], "analysis-baseline")


if __name__ == "__main__":
    unittest.main()
