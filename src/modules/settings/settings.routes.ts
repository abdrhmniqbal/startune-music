import type { SettingsRouteDefinition } from "./settings.types"

export const SETTINGS_CATEGORY_ROUTES: SettingsRouteDefinition[] = [
  {
    name: "appearance",
    title: "Appearance",
    description: "Theme and visual preferences.",
  },
  {
    name: "notifications",
    title: "Notifications",
    description: "Notification behavior and alerts.",
  },
  {
    name: "library",
    title: "Library",
    description: "Scanning, filters, and indexing behavior.",
  },
  {
    name: "advanced",
    title: "Advanced",
    description: "System-level and troubleshooting settings.",
  },
  {
    name: "about",
    title: "About",
    description: "App information and build details.",
  },
]

export const SETTINGS_SCREEN_TITLES: Record<string, string> = {
  index: "Settings",
  appearance: "Appearance",
  notifications: "Notifications",
  library: "Library",
  advanced: "Advanced",
  about: "About",
  "folder-filters": "Folder Filters",
  "track-duration-filter": "Track Duration Filter",
  "log-level": "Log Level",
}
