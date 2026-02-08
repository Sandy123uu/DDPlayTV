from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


def load_module() -> object:
    module_path = Path(__file__).resolve().parents[1] / "compare_quality_snapshot.py"
    spec = importlib.util.spec_from_file_location("compare_quality_snapshot_under_test", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {module_path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


MODULE = load_module()


class CompareQualitySnapshotTest(unittest.TestCase):
    def test_check_thresholds_all_pass(self) -> None:
        thresholds = MODULE.Thresholds(
            new_coverage=80.0,
            new_duplicated_lines_density=3.0,
            new_security_hotspots_reviewed=100.0,
            vulnerabilities=0,
        )
        current = {
            "newCoverage": 88.0,
            "newDuplicatedLinesDensity": 2.2,
            "newSecurityHotspotsReviewed": 100.0,
            "vulnerabilitiesTotal": 0,
        }

        checks = MODULE.check_thresholds(current, thresholds)
        self.assertEqual(len(checks), 4)
        self.assertTrue(all(bool(item["pass"]) for item in checks))

    def test_build_comparison_marks_gate_fail_when_threshold_not_met(self) -> None:
        thresholds = MODULE.Thresholds(
            new_coverage=80.0,
            new_duplicated_lines_density=3.0,
            new_security_hotspots_reviewed=100.0,
            vulnerabilities=0,
        )
        baseline = {
            "analysisKey": "analysis-baseline",
            "branch": "master",
            "gateStatus": "FAIL",
            "issuesTotal": 100,
            "highImpactTotal": 50,
            "vulnerabilitiesTotal": 2,
            "topIssueFiles": [
                {"path": "player_component/src/main/java/Foo.kt", "count": 12},
                {"path": "storage_component/src/main/java/Bar.kt", "count": 7},
            ],
        }
        current = {
            "analysisKey": "analysis-current",
            "branch": "001-fix-sonarcloud-issues",
            "gateStatus": "PASS",
            "issuesTotal": 70,
            "highImpactTotal": 20,
            "vulnerabilitiesTotal": 1,
            "newCoverage": 76.5,
            "newDuplicatedLinesDensity": 2.1,
            "newSecurityHotspotsReviewed": 100.0,
            "topIssueFiles": [
                {"path": "player_component/src/main/java/Foo.kt", "count": 4},
                {"path": "storage_component/src/main/java/Bar.kt", "count": 5},
            ],
        }

        comparison = MODULE.build_comparison(baseline, current, thresholds)

        self.assertEqual(comparison["gateStatus"], "FAIL")
        failed_metrics = {
            item["metric"]
            for item in comparison["thresholdChecks"]
            if not bool(item["pass"])
        }
        self.assertIn("new_coverage", failed_metrics)
        self.assertIn("vulnerabilities", failed_metrics)

    def test_build_top10_delta_includes_union_of_file_paths(self) -> None:
        baseline = {
            "topIssueFiles": [
                {"path": "A.kt", "count": 10},
                {"path": "B.kt", "count": 5},
            ]
        }
        current = {
            "topIssueFiles": [
                {"path": "B.kt", "count": 2},
                {"path": "C.kt", "count": 8},
            ]
        }

        delta_rows = MODULE.build_top10_delta(baseline, current, top_n=10)
        paths = {row["path"] for row in delta_rows}
        self.assertSetEqual(paths, {"A.kt", "B.kt", "C.kt"})

        by_path = {row["path"]: row for row in delta_rows}
        self.assertEqual(by_path["A.kt"]["delta"], -10)
        self.assertEqual(by_path["B.kt"]["delta"], -3)
        self.assertEqual(by_path["C.kt"]["delta"], 8)


if __name__ == "__main__":
    unittest.main()
