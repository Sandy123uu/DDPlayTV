#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ModuleGroup:
    group_id: str
    name: str
    order: int
    modules: tuple[str, ...]


MODULE_STATUS_BEGIN = "<!-- MODULE_STATUS:BEGIN -->"
MODULE_STATUS_END = "<!-- MODULE_STATUS:END -->"


def repo_root_from_script(script_file: Path) -> Path:
    # scripts/code_quality_audit/<script>.py -> repo root is 3 levels up
    return script_file.resolve().parents[2]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def parse_settings_modules(settings_gradle: Path) -> list[str]:
    text = read_text(settings_gradle)
    modules: list[str] = []
    for match in re.finditer(r"""include\(\s*(['"])([^'"]+)\1\s*\)""", text):
        modules.append(match.group(2))
    seen: set[str] = set()
    ordered: list[str] = []
    for module in modules:
        if module not in seen:
            ordered.append(module)
            seen.add(module)
    return ordered


def parse_module_groups(config_path: Path) -> list[ModuleGroup]:
    lines = read_text(config_path).splitlines()

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
            if re.match(r"^\s*\w+:", line):
                in_modules_list = False

    flush()
    if not groups:
        raise ValueError("Invalid module_groups.yaml: no groups parsed")
    return groups


def parse_module_id_prefixes(config_path: Path) -> dict[str, str]:
    lines = read_text(config_path).splitlines()
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
    return module_path[1:].replace(":", "/") + "/"  # directory hint only


def read_module_status_block_lines(path: Path) -> list[str]:
    if not path.exists():
        return []
    text = read_text(path)
    if MODULE_STATUS_BEGIN not in text or MODULE_STATUS_END not in text:
        return []
    _, rest = text.split(MODULE_STATUS_BEGIN, 1)
    block, _ = rest.split(MODULE_STATUS_END, 1)
    return block.splitlines()


def extract_existing_owner_status(block_lines: list[str]) -> dict[str, tuple[str, str]]:
    rows = [l for l in block_lines if l.strip().startswith("|")]
    if len(rows) < 2:
        return {}

    header = [c.strip() for c in rows[0].strip().strip("|").split("|")]
    try:
        module_idx = header.index("模块")
        owner_idx = header.index("Owner")
        status_idx = header.index("Status")
    except ValueError:
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

