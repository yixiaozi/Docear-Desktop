import sys
p = r"E:\yixiaozi\07有条不紊\02时间管理\Hash Photos.mm"
s = open(p, encoding="utf-8").read()
marker = 'ID="ID_966018587"'
i = s.index(marker)
i = s.index(">", i) + 1
depth = 1
pos = i
while depth > 0 and pos < len(s):
    o = s.find("<node", pos)
    c = s.find("</node>", pos)
    if c < 0:
        print("no close, depth=", depth)
        break
    if o >= 0 and o < c:
        depth += 1
        pos = o + 5
    else:
        depth -= 1
        if depth == 0:
            print("activities close at", c)
            break
        pos = c + 7
else:
    print("failed depth", depth)
body = s[i:]
print("opens", body.count("<node"), "closes", body.count("</node>"))
