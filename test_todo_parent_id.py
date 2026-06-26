#!/usr/bin/env python3
"""Simulate todoParentId-based tree attachment for a .mm file."""
import xml.sax
import sys

ICON = "hourglass"

class Handler(xml.sax.ContentHandler):
    def __init__(self):
        self.stack = []
        self.items = []

    def startElement(self, name, attrs):
        if name == "node":
            parent_id = self.stack[-1]["id"] if self.stack else None
            self.stack.append({"id": attrs.get("ID"), "text": attrs.get("TEXT") or "", "icons": []})
        elif name == "icon" and self.stack:
            icon = attrs.get("BUILTIN")
            if icon:
                self.stack[-1]["icons"].append(icon)

    def endElement(self, name):
        if name != "node" or not self.stack:
            return
        info = self.stack.pop()
        if ICON not in [i.lower() for i in info["icons"]]:
            return
        text = info["text"].strip()
        if text.lower() == "bin":
            return
        todo_parent = None
        for anc in reversed(self.stack):
            if ICON in [i.lower() for i in anc["icons"]]:
                label = anc["text"].strip()
                if label.lower() != "bin":
                    todo_parent = anc["id"]
                    break
        extra = [i for i in info["icons"] if i.lower() != ICON]
        self.items.append({"id": info["id"], "text": text, "parent": todo_parent, "icons": extra})

def print_tree(items, parent_id, indent):
    for it in items:
        if it["parent"] == parent_id:
            icons = (" " + ",".join(it["icons"])) if it["icons"] else ""
            print("  " * indent + it["text"][:40] + icons)
            print_tree(items, it["id"], indent + 1)

def main(path):
    h = Handler()
    xml.sax.parse(path, h)
    print("Todo count:", len(h.items))
    roots = [it for it in h.items if it["parent"] is None]
    for r in roots:
        icons = (" " + ",".join(r["icons"])) if r["icons"] else ""
        print(r["text"][:40] + icons)
        print_tree(h.items, r["id"], 1)

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else r"E:\yixiaozi\02目标发展\03婚恋性福\1求偶\3相亲活动\相亲活动.mm")
