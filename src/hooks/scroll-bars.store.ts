import { atom } from "nanostores"

export const $barsVisible = atom(true)
export const $isPlayerExpanded = atom(false)
export type PlayerExpandedView = "artwork" | "lyrics" | "queue"
export const $playerExpandedView = atom<PlayerExpandedView>("artwork")

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
    $barsVisible.set(false)
  } else if (isScrollingUp) {
    $barsVisible.set(true)
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
    $barsVisible.set(true)
  }, 150)
}
