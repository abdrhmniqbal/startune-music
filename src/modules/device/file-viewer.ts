import { Linking } from "react-native"
import { open as openFileViewer } from "react-native-file-viewer-turbo"

import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { resolvePlayableFileUri } from "@/utils/file-path"

interface OpenDeviceFileOptions {
  uri: string
  trackId?: string
}

export async function openDeviceFile({
  uri,
  trackId,
}: OpenDeviceFileOptions): Promise<boolean> {
  const resolvedUri = await resolvePlayableFileUri(uri)

  logInfo("Opening device file", {
    trackId,
    originalUri: uri,
    resolvedUri,
  })

  try {
    await openFileViewer(resolvedUri, {
      showOpenWithDialog: true,
      showAppsSuggestions: false,
    })

    logInfo("Opened device file with native viewer", {
      trackId,
      resolvedUri,
    })
    return true
  } catch (error) {
    logWarn("Native file viewer failed, trying URL fallback", {
      trackId,
      resolvedUri,
      error: error instanceof Error ? error.message : String(error),
    })
  }

  try {
    const openableUri = encodeURI(resolvedUri)
    const canOpenFile = await Linking.canOpenURL(openableUri)

    if (!canOpenFile) {
      logWarn("No handler available for device file URL", {
        trackId,
        resolvedUri: openableUri,
      })
      return false
    }

    await Linking.openURL(openableUri)
    logInfo("Opened device file via URL fallback", {
      trackId,
      resolvedUri: openableUri,
    })
    return true
  } catch (error) {
    logError("Failed to open device file", error, {
      trackId,
      resolvedUri,
    })
    return false
  }
}
