import {
  DarkTheme,
  DefaultTheme,
  ThemeProvider,
} from "@react-navigation/native"
import * as Notifications from "expo-notifications"
import { Stack, useRouter, useSegments } from "expo-router"
import * as SplashScreen from "expo-splash-screen"
import { HeroUINativeProvider } from "heroui-native"
import { type ReactNode, useCallback, useEffect, useRef } from "react"
import { View } from "react-native"
import { GestureHandlerRootView } from "react-native-gesture-handler"
import Animated, {
  useDerivedValue,
  useAnimatedStyle,
  withTiming,
} from "react-native-reanimated"
import { useSafeAreaInsets } from "react-native-safe-area-context"
import { useUniwind } from "uniwind"

import { PlayerSheet } from "@/components/blocks/player/player-sheet"
import { RootProviders } from "@/components/providers/root-providers"
import { getTabBarHeight, MINI_PLAYER_HEIGHT } from "@/constants/layout"
import {
  handleBootstrapDatabaseError,
  handleBootstrapDatabaseReady,
} from "@/modules/bootstrap/bootstrap.runtime"
import {
  INDEXER_NOTIFICATION_ACTION_CANCEL,
  INDEXER_NOTIFICATION_ACTION_PAUSE,
  INDEXER_NOTIFICATION_ACTION_RESUME,
} from "@/modules/indexer/indexer-notification.service"
import {
  cancelIndexing,
  pauseIndexing,
  resumeIndexing,
} from "@/modules/indexer/indexer.service"
import { ROOT_MODAL_SCREEN_OPTIONS } from "@/modules/navigation/stack"
import { useUIStore } from "@/modules/ui/ui.store"
import { useThemeColors } from "@/modules/ui/theme"
import { usePlayerStore } from "@/modules/player/player.store"

import "../global.css"

const TOAST_OFFSET_ANIMATION_DURATION_MS = 250
const TOAST_HIDDEN_BOTTOM_GAP = 0
const TOAST_VISIBLE_BOTTOM_GAP = 0
const SETTINGS_FOLDER_FILTERS_ACTION_HEIGHT = 56
const SETTINGS_FOLDER_FILTERS_ACTION_TOP_PADDING = 12

function ToastAnimatedWrapper({
  children,
  extraBottom,
}: {
  children: ReactNode
  extraBottom: number
}) {
  const animatedExtraBottom = useDerivedValue(() => {
    return withTiming(extraBottom, {
      duration: TOAST_OFFSET_ANIMATION_DURATION_MS,
    })
  }, [extraBottom])

  const animatedStyle = useAnimatedStyle(() => ({
    paddingBottom: animatedExtraBottom.value,
    zIndex: 2100,
    elevation: 2100,
  }))

  return (
    <Animated.View
      style={[{ flex: 1 }, animatedStyle]}
      pointerEvents="box-none"
    >
      {children}
    </Animated.View>
  )
}

SplashScreen.setOptions({
  duration: 1000,
  fade: true,
})
void SplashScreen.preventAutoHideAsync().catch(() => {
  // Splash screen might be already prevented by native/runtime.
})

export default function Layout() {
  const router = useRouter()
  const { theme: currentTheme } = useUniwind()
  const theme = useThemeColors()
  const segments = useSegments()
  const insets = useSafeAreaInsets()
  const barsVisible = useUIStore((state) => state.barsVisible)
  const currentTrack = usePlayerStore((state) => state.currentTrack)
  const hasHiddenSplashRef = useRef(false)
  const handledNotificationResponseRef = useRef<string | null>(null)

  useEffect(() => {
    const handleNotificationResponse = (
      response: Notifications.NotificationResponse | null
    ) => {
      if (!response) {
        return
      }

      const responseKey = `${response.notification.request.identifier}:${response.actionIdentifier}`
      if (handledNotificationResponseRef.current === responseKey) {
        return
      }

      handledNotificationResponseRef.current = responseKey

      const source = response.notification.request.content.data?.source
      if (
        source === "indexer-progress" &&
        response.actionIdentifier !== Notifications.DEFAULT_ACTION_IDENTIFIER
      ) {
        switch (response.actionIdentifier) {
          case INDEXER_NOTIFICATION_ACTION_PAUSE:
            pauseIndexing()
            return
          case INDEXER_NOTIFICATION_ACTION_RESUME:
            resumeIndexing()
            return
          case INDEXER_NOTIFICATION_ACTION_CANCEL:
            cancelIndexing()
            return
          default:
            return
        }
      }

      const route = response.notification.request.content.data?.route
      if (typeof route !== "string" || route.length === 0) {
        return
      }
      router.push(route as never)
    }

    void Notifications.getLastNotificationResponseAsync().then(
      handleNotificationResponse
    )

    const subscription = Notifications.addNotificationResponseReceivedListener(
      handleNotificationResponse
    )

    return () => {
      subscription.remove()
    }
  }, [router])

  const hideSplash = useCallback(() => {
    if (hasHiddenSplashRef.current) {
      return
    }

    hasHiddenSplashRef.current = true
    void SplashScreen.hideAsync().catch(() => {
      // Ignore hide race if splash is already hidden.
    })
  }, [])
  const notifyDatabaseReady = useCallback(async () => {
    await handleBootstrapDatabaseReady()
    hideSplash()
  }, [hideSplash])
  const notifyDatabaseError = useCallback(() => {
    handleBootstrapDatabaseError()
    hideSplash()
  }, [hideSplash])
  const tabBarHeight = getTabBarHeight(insets.bottom)
  const hasMiniPlayer = currentTrack !== null
  const isMainTabsRoute = segments[0] === "(main)"
  const isFolderFiltersRoute =
    segments[0] === "settings" && segments[1] === "folder-filters"
  const folderFiltersToastOffset = isFolderFiltersRoute
    ? SETTINGS_FOLDER_FILTERS_ACTION_HEIGHT +
      Math.max(insets.bottom, SETTINGS_FOLDER_FILTERS_ACTION_TOP_PADDING)
    : 0
  const toastExtraBottomOffset = isMainTabsRoute
    ? barsVisible
      ? tabBarHeight +
        (hasMiniPlayer ? MINI_PLAYER_HEIGHT : 0) +
        TOAST_VISIBLE_BOTTOM_GAP
      : TOAST_HIDDEN_BOTTOM_GAP
    : folderFiltersToastOffset

  const toastContentWrapper = useCallback(
    (children: ReactNode) => {
      return (
        <ToastAnimatedWrapper extraBottom={toastExtraBottomOffset}>
          {children}
        </ToastAnimatedWrapper>
      )
    },
    [toastExtraBottomOffset]
  )

  const navigationTheme = {
    ...(currentTheme === "dark" ? DarkTheme : DefaultTheme),
    colors: {
      ...(currentTheme === "dark" ? DarkTheme.colors : DefaultTheme.colors),
      background: theme.background,
      card: theme.background,
      text: theme.foreground,
      border: theme.border,
      notification: theme.accent,
    },
  }

  return (
    <GestureHandlerRootView
      style={{ flex: 1, backgroundColor: theme.background }}
    >
      <ThemeProvider value={navigationTheme}>
        <View style={{ flex: 1, backgroundColor: theme.background }}>
          <HeroUINativeProvider
            config={{
              devInfo: { stylingPrinciples: false },
              toast: {
                defaultProps: {
                  placement: "bottom",
                },
                contentWrapper: toastContentWrapper,
              },
            }}
          >
            <RootProviders
              onDatabaseReady={notifyDatabaseReady}
              onDatabaseError={notifyDatabaseError}
            >
              <View className="flex-1">
                <Stack
                  screenOptions={{
                    headerShown: false,
                    contentStyle: { backgroundColor: theme.background },
                  }}
                >
                  <Stack.Screen name="(main)" />
                  <Stack.Screen name="settings" options={ROOT_MODAL_SCREEN_OPTIONS} />
                </Stack>
                <PlayerSheet />
              </View>
            </RootProviders>
          </HeroUINativeProvider>
        </View>
      </ThemeProvider>
    </GestureHandlerRootView>
  )
}
