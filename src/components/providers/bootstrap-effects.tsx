import * as MediaLibrary from "expo-media-library"
import { useEffect } from "react"
import { AppState, type AppStateStatus } from "react-native"

import {
  runAutoScan,
  shouldTriggerAutoScanOnMediaLibraryEvent,
} from "@/modules/bootstrap/bootstrap.runtime"

export function BootstrapEffects() {
  useEffect(() => {
    let previousState: AppStateStatus = AppState.currentState

    const subscription = AppState.addEventListener("change", (nextState) => {
      const isReturningToForeground =
        (previousState === "background" || previousState === "inactive") &&
        nextState === "active"
      previousState = nextState

      if (!isReturningToForeground) {
        return
      }

      void runAutoScan()
    })

    return () => {
      subscription.remove()
    }
  }, [])

  useEffect(() => {
    const subscription = MediaLibrary.addListener((event) => {
      void runAutoScan({
        bypassThrottle: shouldTriggerAutoScanOnMediaLibraryEvent(event),
      })
    })

    return () => {
      subscription.remove()
    }
  }, [])

  return null
}
