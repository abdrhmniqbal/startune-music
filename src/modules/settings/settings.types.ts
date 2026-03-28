export type SettingsRouteName =
  | "index"
  | "appearance"
  | "library"
  | "advanced"
  | "about"
  | "folder-filters"
  | "track-duration-filter"
  | "log-level"

export interface SettingsRouteDefinition {
  name: SettingsRouteName
  title: string
  description?: string
}
