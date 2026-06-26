import xml.etree.ElementTree as ET
import html
import sys

path = sys.argv[1] if len(sys.argv) > 1 else r"E:\yixiaozi\02目标发展\03婚恋性福\1求偶\3相亲活动\相亲活动.mm"
TODO = "hourglass"

def parse_node(elem, parent_id=None):
    node_id = elem.get("ID")
    text = elem.get("TEXT") or ""
    icons = [ic.get("BUILTIN") for ic in elem.findall("icon") if ic.get("BUILTIN")]
    info = {"id": node_id, "parent_id": parent_id, "text": text, "icons": icons}
    nodes = [info]
    for child in elem.findall("node"):
        nodes.extend(parse_node(child, node_id))
    return nodes

tree = ET.parse(path)
root = tree.getroot()
all_nodes = []
for child in root.findall("node"):
    all_nodes.extend(parse_node(child, None))

children = {}
nodes_by_id = {}
for n in all_nodes:
    nodes_by_id[n["id"]] = n
    children.setdefault(n["parent_id"], []).append(n)

def has_target(n):
    return TODO in n["icons"] and n["text"].strip().lower() != "bin"

def has_target_desc(node_id):
    n = nodes_by_id.get(node_id)
    if n and has_target(n):
        return True
    for c in children.get(node_id, []):
        if has_target_desc(c["id"]):
            return True
    return False

todos = []

def collect(node_id, depth=0):
    for c in children.get(node_id, []):
        is_t = has_target(c)
        has_d = has_target_desc(c["id"])
        if is_t:
            label = html.unescape(c["text"]).replace("\n", " ")[:60]
            todos.append((depth, label))
            collect(c["id"], depth + 1)
        elif has_d:
            collect(c["id"], depth)

map_root = [n for n in all_nodes if n["parent_id"] is None][0]["id"]
collect(map_root)
print("Todo count:", len(todos))
for d, t in todos:
    if any(k in t for k in ["吴茜", "决定", "其实", "刘全杰", "郑薇"]):
        print("  " * d + t)
