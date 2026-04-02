import {
  BottomTabBar,
  type BottomTabBarProps,
} from "@react-navigation/bottom-tabs"
import { Tabs } from "expo-router"
import { useCallback } from "react"
import Animated, {
  useDerivedValue,
  useAnimatedStyle,
  withTiming,
} from "react-native-reanimated"
import { useSafeAreaInsets } from "react-native-safe-area-context"

import { MiniPlayer } from "@/components/blocks/mini-player"
import LocalHomeIcon from "@/components/icons/local/home"
import LocalHomeSolidIcon from "@/components/icons/local/home-solid"
import LocalLibraryIcon from "@/components/icons/local/library"
import LocalLibrarySolidIcon from "@/components/icons/local/library-solid"
import LocalSearchIcon from "@/components/icons/local/search"
import LocalSearchSolidIcon from "@/components/icons/local/search-solid"
import {
  getTabBarBottomPadding,
  getTabBarHeight,
  MINI_PLAYER_HEIGHT,
} from "@/constants/layout"
import { useUIStore } from "@/modules/ui/ui.store"
import { useThemeColors } from "@/modules/ui/theme"

const TAB_HIDE_DURATION_MS = 250
const TAB_HIDE_EXTRA_OFFSET = 16

export default function MainLayout() {
  const theme = useThemeColors()
  const insets = useSafeAreaInsets()
  const barsVisible = useUIStore((state) => state.barsVisible)
  const tabBarBottomPadding = getTabBarBottomPadding(insets.bottom)
  const tabBarHeight = getTabBarHeight(insets.bottom)
  const hiddenOffset = tabBarHeight + MINI_PLAYER_HEIGHT + TAB_HIDE_EXTRA_OFFSET
  const translateY = useDerivedValue(() => {
    return withTiming(barsVisible ? 0 : hiddenOffset, {
      duration: TAB_HIDE_DURATION_MS,
    })
  }, [barsVisible, hiddenOffset])

  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [
        {
          translateY: translateY.value,
        },
      ],
    }
  })

  const renderTabBar = useCallback(
    (props: BottomTabBarProps) => {
      return (
        <Animated.View
          pointerEvents="box-none"
          style={[
            animatedStyle,
            {
              position: "absolute",
              bottom: 0,
              left: 0,
              right: 0,
            },
          ]}
        >
          <MiniPlayer bottomOffset={tabBarHeight} />
          <BottomTabBar {...props} />
        </Animated.View>
      )
    },
    [animatedStyle, tabBarHeight]
  )

  return (
    <Tabs
      tabBar={renderTabBar}
      detachInactiveScreens={false}
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: theme.foreground,
        tabBarInactiveTintColor: theme.muted,
        tabBarHideOnKeyboard: true,
        freezeOnBlur: false,
        sceneStyle: {
          backgroundColor: theme.background,
        },
        tabBarStyle: {
          backgroundColor: theme.background,
          borderTopWidth: 1,
          borderTopColor: theme.border,
          height: tabBarHeight,
          paddingTop: 8,
          paddingBottom: tabBarBottomPadding,
        },
        tabBarLabelStyle: {
          fontSize: 12,
          fontWeight: "600" as const,
        },
        animation: "none",
      }}
    >
      <Tabs.Screen
        name="(home)"
        options={{
          title: "Home",
          tabBarIcon: ({ color, size, focused }) =>
            focused ? (
              <LocalHomeSolidIcon
                fill="none"
                color={color}
                width={size}
                height={size}
              />
            ) : (
              <LocalHomeIcon
                fill="none"
                color={color}
                width={size}
                height={size}
              />
            ),
        }}
      />
      <Tabs.Screen
        name="(search)"
        options={{
          title: "Search",
          tabBarIcon: ({ color, size, focused }) =>
            focused ? (
              <LocalSearchSolidIcon
                fill="none"
                color={color}
                width={size}
                height={size}
              />
            ) : (
              <LocalSearchIcon
                fill="none"
                color={color}
                width={size}
                height={size}
              />
            ),
        }}
      />
      <Tabs.Screen
        name="(library)"
        options={{
          title: "Library",
          popToTopOnBlur: true,
          tabBarIcon: ({ color, size, focused }) =>
            focused ? (
              <LocalLibrarySolidIcon
                fill="none"
                color={color}
                width={size}
                height={size}
              />
            ) : (
              <LocalLibraryIcon
                fill="none"
                color={color}
                width={size}
                height={size}
              />
            ),
        }}
      />
    </Tabs>
  )
}
