import * as Notifications from "expo-notifications"
import { Platform } from "react-native"

import type { IndexerScanProgress } from "@/modules/indexer/indexer.types"
import { logError, logInfo } from "@/modules/logging/logging.service"

const INDEXER_NOTIFICATION_CHANNEL_ID = "indexer-progress"
const INDEXER_NOTIFICATION_ROUTE = "/(main)/(library)"

let notificationsConfigured = false
let permissionResolved = false
let notificationsGranted = false
let permissionRequestedThisSession = false
let activeNotificationId: string | null = null

function formatProgressBody(progress: IndexerScanProgress) {
  const fileName = progress.currentFile || "Preparing..."
  if (progress.total <= 0) {
    return `${fileName}`
  }

  const percent = Math.min(
    100,
    Math.max(0, Math.round((progress.current / progress.total) * 100))
  )
  return `${progress.current}/${progress.total} • ${percent}% • ${fileName}`
}

async function configureNotifications() {
  if (notificationsConfigured) {
    return
  }

  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldPlaySound: false,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  })

  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync(
      INDEXER_NOTIFICATION_CHANNEL_ID,
      {
        name: "Library Indexing",
        description: "Shows music library indexing progress",
        importance: Notifications.AndroidImportance.DEFAULT,
        vibrationPattern: [0],
        enableVibrate: false,
        sound: null,
      }
    )
  }

  notificationsConfigured = true
}

async function ensureNotificationPermission() {
  await configureNotifications()

  if (permissionResolved) {
    return notificationsGranted
  }

  const existingPermissions = await Notifications.getPermissionsAsync()
  if (existingPermissions.granted) {
    permissionResolved = true
    notificationsGranted = true
    return true
  }

  if (!permissionRequestedThisSession) {
    permissionRequestedThisSession = true
    const requestedPermissions = await Notifications.requestPermissionsAsync()
    if (requestedPermissions.granted) {
      permissionResolved = true
      notificationsGranted = true
      logInfo("Notification permission granted for indexer progress")
      return true
    }
  }

  permissionResolved = true
  notificationsGranted = false
  logInfo("Notification permission denied for indexer progress")
  return false
}

async function replaceIndexerNotification(title: string, body: string) {
  if (!(await ensureNotificationPermission())) {
    return
  }

  try {
    if (activeNotificationId) {
      await Notifications.dismissNotificationAsync(activeNotificationId)
      activeNotificationId = null
    }

    activeNotificationId = await Notifications.scheduleNotificationAsync({
      content: {
        title,
        body,
        sound: false,
        data: {
          source: "indexer-progress",
          route: INDEXER_NOTIFICATION_ROUTE,
        },
      },
      trigger: null,
    })
  } catch (error) {
    logError("Failed to update indexer progress notification", error)
  }
}

export async function beginIndexerProgressNotification() {
  await replaceIndexerNotification(
    "Indexing music library",
    "Preparing your library scan..."
  )
}

export async function updateIndexerProgressNotification(
  progress: IndexerScanProgress
) {
  const title =
    progress.phase === "scanning"
      ? "Scanning music files"
      : "Processing music metadata"

  await replaceIndexerNotification(title, formatProgressBody(progress))
}

export async function completeIndexerProgressNotification(
  totalFiles: number
) {
  await replaceIndexerNotification(
    "Library indexing complete",
    `${totalFiles} tracks updated`
  )
}

export async function failIndexerProgressNotification() {
  await replaceIndexerNotification(
    "Library indexing failed",
    "Tap to reopen the app and try again"
  )
}

export async function dismissIndexerProgressNotification() {
  if (!activeNotificationId) {
    return
  }

  try {
    await Notifications.dismissNotificationAsync(activeNotificationId)
  } catch (error) {
    logError("Failed to dismiss indexer progress notification", error)
  } finally {
    activeNotificationId = null
  }
}
