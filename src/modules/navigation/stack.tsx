import { withLayoutContext } from "expo-router"
import type { ReactNode } from "react"
import Transition from "react-native-screen-transitions"
import {
  createNativeStackNavigator,
  type NativeStackNavigationOptions as TransitionStackNavigationOptions,
} from "react-native-screen-transitions/native-stack"

type NavigationThemeColors = {
  background: string
  foreground: string
}

const { Navigator } = createNativeStackNavigator()

export const TransitionStack = withLayoutContext<
  TransitionStackNavigationOptions,
  typeof Navigator
>(Navigator)

export const HIDDEN_STACK_SCREEN_OPTIONS = {
  headerShown: false,
} as const

export const ROOT_MODAL_SCREEN_OPTIONS = {
  headerShown: false,
  presentation: "modal" as const,
  animation: "slide_from_bottom" as const,
}

export function getDefaultNativeStackOptions(theme: NavigationThemeColors) {
  return {
    headerStyle: {
      backgroundColor: theme.background,
    },
    headerTintColor: theme.foreground,
    headerShadowVisible: false,
    headerTitleAlign: "center" as const,
    contentStyle: {
      backgroundColor: theme.background,
    },
    animation: "default" as const,
  }
}

export function getLargeTitleRootScreenOptions(options: {
  title: string
  headerRight?: () => ReactNode
  headerLeft?: () => ReactNode
}) {
  return {
    title: options.title,
    headerLargeTitle: true,
    headerTitleAlign: "left" as const,
    headerRight: options.headerRight,
    headerLeft: options.headerLeft,
  }
}

export function getBackButtonScreenOptions(
  title: string,
  headerLeft: () => ReactNode
) {
  return {
    title,
    headerBackButtonMenuEnabled: false,
    headerBackVisible: false,
    headerLeft,
  }
}

export function getDrillDownScreenOptions(
  title: string,
  headerLeft: () => ReactNode
) {
  return {
    ...getBackButtonScreenOptions(title, headerLeft),
    animation: "simple_push" as const,
  }
}

export function getMediaDetailTransitionOptions(
  theme: NavigationThemeColors,
  headerLeft: () => ReactNode
) {
  return {
    ...getDefaultNativeStackOptions(theme),
    ...getBackButtonScreenOptions("", headerLeft),
    enableTransitions: true,
    ...Transition.Presets.ZoomIn(),
  }
}

export function getModalTaskTransitionOptions(
  theme: NavigationThemeColors,
  title: string,
  headerLeft: () => ReactNode
) {
  return {
    ...getDefaultNativeStackOptions(theme),
    ...getBackButtonScreenOptions(title, headerLeft),
    enableTransitions: true,
    ...Transition.Presets.SlideFromBottom(),
  }
}
