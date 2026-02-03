#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class ModuleGroup:
    group_id: str
    name: str
    order: int
    modules: tuple[str, ...]


MODULE_STATUS_BEGIN = "<!-- MODULE_STATUS:BEGIN -->"
MODULE_STATUS_END = "<!-- MODULE_STATUS:END -->"


def _repo_root_from_script(script_file: Path) -> Path:
    # scripts/code_quality_audit/init_audit_docs.py -> repo root is 3 levels up
    # (init_audit_docs.py -> code_quality_audit -> scripts -> repo)
    return script_file.resolve().parents[2]


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def parse_settings_modules(settings_gradle: Path) -> list[str]:
    text = _read_text(settings_gradle)
    modules: list[str] = []
    for match in re.finditer(r"""include\(\s*(['"])([^'"]+)\1\s*\)""", text):
        modules.append(match.group(2))
    # Keep order, but de-dup just in case.
    seen: set[str] = set()
    ordered: list[str] = []
    for module in modules:
        if module not in seen:
            ordered.append(module)
            seen.add(module)
    return ordered


def parse_module_groups(config_path: Path) -> list[ModuleGroup]:
    lines = _read_text(config_path).splitlines()

    groups: list[ModuleGroup] = []
    current_id: str | None = None
    current_name: str | None = None
    current_order: int | None = None
    current_modules: list[str] = []
    in_modules_list = False

    def flush() -> None:
        nonlocal current_id, current_name, current_order, current_modules, in_modules_list
        if current_id is None:
            return
        if current_name is None or current_order is None:
            raise ValueError(f"Invalid module_groups.yaml: group '{current_id}' missing name/order")
        groups.append(
            ModuleGroup(
                group_id=current_id,
                name=current_name,
                order=current_order,
                modules=tuple(current_modules),
            )
        )
        current_id = None
        current_name = None
        current_order = None
        current_modules = []
        in_modules_list = False

    for raw in lines:
        line = raw.rstrip()
        if not line or line.lstrip().startswith("#"):
            continue

        m = re.match(r"^\s*-\s+id:\s*(.+?)\s*$", line)
        if m:
            flush()
            current_id = m.group(1).strip().strip('"').strip("'")
            continue

        if current_id is None:
            continue

        m = re.match(r"^\s*name:\s*(.+?)\s*$", line)
        if m:
            current_name = m.group(1).strip().strip('"').strip("'")
            continue

        m = re.match(r"^\s*order:\s*(\d+)\s*$", line)
        if m:
            current_order = int(m.group(1))
            continue

        if re.match(r"^\s*modules:\s*$", line):
            in_modules_list = True
            continue

        if in_modules_list:
            m = re.match(r"^\s*-\s*(['\"])?(:[^'\"]+)\1?\s*$", line)
            if m:
                current_modules.append(m.group(2))
                continue
            # Leaving module list on next field
            if re.match(r"^\s*\w+:", line):
                in_modules_list = False

    flush()
    if not groups:
        raise ValueError("Invalid module_groups.yaml: no groups parsed")
    return groups


def parse_module_id_prefixes(config_path: Path) -> dict[str, str]:
    lines = _read_text(config_path).splitlines()
    prefixes: dict[str, str] = {}

    in_prefixes = False
    for raw in lines:
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line == "prefixes:":
            in_prefixes = True
            continue
        if not in_prefixes:
            continue

        m = re.match(r"""^(['"])?(:[^'"]+)\1?:\s*(['"])?([A-Z0-9_]+)\3?\s*$""", line)
        if not m:
            continue
        module_path = m.group(2)
        prefix = m.group(4)
        prefixes[module_path] = prefix

    if not prefixes:
        raise ValueError("Invalid module_id_prefixes.yaml: no prefixes parsed")
    return prefixes


def module_to_filename(module_path: str) -> str:
    if not module_path.startswith(":"):
        raise ValueError(f"Invalid module path: {module_path}")
    return module_path[1:].replace(":", "__") + ".md"


def module_to_dir(module_path: str) -> str:
    if not module_path.startswith(":"):
        raise ValueError(f"Invalid module path: {module_path}")
    return module_path[1:].replace(":", "/") + "/"


def _extract_existing_owner_status(block_lines: list[str]) -> dict[str, tuple[str, str]]:
    """
    Parse an existing markdown table in the MODULE_STATUS block.
    Return module -> (owner, status)
    """
    rows = [l for l in block_lines if l.strip().startswith("|")]
    if len(rows) < 2:
        return {}

    header = [c.strip() for c in rows[0].strip().strip("|").split("|")]
    try:
        module_idx = header.index("模块")
        owner_idx = header.index("Owner")
        status_idx = header.index("Status")
    except ValueError:
        # Unknown table layout; do not attempt to preserve.
        return {}

    preserved: dict[str, tuple[str, str]] = {}
    for row in rows[2:]:
        cols = [c.strip() for c in row.strip().strip("|").split("|")]
        if len(cols) <= max(module_idx, owner_idx, status_idx):
            continue
        module = cols[module_idx]
        owner = cols[owner_idx]
        status = cols[status_idx]
        if module.startswith(":"):
            preserved[module] = (owner, status)
    return preserved


def _render_module_status_table(
    modules: Iterable[str],
    module_to_group: dict[str, str],
    group_order: dict[str, int],
    module_to_prefix: dict[str, str],
    existing_owner_status: dict[str, tuple[str, str]],
) -> str:
    rows: list[tuple[int, str, str, str, str, str, str]] = []
    for module in modules:
        group = module_to_group.get(module, "UNASSIGNED")
        order = group_order.get(group, 999)
        prefix = module_to_prefix.get(module, "UNKNOWN")
        owner, status = existing_owner_status.get(module, ("", "Todo"))
        report_path = f"document/code_quality_audit/modules/{module_to_filename(module)}"
        rows.append((order, module, group, prefix, owner, status, report_path))

    rows.sort(key=lambda r: (r[0], r[1]))

    lines: list[str] = []
    lines.append("| 模块 | 分组 | ID 前缀 | Owner | Status | 报告 |")
    lines.append("|---|---|---|---|---|---|")
    for _, module, group, prefix, owner, status, report_path in rows:
        lines.append(f"| {module} | {group} | {prefix} | {owner} | {status} | {report_path} |")
    return "\n".join(lines) + "\n"


def _update_module_status_file(
    path: Path,
    table_markdown: str,
) -> None:
    if not path.exists():
        content = "\n".join(
            [
                "# 模块覆盖率 / 状态跟踪表",
                "",
                "本表用于跟踪“纳入构建范围”的模块排查覆盖率与推进状态，便于分工与复核。",
                "",
                MODULE_STATUS_BEGIN,
                "",
                table_markdown.rstrip(),
                "",
                MODULE_STATUS_END,
                "",
            ]
        )
        _write_text(path, content + "\n")
        return

    text = _read_text(path)
    if MODULE_STATUS_BEGIN not in text or MODULE_STATUS_END not in text:
        raise ValueError(
            f"module_status.md missing markers: {MODULE_STATUS_BEGIN} / {MODULE_STATUS_END}"
        )

    before, rest = text.split(MODULE_STATUS_BEGIN, 1)
    _, after = rest.split(MODULE_STATUS_END, 1)
    new_block = "\n" + table_markdown.rstrip() + "\n"
    new_text = before + MODULE_STATUS_BEGIN + new_block + MODULE_STATUS_END + after
    _write_text(path, new_text)


def _read_module_status_block_lines(path: Path) -> list[str]:
    if not path.exists():
        return []
    text = _read_text(path)
    if MODULE_STATUS_BEGIN not in text or MODULE_STATUS_END not in text:
        return []
    _, rest = text.split(MODULE_STATUS_BEGIN, 1)
    block, _ = rest.split(MODULE_STATUS_END, 1)
    return block.splitlines()


def _ensure_module_reports(
    template_path: Path,
    modules_dir: Path,
    modules: Iterable[str],
    module_to_prefix: dict[str, str],
) -> tuple[int, int]:
    """
    Returns: (created_count, skipped_existing_count)
    """
    template = _read_text(template_path)
    created = 0
    skipped = 0
    for module in modules:
        out_path = modules_dir / module_to_filename(module)
        if out_path.exists():
            skipped += 1
            continue

        prefix = module_to_prefix.get(module, "UNKNOWN")
        content = (
            template.replace("<modulePath>", module)
            .replace("<owner>", "")
            .replace("<moduleDir 或关键包名>", module_to_dir(module))
            .replace("<PREFIX>", prefix)
        )
        _write_text(out_path, content.rstrip() + "\n")
        created += 1
    return created, skipped


def _validate_modules_covered(
    modules: list[str],
    module_to_group: dict[str, str],
    module_to_prefix: dict[str, str],
) -> None:
    missing_group = [m for m in modules if m not in module_to_group]
    missing_prefix = [m for m in modules if m not in module_to_prefix]

    if missing_group or missing_prefix:
        parts: list[str] = []
        if missing_group:
            parts.append("Missing group for: " + ", ".join(missing_group))
        if missing_prefix:
            parts.append("Missing prefix for: " + ", ".join(missing_prefix))
        raise ValueError("; ".join(parts))


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Init code quality audit docs.")
    parser.add_argument(
        "--repo-root",
        type=str,
        default="",
        help="Repository root path (auto-detected if omitted).",
    )
    args = parser.parse_args(argv)

    repo_root = Path(args.repo_root).resolve() if args.repo_root else _repo_root_from_script(Path(__file__))
    settings_gradle = repo_root / "settings.gradle.kts"

    config_groups = repo_root / "document" / "code_quality_audit" / "config" / "module_groups.yaml"
    config_prefixes = repo_root / "document" / "code_quality_audit" / "config" / "module_id_prefixes.yaml"
    template_module_report = (
        repo_root / "document" / "code_quality_audit" / "templates" / "module_report.md"
    )

    modules_dir = repo_root / "document" / "code_quality_audit" / "modules"
    global_dir = repo_root / "document" / "code_quality_audit" / "global"
    module_status_path = global_dir / "module_status.md"

    modules = parse_settings_modules(settings_gradle)
    if not modules:
        print(f"ERROR: no modules found in {settings_gradle}", file=sys.stderr)
        return 1

    groups = parse_module_groups(config_groups)
    module_to_group: dict[str, str] = {}
    group_order: dict[str, int] = {}
    for group in groups:
        group_order[group.name] = group.order
        for module in group.modules:
            if module in module_to_group:
                raise ValueError(f"Duplicate module in groups config: {module}")
            module_to_group[module] = group.name

    module_to_prefix = parse_module_id_prefixes(config_prefixes)
    _validate_modules_covered(modules, module_to_group, module_to_prefix)

    existing_block_lines = _read_module_status_block_lines(module_status_path)
    existing_owner_status = _extract_existing_owner_status(existing_block_lines)

    created_reports, skipped_reports = _ensure_module_reports(
        template_path=template_module_report,
        modules_dir=modules_dir,
        modules=modules,
        module_to_prefix=module_to_prefix,
    )

    table = _render_module_status_table(
        modules=modules,
        module_to_group=module_to_group,
        group_order=group_order,
        module_to_prefix=module_to_prefix,
        existing_owner_status=existing_owner_status,
    )
    _update_module_status_file(module_status_path, table)

    print("Init audit docs done.")
    print(f"- Repo root: {repo_root}")
    print(f"- Modules (from settings.gradle.kts): {len(modules)}")
    print(f"- Module reports: created {created_reports}, existing {skipped_reports}")
    print(f"- Module status updated: {module_status_path.relative_to(repo_root)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

