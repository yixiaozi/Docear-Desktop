# -*- coding: utf-8 -*-
"""
Export Docear workspace side-tab data for AI consumption.
Mirrors logic from MindMapWorkspaceContextScanner, AbstractAllItemsTabPanel,
NodeDetailsTagScanner, EnhancedAllRecentlyModified, FavoritesAndTagsStore.
"""
from __future__ import annotations

import html
import os
import re
import sys
import xml.sax
from collections import OrderedDict
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

DATA_ROOT = Path(r"E:\yixiaozi")
OUTPUT_DIR = DATA_ROOT / "00统领全局" / ".AI请查看这里"
FAVORITES_SETTINGS = DATA_ROOT / "_data" / "17DAB3A24CC7NGK3HWY5ERX3AURZZAJ2PT99" / "favorites.settings"

TODO_ICON = "hourglass"
PUBLISH_ICON = "internet"
PIN_TAG = "钉选"
TAG_ARCHIVED = "已归档"
RECENTLY_MODIFIED_LIMIT = 1000

HASHTAG_PATTERN = re.compile(r"#([^#\s<]+)")
NUMERIC_ONLY = re.compile(r"^\d+$")
SKIP_DIRS = {"_data", "bin", ".git"}
CONFLICT_COPY = "冲突副本"


def strip_html(text: str) -> str:
    if not text:
        return ""
    text = html.unescape(text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def sanitize_xml_text(text: str) -> str:
    """Remove invalid numeric character references that break xml.sax."""

    def repl(match: re.Match) -> str:
        raw = match.group(1)
        try:
            num = int(raw[1:], 16) if raw.startswith("x") else int(raw)
            if 0 <= num <= 0x10FFFF:
                return match.group(0)
        except ValueError:
            pass
        return ""

    return re.sub(r"&#(x?[0-9a-fA-F]+);", repl, text)


def is_valid_mm(file: Path) -> bool:
    name = file.name
    if name.startswith("~") or CONFLICT_COPY in name:
        return False
    return name.lower().endswith(".mm")


def collect_mm_files(root: Path) -> List[Path]:
    files: List[Path] = []
    if not root.is_dir():
        return files
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [
            d for d in dirnames
            if not d.startswith(".") and d.lower() != "bin"
        ]
        for name in filenames:
            p = Path(dirpath) / name
            if is_valid_mm(p):
                files.append(p)
    return files


def rel_path(file: Path) -> str:
    try:
        return str(file.relative_to(DATA_ROOT)).replace("\\", "/")
    except ValueError:
        return str(file).replace("\\", "/")


def fmt_ts(ms: int) -> str:
    if ms <= 0:
        return ""
    return datetime.fromtimestamp(ms / 1000.0).strftime("%Y-%m-%d %H:%M:%S")


class MindMapHandler(xml.sax.ContentHandler):
    def __init__(self, file: Path):
        self.file = file
        self.node_stack: List[dict] = []
        self.reminders: List[dict] = []
        self.todos: List[dict] = []
        self.published: List[dict] = []
        self.modified_nodes: List[dict] = []
        self.pinned_candidates: List[dict] = []

    def startElement(self, name, attrs):
        if name == "node":
            self.node_stack.append({
                "id": attrs.get("ID", ""),
                "text": attrs.get("TEXT", "") or "",
                "remind_type": attrs.get("REMINDERTYPE"),
                "details_parts": [],
                "in_details": False,
                "icons": set(),
            })
            modified_str = attrs.get("MODIFIED")
            if modified_str and self.node_stack:
                try:
                    modified_at = int(modified_str)
                    node = self.node_stack[-1]
                    text = strip_html(node["text"])
                    if text and text.lower() != "bin" and not NUMERIC_ONLY.match(text):
                        self.modified_nodes.append({
                            **self._item(node, text),
                            "modified_at": modified_at,
                        })
                except ValueError:
                    pass
        elif name == "icon" and self.node_stack:
            icon = attrs.get("BUILTIN", "")
            self.node_stack[-1]["icons"].add(icon.lower())
        elif name == "Parameters" and self.node_stack:
            remind_at = attrs.get("REMINDUSERAT")
            if remind_at:
                try:
                    ts = int(remind_at)
                    if ts > 0:
                        node = self.node_stack[-1]
                        text = strip_html(node["text"])
                        remind_type = node.get("remind_type")
                        recurring = remind_type is not None and remind_type.lower() != "onetime"
                        if text and text.lower() != "bin":
                            self.reminders.append({
                                **self._item(node, text),
                                "remind_at": ts,
                                "recurring": recurring,
                                "remind_type": remind_type or "onetime",
                            })
                except ValueError:
                    pass
        elif name == "richcontent" and self.node_stack:
            if attrs.get("TYPE") == "DETAILS":
                self.node_stack[-1]["in_details"] = True
                self.node_stack[-1]["details_parts"] = []

    def characters(self, content):
        if self.node_stack and self.node_stack[-1]["in_details"]:
            self.node_stack[-1]["details_parts"].append(content)

    def endElement(self, name):
        if name == "richcontent" and self.node_stack:
            self.node_stack[-1]["in_details"] = False
        elif name == "node":
            if not self.node_stack:
                return
            node = self.node_stack.pop()
            text = strip_html(node["text"])
            icons = node["icons"]
            if text and text.lower() != "bin":
                if TODO_ICON.lower() in icons:
                    self.todos.append(self._item(node, text))
                if PUBLISH_ICON.lower() in icons:
                    self.published.append(self._item(node, text))
            details_html = "".join(node["details_parts"])
            tags = parse_detail_tags(details_html)
            if tags:
                self.pinned_candidates.append({
                    **self._item(node, strip_html(node["text"])),
                    "tags": sorted(tags - {PIN_TAG}),
                    "pinned": PIN_TAG in tags,
                    "details_plain": strip_html(details_html),
                })

    def _item(self, node: dict, text: str) -> dict:
        return {
            "map_file": str(self.file),
            "map_rel": rel_path(self.file),
            "map_name": self.file.name,
            "node_id": node["id"],
            "node_text": text,
        }


def is_tag_only_line(line: str) -> bool:
    line = line.strip()
    if "#" not in line:
        return False
    tokens = line.split()
    for token in tokens:
        if not token.startswith("#") or len(token) <= 1:
            return False
    return True


def parse_detail_tags(details_html: str) -> Set[str]:
    if not details_html or not details_html.strip():
        return set()
    decoded = html.unescape(details_html)
    text = decoded.replace("</p>", "\n").replace("<p>", "")
    text = re.sub(r"<[^>]+>", " ", text)
    text = html.unescape(text)
    tags: Set[str] = set()
    for line in text.split("\n"):
        line = line.strip()
        if not line or not is_tag_only_line(line):
            continue
        for m in HASHTAG_PATTERN.finditer(line):
            tag = html.unescape(m.group(1).strip())
            if tag and not re.match(r"^\d+;?$", tag) and "&" not in tag:
                tags.add(tag)
    return tags


def scan_file(file: Path) -> MindMapHandler:
    handler = MindMapHandler(file)
    try:
        raw = file.read_text(encoding="utf-8", errors="replace")
        xml.sax.parseString(sanitize_xml_text(raw), handler)
    except Exception as e:
        print(f"WARN scan {file}: {e}", file=sys.stderr)
    return handler


def dedupe(items: List[dict], key_fn) -> List[dict]:
    seen = set()
    out = []
    for item in items:
        key = key_fn(item)
        if key in seen:
            continue
        seen.add(key)
        out.append(item)
    return out


def load_favorites() -> Tuple[List[dict], Dict[str, str]]:
    entries: List[dict] = []
    tags_by_uri: Dict[str, str] = {}
    if not FAVORITES_SETTINGS.is_file():
        return entries, tags_by_uri
    props: Dict[str, str] = {}
    with open(FAVORITES_SETTINGS, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                k, v = line.split("=", 1)
                props[k.strip()] = v.encode("utf-8").decode("unicode_escape")
    favs = [x for x in props.get("favorites", "").split("\n") if x.strip()]
    for line in props.get("tags", "").split("\n"):
        if "\t" in line:
            uri, tag = line.split("\t", 1)
            tags_by_uri[uri.strip()] = tag.strip()
    for uri in favs:
        full = DATA_ROOT / uri.replace("/", os.sep)
        entries.append({
            "uri": uri,
            "tags": tags_by_uri.get(uri, ""),
            "exists": full.is_file(),
            "abs_path": str(full) if full.is_file() else "",
            "name": Path(uri).name,
        })
    return entries, tags_by_uri


def format_location(item: dict) -> str:
    return f"{item.get('map_rel', '')}#node={item.get('node_id', '')}"


def write_section_header(f, title: str, count: int, desc: str):
    f.write(f"# {title}\n\n")
    f.write(f"- 导出时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    f.write(f"- 数据根目录: {DATA_ROOT}\n")
    f.write(f"- 条目数: {count}\n")
    f.write(f"- 说明: {desc}\n\n")
    f.write("---\n\n")


def export_reminders(path: Path, items: List[dict], title: str, desc: str):
    items = sorted(items, key=lambda x: x["remind_at"])
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        write_section_header(f, title, len(items), desc)
        for i, item in enumerate(items, 1):
            f.write(f"## {i}. {item['node_text']}\n\n")
            f.write(f"- 提醒时间: {fmt_ts(item['remind_at'])}\n")
            f.write(f"- 提醒类型: {item.get('remind_type', '')}\n")
            f.write(f"- 导图: {item['map_rel']}\n")
            f.write(f"- 节点ID: {item['node_id']}\n")
            f.write(f"- 定位: {format_location(item)}\n\n")


def export_simple_items(path: Path, items: List[dict], title: str, desc: str, extra_fields=None):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        write_section_header(f, title, len(items), desc)
        for i, item in enumerate(items, 1):
            f.write(f"## {i}. {item['node_text']}\n\n")
            f.write(f"- 导图: {item['map_rel']}\n")
            f.write(f"- 节点ID: {item['node_id']}\n")
            f.write(f"- 定位: {format_location(item)}\n")
            if extra_fields:
                for label, key in extra_fields:
                    if key in item and item[key]:
                        f.write(f"- {label}: {item[key]}\n")
            f.write("\n")


def export_pinned(path: Path, items: List[dict]):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        write_section_header(
            f, "我的钉选", len(items),
            "节点详情中含 #钉选 标签且未标记 #已归档 的节点。"
        )
        for i, item in enumerate(items, 1):
            f.write(f"## {i}. {item['node_text']}\n\n")
            f.write(f"- 导图: {item['map_rel']}\n")
            f.write(f"- 节点ID: {item['node_id']}\n")
            f.write(f"- 定位: {format_location(item)}\n")
            if item.get("tags"):
                f.write(f"- 其他标签: {', '.join(item['tags'])}\n")
            if item.get("details_plain"):
                f.write(f"- 节点详情摘要: {item['details_plain'][:500]}\n")
            f.write("\n")


def export_recently_modified(path: Path, items: List[dict]):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        write_section_header(
            f, "最近修改（前1000条）", len(items),
            "按节点 MODIFIED 时间戳降序排列，取前1000条（不限天数）。"
        )
        current_group = None
        for i, item in enumerate(items, 1):
            group = time_group_label(item["modified_at"])
            if group != current_group:
                current_group = group
                f.write(f"# {group}\n\n")
            f.write(f"## {i}. {fmt_ts(item['modified_at'])} {item['node_text']}\n\n")
            f.write(f"- 导图: {item['map_rel']}\n")
            f.write(f"- 节点ID: {item['node_id']}\n")
            f.write(f"- 定位: {format_location(item)}\n\n")


def time_group_label(ts: int) -> str:
    now = datetime.now()
    dt = datetime.fromtimestamp(ts / 1000.0)
    today = now.date()
    d = dt.date()
    from datetime import timedelta
    if d == today:
        return "今天"
    if d == today - timedelta(days=1):
        return "昨天"
    if d == today - timedelta(days=2):
        return "前天"
    return dt.strftime("%Y-%m-%d")


def export_favorites(path: Path, items: List[dict]):
    tag_counts: Dict[str, int] = OrderedDict()
    tag_counts["全部"] = len(items)
    for item in items:
        tag = item.get("tags") or "未分类"
        tag_counts[tag] = tag_counts.get(tag, 0) + 1

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        write_section_header(
            f, "收藏", len(items),
            "来自 favorites.settings，含收藏路径与分类标签。"
        )
        f.write("## 分类统计\n\n")
        for tag, count in tag_counts.items():
            f.write(f"- {tag}: {count}\n")
        f.write("\n---\n\n")
        for i, item in enumerate(items, 1):
            f.write(f"## {i}. {item['name']}\n\n")
            f.write(f"- 相对路径: {item['uri']}\n")
            f.write(f"- 标签: {item.get('tags') or '（无）'}\n")
            f.write(f"- 文件存在: {'是' if item['exists'] else '否'}\n")
            if item.get("abs_path"):
                f.write(f"- 绝对路径: {item['abs_path']}\n")
            f.write("\n")


def is_workspace_file(file: Path) -> bool:
    if not file.is_file():
        return False
    name = file.name
    if name.startswith("~") or CONFLICT_COPY in name:
        return False
    return True


def collect_all_workspace_files(root: Path) -> List[Path]:
    files: List[Path] = []
    seen = set()

    def walk(directory: Path):
        if not directory.is_dir():
            return
        try:
            children = sorted(directory.iterdir(), key=lambda p: p.name.lower())
        except OSError:
            return
        for child in children:
            if child.is_dir():
                if child.name.lower() in SKIP_DIRS:
                    continue
                walk(child)
            elif is_workspace_file(child):
                key = str(child.resolve())
                if key not in seen:
                    seen.add(key)
                    files.append(child)

    walk(root)
    return files


class _TreeNode:
    __slots__ = ("subdirs", "files")

    def __init__(self):
        self.subdirs: Dict[str, "_TreeNode"] = {}
        self.files: List[str] = []


def build_workspace_tree(root: Path) -> str:
    files = collect_all_workspace_files(root)
    tree = _TreeNode()
    for file in files:
        try:
            rel = str(file.relative_to(root)).replace("\\", "/")
        except ValueError:
            rel = file.name
        parts = [p for p in rel.split("/") if p]
        if not parts:
            continue
        node = tree
        for i, part in enumerate(parts):
            if i == len(parts) - 1:
                if part not in node.files:
                    node.files.append(part)
            else:
                node = node.subdirs.setdefault(part, _TreeNode())

    lines = [f"{root.name}/"]

    def render(node: _TreeNode, prefix: str):
        names = sorted(node.subdirs.keys(), key=str.lower) + sorted(node.files, key=str.lower)
        for i, name in enumerate(names):
            last = i == len(names) - 1
            branch = "└── " if last else "├── "
            next_prefix = prefix + ("    " if last else "│   ")
            if name in node.subdirs:
                lines.append(f"{prefix}{branch}{name}/")
                render(node.subdirs[name], next_prefix)
            else:
                lines.append(f"{prefix}{branch}{name}")

    render(tree, "")
    return "\n".join(lines) + "\n"


def write_workspace_tree(path: Path):
    files = collect_all_workspace_files(DATA_ROOT)
    tree_text = build_workspace_tree(DATA_ROOT)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# 工作区完整文件目录树\n")
        f.write(f"# 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"# 数据根目录: {DATA_ROOT}\n")
        f.write(f"# 文件总数: {len(files)}\n")
        f.write("# 说明: 相对数据根的树形列表，已排除 _data、bin、.git 目录，含隐藏目录（如 .AI请查看这里）\n\n")
        f.write(tree_text)


def main():
    output = OUTPUT_DIR
    output.mkdir(parents=True, exist_ok=True)

    print(f"Scanning mind maps under {DATA_ROOT} ...")
    mm_files = collect_mm_files(DATA_ROOT)
    print(f"Found {len(mm_files)} .mm files")

    one_time: List[dict] = []
    recurring: List[dict] = []
    todos: List[dict] = []
    published: List[dict] = []
    modified: List[dict] = []
    pinned_all: List[dict] = []

    for idx, mm in enumerate(mm_files, 1):
        if idx % 200 == 0:
            print(f"  scanned {idx}/{len(mm_files)}")
        h = scan_file(mm)
        for r in h.reminders:
            if r["recurring"]:
                recurring.append(r)
            else:
                one_time.append(r)
        todos.extend(h.todos)
        published.extend(h.published)
        modified.extend(h.modified_nodes)
        for p in h.pinned_candidates:
            if p["pinned"] and TAG_ARCHIVED not in p.get("tags", []):
                pinned_all.append(p)

    one_time = dedupe(one_time, lambda x: f"{x['map_file']}|{x['node_id']}|{x['remind_at']}")
    recurring = dedupe(recurring, lambda x: f"{x['map_file']}|{x['node_id']}|{x['remind_at']}")
    todos = dedupe(todos, lambda x: f"{x['map_file']}|{x['node_id']}")
    published = dedupe(published, lambda x: f"{x['map_file']}|{x['node_id']}")
    modified = dedupe(modified, lambda x: f"{x['map_file']}|{x['node_id']}|{x['modified_at']}")
    modified.sort(key=lambda x: x["modified_at"], reverse=True)
    modified = modified[:RECENTLY_MODIFIED_LIMIT]
    pinned_all = dedupe(pinned_all, lambda x: f"{x['map_file']}|{x['node_id']}")

    # group published/todos like UI (by path then name)
    todos.sort(key=lambda x: (x["map_rel"], x["node_text"]))
    published.sort(key=lambda x: (x["map_rel"], x["node_text"]))
    pinned_all.sort(key=lambda x: (x["map_rel"], x["node_text"]))

    favorites, _ = load_favorites()

    files_map = {
        "01-全部提醒.md": lambda p: export_reminders(
            p, one_time, "全部提醒", "一次性提醒（REMINDERTYPE=onetime 或未设置）。"),
        "02-周期提醒.md": lambda p: export_reminders(
            p, recurring, "周期提醒", "周期性提醒（REMINDERTYPE 非 onetime）。"),
        "03-全部待办.md": lambda p: export_simple_items(
            p, todos, "全部待办", "节点带 hourglass（沙漏）图标的待办项。"),
        "04-我的钉选.md": lambda p: export_pinned(p, pinned_all),
        "05-全部发布.md": lambda p: export_simple_items(
            p, published, "全部发布", "节点带 internet（发布）图标的项。"),
        "06-最近修改.md": lambda p: export_recently_modified(p, modified),
        "07-收藏.md": lambda p: export_favorites(p, favorites),
    }

    written: List[Path] = []
    for name, writer in files_map.items():
        path = output / name
        writer(path)
        written.append(path)
        print(f"Wrote {path}")

    tree_path = output / "00-文件目录树.txt"
    write_workspace_tree(tree_path)
    print(f"Wrote {tree_path}")
    print("Done.", {
        "one_time": len(one_time),
        "recurring": len(recurring),
        "todos": len(todos),
        "pinned": len(pinned_all),
        "published": len(published),
        "recent": len(modified),
        "favorites": len(favorites),
        "workspace_files": len(collect_all_workspace_files(DATA_ROOT)),
    })


if __name__ == "__main__":
    main()
