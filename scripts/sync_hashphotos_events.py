#!/usr/bin/env python3
"""One-shot sync: Hash Photos CSV -> Hash Photos.mm 活动数据 node."""
import csv
import os
import re
import sys
import time
import xml.etree.ElementTree as ET

MAP_PATH = r"E:\yixiaozi\07有条不紊\02时间管理\Hash Photos.mm"
EXPORT_DIR = r"E:\yixiaozi\07有条不紊\02时间管理\.files\Hash Photos"
ACTIVITIES_TEXT = "活动数据"
DATE_KEY_ATTR = "dateKey"
SYNC_STATE = ".hashphotos-sync.state"


def find_latest_csv(export_dir):
    latest = None
    latest_key = -1
    for name in os.listdir(export_dir):
        if not name.lower().endswith(".csv"):
            continue
        path = os.path.join(export_dir, name)
        key = os.path.getmtime(path)
        m = re.search(r"-(\d{8}_\d{6})\.csv$", name, re.I)
        if m:
            try:
                d, t = m.group(1).split("_")
                key = int(d + t)
            except ValueError:
                pass
        if latest is None or key > latest_key:
            latest = path
            latest_key = key
    return latest


def read_csv(path):
    rows = {}
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.reader(f)
        header = next(reader, None)
        for row in reader:
            if len(row) < 2:
                continue
            date_key = row[0].strip()
            title = row[1].strip()
            if re.fullmatch(r"\d{8}", date_key) and title:
                rows[date_key] = title
    return rows


def read_date_key(node):
    for attr in node.findall("attribute"):
        if attr.get("NAME") == DATE_KEY_ATTR:
            val = (attr.get("VALUE") or "").strip()
            if re.fullmatch(r"\d{8}", val):
                return val
    text = (node.get("TEXT") or "").strip()
    if len(text) >= 8 and re.fullmatch(r"\d{8}", text[:8]):
        return text[:8]
    return None


def new_id(seq):
    return f"ID_{int(time.time() * 1000) + seq}"


def build_event_node(date_key, title, seq):
    now = str(int(time.time() * 1000) + seq)
    node = ET.Element("node")
    node.set("TEXT", title)
    node.set("POSITION", "right")
    node.set("ID", new_id(seq))
    node.set("CREATED", now)
    node.set("MODIFIED", now)
    attr = ET.SubElement(node, "attribute")
    attr.set("NAME", DATE_KEY_ATTR)
    attr.set("VALUE", date_key)
    return node


def find_activities_node(root):
    for node in root.iter("node"):
        text = node.get("TEXT") or ""
        if text == ACTIVITIES_TEXT:
            return node
    return None


def sync():
    csv_path = find_latest_csv(EXPORT_DIR)
    if not csv_path:
        print("No CSV found in", EXPORT_DIR)
        return 1

    desired = read_csv(csv_path)
    print(f"CSV: {os.path.basename(csv_path)} ({len(desired)} events)")

    tree = ET.parse(MAP_PATH)
    root = tree.getroot()
    activities = find_activities_node(root)
    if activities is None:
        print("活动数据 node not found")
        return 1

    existing = {}
    for child in list(activities):
        if child.tag != "node":
            continue
        dk = read_date_key(child)
        if dk:
            existing[dk] = child

    added = updated = deleted = 0
    seq = 0

    for dk in sorted(existing.keys()):
        if dk not in desired:
            activities.remove(existing[dk])
            deleted += 1

    for date_key in sorted(desired.keys()):
        title = desired[date_key]
        if date_key in existing:
            node = existing[date_key]
            if (node.get("TEXT") or "") != title:
                node.set("TEXT", title)
                node.set("MODIFIED", str(int(time.time() * 1000)))
                updated += 1
        else:
            activities.append(build_event_node(date_key, title, seq))
            seq += 1
            added += 1

    ET.indent(tree, space="")
    tree.write(MAP_PATH, encoding="UTF-8", xml_declaration=False)

    state_path = os.path.join(EXPORT_DIR, SYNC_STATE)
    with open(state_path, "w", encoding="utf-8") as f:
        f.write(f"source={os.path.basename(csv_path)}\n")
        f.write(f"mtime={os.path.getmtime(csv_path)}\n")

    print(f"Done: +{added} ~{updated} -{deleted}")
    print(f"Updated: {MAP_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(sync())
