import { create } from "zustand"

export type PlayerExpandedView = "artwork" | "lyrics" | "queue"

interface UIState {
  barsVisible: boolean
  isPlayerExpanded: boolean
  playerExpandedView: PlayerExpandedView
}

export const useUIStore = create<UIState>(() => ({
  barsVisible: true,
  isPlayerExpanded: false,
  playerExpandedView: "artwork",
}))

export const $barsVisible = {
  get: () => useUIStore.getState().barsVisible,
  set: (value: boolean) => useUIStore.setState({ barsVisible: value }),
}

export const $isPlayerExpanded = {
  get: () => useUIStore.getState().isPlayerExpanded,
  set: (value: boolean) => useUIStore.setState({ isPlayerExpanded: value }),
}

export const $playerExpandedView = {
  get: () => useUIStore.getState().playerExpandedView,
  set: (value: PlayerExpandedView) =>
    useUIStore.setState({ playerExpandedView: value }),
}

export function setBarsVisible(value: boolean) {
  $barsVisible.set(value)
}

export function setPlayerExpandedView(value: PlayerExpandedView) {
  $playerExpandedView.set(value)
}

export function openPlayer(view: PlayerExpandedView = "artwork") {
  useUIStore.setState({
    isPlayerExpanded: true,
    playerExpandedView: view,
  })
}

export function closePlayer() {
  useUIStore.setState({
    isPlayerExpanded: false,
    playerExpandedView: "artwork",
  })
}

export function togglePlayerExpandedView(value: PlayerExpandedView) {
  const currentView = useUIStore.getState().playerExpandedView
  $playerExpandedView.set(currentView === value ? "artwork" : value)
}

let lastScrollY = 0
let showTimeout: NodeJS.Timeout | null = null

export function handleScroll(currentY: number) {
  if (showTimeout) {
    clearTimeout(showTimeout)
    showTimeout = null
  }

  const isScrollingDown = currentY > lastScrollY && currentY > 50
  const isScrollingUp = currentY < lastScrollY

  if (isScrollingDown) {
    setBarsVisible(false)
  } else if (isScrollingUp) {
    setBarsVisible(true)
  }

  lastScrollY = currentY
}

export function handleScrollStart() {
  if (showTimeout) {
    clearTimeout(showTimeout)
    showTimeout = null
  }
}

export function handleScrollStop() {
  if (showTimeout) {
    clearTimeout(showTimeout)
  }
  showTimeout = setTimeout(() => {
    setBarsVisible(true)
  }, 150)
}
