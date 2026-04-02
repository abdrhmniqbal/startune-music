import * as Notifications from "expo-notifications"
import { Platform } from "react-native"

import type { IndexerScanProgress } from "@/modules/indexer/indexer.types"
import { logError, logInfo } from "@/modules/logging/logging.service"
import { ensureIndexerNotificationsConfigLoaded } from "@/modules/settings/indexer-notifications"

const INDEXER_NOTIFICATION_CHANNEL_ID = "indexer-progress"
const INDEXER_NOTIFICATION_ROUTE = "/(main)/(library)"
const INDEXER_NOTIFICATION_ID = "indexer-progress-active"
const INDEXER_NOTIFICATION_ACTIVE_CATEGORY_ID = "indexer-progress-active-actions"
const INDEXER_NOTIFICATION_PAUSED_CATEGORY_ID = "indexer-progress-paused-actions"

export const INDEXER_NOTIFICATION_ACTION_PAUSE = "INDEXER_NOTIFICATION_ACTION_PAUSE"
export const INDEXER_NOTIFICATION_ACTION_RESUME = "INDEXER_NOTIFICATION_ACTION_RESUME"
export const INDEXER_NOTIFICATION_ACTION_CANCEL = "INDEXER_NOTIFICATION_ACTION_CANCEL"

let notificationsConfigured = false
let permissionResolved = false
let notificationsGranted = false
let permissionRequestedThisSession = false
let activeNotificationId: string | null = null
let lastNotificationSignature: string | null = null

function isIndexerNotification(notification: Notifications.Notification) {
  const payload = notification.request.content.data as
    | { source?: unknown }
    | undefined
  return payload?.source === "indexer-progress"
}

async function dismissPresentedIndexerNotifications(
  options?: { keepId?: string | null }
) {
  const keepId = options?.keepId ?? null
  try {
    const presented = await Notifications.getPresentedNotificationsAsync()
    const pendingDismissals = presented
      .filter(isIndexerNotification)
      .filter((notification) => notification.request.identifier !== keepId)
      .map((notification) =>
        Notifications.dismissNotificationAsync(notification.request.identifier)
      )

    if (pendingDismissals.length > 0) {
      await Promise.allSettled(pendingDismissals)
    }
  } catch (error) {
    logError("Failed to dismiss presented indexer notifications", error)
  }
}

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
      shouldShowBanner: false,
      shouldShowList: true,
    }),
  })

  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync(
      INDEXER_NOTIFICATION_CHANNEL_ID,
      {
        name: "Library Indexing",
        description: "Shows music library indexing progress",
        importance: Notifications.AndroidImportance.LOW,
        vibrationPattern: [0],
        enableVibrate: false,
        sound: null,
      }
    )
  }

  await Notifications.setNotificationCategoryAsync(
    INDEXER_NOTIFICATION_ACTIVE_CATEGORY_ID,
    [
      {
        identifier: INDEXER_NOTIFICATION_ACTION_PAUSE,
        buttonTitle: "Pause",
      },
      {
        identifier: INDEXER_NOTIFICATION_ACTION_CANCEL,
        buttonTitle: "Cancel",
        options: {
          isDestructive: true,
        },
      },
    ]
  )

  await Notifications.setNotificationCategoryAsync(
    INDEXER_NOTIFICATION_PAUSED_CATEGORY_ID,
    [
      {
        identifier: INDEXER_NOTIFICATION_ACTION_RESUME,
        buttonTitle: "Resume",
      },
      {
        identifier: INDEXER_NOTIFICATION_ACTION_CANCEL,
        buttonTitle: "Cancel",
        options: {
          isDestructive: true,
        },
      },
    ]
  )

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

async function replaceIndexerNotification(
  title: string,
  body: string,
  options?: { paused?: boolean; interactive?: boolean }
) {
  const notificationsEnabled = await ensureIndexerNotificationsConfigLoaded()
  if (!notificationsEnabled) {
    await dismissPresentedIndexerNotifications()
    if (activeNotificationId) {
      try {
        await Notifications.dismissNotificationAsync(INDEXER_NOTIFICATION_ID)
      } catch (error) {
        logError("Failed to dismiss indexer notification while disabled", error)
      } finally {
        activeNotificationId = null
        lastNotificationSignature = null
      }
    }

    return
  }

  if (!(await ensureNotificationPermission())) {
    return
  }

  try {
    const paused = options?.paused ?? false
    const interactive = options?.interactive ?? false
    const categoryIdentifier = interactive
      ? paused
        ? INDEXER_NOTIFICATION_PAUSED_CATEGORY_ID
        : INDEXER_NOTIFICATION_ACTIVE_CATEGORY_ID
      : undefined
    const signature = `${title}\n${body}`
    if (activeNotificationId && lastNotificationSignature === signature) {
      return
    }

    if (!activeNotificationId) {
      await dismissPresentedIndexerNotifications({ keepId: INDEXER_NOTIFICATION_ID })
    }

    await Notifications.scheduleNotificationAsync({
      identifier: INDEXER_NOTIFICATION_ID,
      content: {
        title,
        body,
        sound: false,
        sticky: true,
        autoDismiss: false,
        categoryIdentifier,
        data: {
          source: "indexer-progress",
          route: INDEXER_NOTIFICATION_ROUTE,
          paused,
        },
      },
      trigger: null,
    })
    activeNotificationId = INDEXER_NOTIFICATION_ID
    lastNotificationSignature = signature
  } catch (error) {
    logError("Failed to update indexer progress notification", error)
  }
}

export async function beginIndexerProgressNotification() {
  await replaceIndexerNotification(
    "Indexing music library",
    "Preparing your library scan...",
    { interactive: true }
  )
}

export async function updateIndexerProgressNotification(
  progress: IndexerScanProgress
) {
  const title =
    progress.phase === "scanning"
      ? "Scanning music files"
      : "Processing music metadata"

  await replaceIndexerNotification(title, formatProgressBody(progress), {
    interactive: true,
  })
}

export async function completeIndexerProgressNotification(
  totalFiles: number
) {
  await replaceIndexerNotification(
    "Library indexing complete",
    `${totalFiles} tracks updated`,
    { interactive: false }
  )
}

export async function failIndexerProgressNotification() {
  await replaceIndexerNotification(
    "Library indexing failed",
    "Tap to reopen the app and try again",
    { interactive: false }
  )
}

export async function pauseIndexerProgressNotification(progress: {
  current: number
  total: number
  currentFile: string
}) {
  const body =
    progress.total > 0
      ? `${progress.current}/${progress.total} • Paused`
      : "Paused"

  await replaceIndexerNotification("Library indexing paused", body, {
    paused: true,
    interactive: true,
  })
}

export async function resumeIndexerProgressNotification(
  progress: IndexerScanProgress
) {
  await replaceIndexerNotification(
    progress.phase === "scanning"
      ? "Scanning music files"
      : "Processing music metadata",
    formatProgressBody(progress),
    {
      paused: false,
      interactive: true,
    }
  )
}

export async function dismissIndexerProgressNotification() {
  try {
    await Notifications.dismissNotificationAsync(INDEXER_NOTIFICATION_ID)
  } catch (error) {
    logError("Failed to dismiss indexer progress notification", error)
  } finally {
    await dismissPresentedIndexerNotifications()
    activeNotificationId = null
    lastNotificationSignature = null
  }
}
