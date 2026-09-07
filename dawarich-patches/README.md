# Dawarich 分享页空白修复

## 问题

分享链接 `https://footprint.mantoublog.top/s/78dbf9e8-0156-4ab7-925d-c65859b42a58` 在电脑端地图区域空白。

## 根因（已复现）

1. **时间范围异常**：分享页标题为 `1980-09-03 → 2100-09-03`（43830 天），API 返回 **92,885** 个轨迹点（约 3.5MB，加载需 ~36 秒）。
2. **服务端未限流**：上游 Dawarich 的 `PointsController` 已有 `MAX_POINTS = 10_000`，但当前 VPS 返回 92885 点，说明运行的是旧版或未生效的代码。
3. **前端渲染方式错误**：现有补丁把每个 GPS 点渲染成 MapLibre **圆点图层**（桌面端仍约 3 万个 Feature），WebGL 过载后 **canvas 从 DOM 脱离**，页面只剩灰色底和「已简化显示 … 个点」提示。

## 修复

`app/javascript/controllers/shared_trip_map_controller.js`：

- 改用 **LineString 折线**（1 个 GeoJSON Feature）代替大量圆点
- 桌面最多 **8000** 点、移动端 **5000** 点
- 加载/提示文字挂到 **地图容器外的父元素**，避免破坏 MapLibre DOM
- 防止 Stimulus 重复 `connect`

## 部署（VPS）

```bash
# 1. 把 dawarich-patches 目录拷到 VPS
# 2. SSH 登录后执行（第二个参数为 Dawarich 源码/挂载目录）
bash dawarich-patches/scripts/deploy-footprint-share-fix.sh /opt/dawarich
```

部署后 **Ctrl+F5** 强刷分享页。

## 建议同时做

1. **重新生成分享链接**：在 Dawarich 里把日期范围改成实际需要展示的时间段（不要 1980–2100）。
2. **升级 Dawarich** 或确认 `app/controllers/api/v1/shared/points_controller.rb` 中 `MAX_POINTS = 10_000` 已生效，避免 API 一次返回近 10 万点。
