import { Controller } from "@hotwired/stimulus"
import maplibregl from "maplibre-gl"
import { DayRoutesLayer } from "maps_maplibre/layers/day_routes_layer"
import { ReplayPanel } from "maps_maplibre/managers/replay_panel"
import { getMapStyle } from "maps_maplibre/utils/style_manager"

// Shared links: render a downsampled LineString (not thousands of circle features).
// Overlays must stay outside the MapLibre container — appending into the map
// container detaches the WebGL canvas on desktop.

function isMobileLike() {
  return (
    window.matchMedia("(max-width: 768px)").matches ||
    window.matchMedia("(pointer: coarse)").matches ||
    /iPhone|iPad|iPod|Android/i.test(navigator.userAgent)
  )
}

function renderPointLimit() {
  return isMobileLike() ? 5_000 : 8_000
}

function downsamplePoints(points, maxPoints) {
  if (!Array.isArray(points) || points.length <= maxPoints) return points
  const step = Math.ceil(points.length / maxPoints)
  const out = []
  for (let i = 0; i < points.length; i += step) out.push(points[i])
  const last = points[points.length - 1]
  if (out[out.length - 1] !== last) out.push(last)
  return out
}

export default class extends Controller {
  static values = {
    linkId: String,
    showPhotos: Boolean,
    byDay: Boolean,
    timezone: String,
  }

  static targets = [
    "map",
    "replayToggleBtn",
    "replayPanel",
    "replayScrubber",
    "replayScrubberTrack",
    "replayDensityContainer",
    "replayDayDisplay",
    "replayDayCount",
    "replayPrevDayButton",
    "replayNextDayButton",
    "replayTimeDisplay",
    "replaySpeedDisplay",
    "replayDataIndicator",
    "replayPlayButton",
    "replayFollowButton",
    "replayPlayIcon",
    "replayPauseIcon",
    "replaySpeedSlider",
    "replaySpeedLabel",
    "replayCycleControls",
    "replayPointCounter",
  ]

  connect() {
    if (this._connecting || this.map) return
    this._connecting = true
    this.selectedDay = null
    this.allPoints = []
    this.photoMarkers = []
    this.showLoading("正在加载轨迹…")
    this.initializeMap()
      .catch((err) => {
        console.error("shared-trip-map init failed", err)
        this.showError("地图加载失败，请刷新重试")
      })
      .finally(() => {
        this._connecting = false
      })
  }

  get mapContainer() {
    return this.hasMapTarget ? this.mapTarget : this.element
  }

  get overlayHost() {
    return this.mapContainer.parentElement || this.mapContainer
  }

  async initializeMap() {
    const style = await getMapStyle("light")

    this.map = new maplibregl.Map({
      container: this.mapContainer,
      style,
      center: [116.4, 39.9],
      zoom: 4,
      attributionControl: { compact: true },
      interactive: true,
      failIfMajorPerformanceCaveat: false,
    })

    this.map.addControl(
      new maplibregl.NavigationControl({ showCompass: false }),
      "top-right",
    )

    this.map.on("error", (event) => {
      console.error("MapLibre error", event?.error || event)
    })

    this.map.on("load", () => {
      this.clearLoading()
      if (this.byDayValue) {
        this.loadDayRoutes()
      } else {
        this.loadPoints()
      }
      if (this.showPhotosValue) this.loadPhotos()
    })
  }

  overlayEl(kind) {
    return this.overlayHost.querySelector(`[data-map-${kind}]`)
  }

  showLoading(message) {
    this.clearOverlay("loading")
    const el = document.createElement("p")
    el.dataset.mapLoading = "true"
    el.className =
      "pointer-events-none absolute inset-x-0 top-2 z-10 text-center text-sm text-base-content/70"
    el.textContent = message
    this.mountOverlay(el)
  }

  clearLoading() {
    this.clearOverlay("loading")
  }

  showError(message) {
    this.clearLoading()
    this.clearOverlay("error")
    const el = document.createElement("p")
    el.dataset.mapError = "true"
    el.className =
      "pointer-events-none absolute inset-x-0 top-2 z-10 text-center text-sm text-error"
    el.textContent = message
    this.mountOverlay(el)
  }

  maybeShowSampleNotice(rawCount, renderedCount) {
    if (rawCount <= renderedCount) return
    this.clearOverlay("notice")
    const el = document.createElement("p")
    el.dataset.mapNotice = "true"
    el.className =
      "pointer-events-none absolute bottom-2 left-2 z-10 rounded-md bg-base-100/90 px-2 py-1 text-xs text-base-content/70 shadow"
    el.textContent = `已简化显示 ${renderedCount.toLocaleString()} / ${rawCount.toLocaleString()} 个点`
    this.mountOverlay(el)
  }

  mountOverlay(el) {
    const host = this.overlayHost
    if (!host) return
    if (getComputedStyle(host).position === "static") {
      host.style.position = "relative"
    }
    host.appendChild(el)
  }

  clearOverlay(kind) {
    const el = this.overlayEl(kind)
    el?.remove()
  }

  clearOverlays() {
    for (const kind of ["loading", "error", "notice"]) {
      this.clearOverlay(kind)
    }
  }

  preparePoints(rawPoints) {
    const limit = renderPointLimit()
    const points = downsamplePoints(rawPoints, limit)
    return { rawPoints, points }
  }

  disconnect() {
    this.clearOverlays()
    if (this.replayPanel) {
      this.replayPanel.destroy()
      this.replayPanel = null
    }
    if (this.dayRoutesLayer) {
      this.dayRoutesLayer.remove()
      this.dayRoutesLayer = null
    }
    if (this.map) {
      this.map.remove()
      this.map = null
    }
  }

  async fetchPoints() {
    const res = await fetch(`/api/v1/shared/${this.linkIdValue}/points`)
    if (!res.ok) return []
    return res.json()
  }

  removeSharedRouteLayer() {
    if (!this.map) return
    if (this.map.getLayer("shared-route-line")) {
      this.map.removeLayer("shared-route-line")
    }
    if (this.map.getSource("shared-route")) {
      this.map.removeSource("shared-route")
    }
  }

  renderSharedRoute(points, { color = "#E53935" } = {}) {
    this.removeSharedRouteLayer()
    if (!points.length) return null

    const coords = points.map(([lon, lat]) => [lon, lat])
    if (coords.length === 1) {
      return this.renderSharedPoints([points[0]], { color })
    }

    this.map.addSource("shared-route", {
      type: "geojson",
      data: {
        type: "Feature",
        geometry: { type: "LineString", coordinates: coords },
      },
    })

    this.map.addLayer({
      id: "shared-route-line",
      type: "line",
      source: "shared-route",
      layout: { "line-join": "round", "line-cap": "round" },
      paint: {
        "line-color": color,
        "line-width": 3,
        "line-opacity": 0.9,
      },
    })

    return coords.reduce(
      (b, c) => b.extend(c),
      new maplibregl.LngLatBounds(coords[0], coords[0]),
    )
  }

  removeSharedPointsLayer() {
    if (!this.map) return
    if (this.map.getLayer("shared-points-circle")) {
      this.map.removeLayer("shared-points-circle")
    }
    if (this.map.getSource("shared-points")) {
      this.map.removeSource("shared-points")
    }
  }

  renderSharedPoints(points, { color = "#E53935" } = {}) {
    this.removeSharedPointsLayer()
    if (!points.length) return null

    const features = points.map(([lon, lat, ts]) => ({
      type: "Feature",
      geometry: { type: "Point", coordinates: [lon, lat] },
      properties: { timestamp: ts ?? null },
    }))

    this.map.addSource("shared-points", {
      type: "geojson",
      data: { type: "FeatureCollection", features },
    })

    this.map.addLayer({
      id: "shared-points-circle",
      type: "circle",
      source: "shared-points",
      paint: {
        "circle-radius": 6,
        "circle-color": color,
        "circle-opacity": 0.92,
        "circle-stroke-width": 1.5,
        "circle-stroke-color": "#ffffff",
      },
    })

    const coords = points.map(([lon, lat]) => [lon, lat])
    return coords.reduce(
      (b, c) => b.extend(c),
      new maplibregl.LngLatBounds(coords[0], coords[0]),
    )
  }

  async loadPoints() {
    try {
      const rawPoints = await this.fetchPoints()
      if (!rawPoints.length) {
        this.showError("暂无轨迹数据")
        return
      }

      const { rawPoints: raw, points } = this.preparePoints(rawPoints)
      const bounds = this.renderSharedRoute(points)
      this.maybeShowSampleNotice(raw.length, points.length)
      if (bounds) {
        this.map.fitBounds(bounds, { padding: 40, maxZoom: 15, duration: 0 })
      }
    } catch (err) {
      console.error("loadPoints failed", err)
      this.showError("轨迹加载失败，请检查网络后刷新")
    }
  }

  async loadDayRoutes() {
    try {
      const rawPoints = await this.fetchPoints()
      if (!rawPoints.length) {
        this.showError("暂无轨迹数据")
        return
      }

      const { rawPoints: raw, points } = this.preparePoints(rawPoints)
      this.allPoints = points.map(([lon, lat, ts]) => ({
        longitude: lon,
        latitude: lat,
        timestamp: ts,
      }))

      const bounds = this.renderSharedRoute(points)
      this.maybeShowSampleNotice(raw.length, points.length)
      if (bounds) {
        this.map.fitBounds(bounds, { padding: 40, maxZoom: 15, duration: 0 })
      }
    } catch (err) {
      console.error("loadDayRoutes failed", err)
      this.showError("轨迹加载失败，请检查网络后刷新")
    }
  }

  groupByDay(points) {
    const fmt = this.dayFormatter()
    const byDay = {}
    for (const [lon, lat, ts] of points) {
      const key = fmt.format(new Date(ts * 1000))
      if (!byDay[key]) byDay[key] = []
      byDay[key].push({ longitude: lon, latitude: lat, timestamp: ts })
    }
    return byDay
  }

  dayFormatter() {
    const opts = { year: "numeric", month: "2-digit", day: "2-digit" }
    try {
      return new Intl.DateTimeFormat("en-CA", {
        timeZone: this.timezoneValue || "UTC",
        ...opts,
      })
    } catch {
      return new Intl.DateTimeFormat("en-CA", { timeZone: "UTC", ...opts })
    }
  }

  recolorDots() {
    for (const dot of this.element.querySelectorAll("[data-day-dot]")) {
      const color = this.dayRoutesLayer?.getDayColor(dot.dataset.dayDot)
      if (color) dot.style.backgroundColor = color
    }
  }

  hoverDay(event) {
    const dayKey = event.params.dayKey
    if (!this.dayRoutesLayer?.getDayColor(dayKey)) return
    this.dayRoutesLayer.selectDay(dayKey)
  }

  leaveDay() {
    if (!this.dayRoutesLayer) return
    if (this.selectedDay) {
      this.dayRoutesLayer.selectDay(this.selectedDay)
    } else {
      this.dayRoutesLayer.selectAllDays()
    }
  }

  toggleDay(event) {
    this.pinDay(event.params.dayKey)
  }

  pinDay(dayKey) {
    if (!this.dayRoutesLayer?.getDayColor(dayKey)) return

    if (this.selectedDay === dayKey) {
      this.selectedDay = null
      this.dayRoutesLayer.selectAllDays()
      const full = this.dayRoutesLayer.getFullBounds()
      if (full) this.map.fitBounds(full, { padding: 40, maxZoom: 15 })
    } else {
      this.selectedDay = dayKey
      this.dayRoutesLayer.selectDay(dayKey)
      const dayBounds = this.dayRoutesLayer.getDayBounds(dayKey)
      if (dayBounds) this.map.fitBounds(dayBounds, { padding: 60, maxZoom: 15 })
    }
    this.markActiveRow()
    this.replayPanel?.syncToDay(dayKey)
  }

  toggleReplay() {
    if (!this.replayPanel) {
      this.replayPanel = new ReplayPanel({
        controller: this,
        map: this.map,
        dayRoutesLayer: this.dayRoutesLayer,
        element: this.element,
        timezone: this.timezoneValue,
        allPoints: this.allPoints,
        getPhotos: () =>
          (this.photoMarkers || []).map(({ photo }) => ({
            id: photo.id,
            taken_at: photo.taken_at,
            thumbnail_url: photo.thumbnail_url,
            latitude: photo.latitude,
            longitude: photo.longitude,
            source: photo.source,
          })),
        onReplayPhotosActive: (active) => {
          for (const { marker } of this.photoMarkers || []) {
            marker.getElement().style.display = active ? "none" : ""
          }
        },
      })
    }
    this.replayPanel.toggle()
  }

  replayScrubberHover(event) {
    this.replayPanel?.scrubberHover(event)
  }

  replayPrevDay() {
    this.replayPanel?.prevDay()
  }

  replayNextDay() {
    this.replayPanel?.nextDay()
  }

  replayCyclePrev() {
    this.replayPanel?.cyclePrev()
  }

  replayCycleNext() {
    this.replayPanel?.cycleNext()
  }

  replayTogglePlayback() {
    this.replayPanel?.togglePlayback()
  }

  replayRecenterFollow() {
    this.replayPanel?.recenterFollow()
  }

  replaySpeedChange(event) {
    this.replayPanel?.speedChange(event)
  }

  markActiveRow() {
    for (const row of this.element.querySelectorAll("[data-day-key]")) {
      row.classList.toggle("ring-2", row.dataset.dayKey === this.selectedDay)
      row.classList.toggle(
        "ring-primary",
        row.dataset.dayKey === this.selectedDay,
      )
    }
  }

  async loadPhotos() {
    const res = await fetch(`/api/v1/shared/${this.linkIdValue}/photos`)
    if (!res.ok) return
    const photos = await res.json()

    this.photoMarkers = []
    for (const photo of photos) {
      if (photo.latitude == null || photo.longitude == null) continue
      const el = document.createElement("img")
      el.src = photo.thumbnail_url
      el.className =
        "shared-photo-marker rounded-full border-2 border-white shadow"
      el.style.width = "32px"
      el.style.height = "32px"
      el.style.objectFit = "cover"
      const marker = new maplibregl.Marker({ element: el })
        .setLngLat([photo.longitude, photo.latitude])
        .addTo(this.map)
      this.photoMarkers.push({ photo, marker })
    }
  }
}
