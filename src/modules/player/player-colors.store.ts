import { create } from "zustand"

export interface ColorPalette {
  bg: string
  primary: string
  secondary: string
}

const DEFAULT_COLORS: ColorPalette = {
  bg: "#1a1a1a",
  primary: "#cccccc",
  secondary: "#000000",
}

interface PlayerColorsState {
  currentImageUri: string | null
  currentColors: ColorPalette
  isLoadingColors: boolean
}

export const usePlayerColorsStore = create<PlayerColorsState>(() => ({
  currentImageUri: null,
  currentColors: DEFAULT_COLORS,
  isLoadingColors: false,
}))

export function getDefaultPlayerColors() {
  return DEFAULT_COLORS
}

export function getCurrentImageUriState() {
  return usePlayerColorsStore.getState().currentImageUri
}

export function setCurrentImageUriState(value: string | null) {
  usePlayerColorsStore.setState({ currentImageUri: value })
}

export function setCurrentColorsState(value: ColorPalette) {
  usePlayerColorsStore.setState({ currentColors: value })
}

export function setIsLoadingColorsState(value: boolean) {
  usePlayerColorsStore.setState({ isLoadingColors: value })
}
