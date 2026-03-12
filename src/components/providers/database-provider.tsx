import { useMigrations } from "drizzle-orm/expo-sqlite/migrator"
import { useEffect, useRef, useState } from "react"
import { Text, View } from "react-native"

import { db } from "@/db/client"
import migrations from "@/db/migrations/migrations"
import { logError } from "@/modules/logging"
import { loadTracks } from "@/modules/player/player.store"

export function DatabaseProvider({
  children,
  onReady,
}: {
  children: React.ReactNode
  onReady?: () => void
}) {
  const [hasLoadedTracks, setHasLoadedTracks] = useState(false)
  const [loadError, setLoadError] = useState<Error | null>(null)
  const hasNotifiedReadyRef = useRef(false)
  const { success, error } = useMigrations(db, migrations)

  useEffect(() => {
    if (!success) {
      return
    }

    const loadData = async () => {
      try {
        await loadTracks()
        setHasLoadedTracks(true)
      } catch (dataError) {
        logError("Database data loading failed", dataError)
        setLoadError(dataError as Error)
      }
    }

    void loadData()
  }, [success])

  const resolvedError = loadError ?? error
  const isInitializing = !success || !hasLoadedTracks

  useEffect(() => {
    if (hasNotifiedReadyRef.current) {
      return
    }

    if (resolvedError || !isInitializing) {
      hasNotifiedReadyRef.current = true
      onReady?.()
    }
  }, [isInitializing, onReady, resolvedError])

  if (resolvedError) {
    const message = resolvedError.message || ""
    const isLegacySchemaConflict =
      message.includes("CREATE TABLE") || message.includes("already exists")

    return (
      <View className="flex-1 items-center justify-center bg-background p-4">
        <Text className="mb-2 text-center text-danger">Database Error</Text>
        <Text className="text-muted-foreground text-center text-sm">
          {isLegacySchemaConflict
            ? "Schema conflict detected. Clear app data or reinstall once to re-baseline migrations."
            : message}
        </Text>
      </View>
    )
  }

  return <View className="flex-1 bg-background">{children}</View>
}
