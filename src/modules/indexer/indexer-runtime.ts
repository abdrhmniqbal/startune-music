interface QueuedIndexerRun {
  requested: boolean
  forceFullScan: boolean
  showProgress: boolean
}

let abortController: AbortController | null = null
let runToken = 0
let completePhaseTimeout: ReturnType<typeof setTimeout> | null = null
let paused = false
let pauseWaiters: Array<() => void> = []
let queuedRun: QueuedIndexerRun = {
  requested: false,
  forceFullScan: false,
  showProgress: false,
}

export function isIndexerRunActive() {
  return abortController !== null
}

export function queueIndexerRun(forceFullScan: boolean, showProgress: boolean) {
  queuedRun = {
    requested: true,
    forceFullScan: queuedRun.forceFullScan || forceFullScan,
    showProgress: queuedRun.showProgress || showProgress,
  }
}

export function startIndexerRunRuntime() {
  clearIndexerCompletePhaseTimeout()
  const controller = new AbortController()
  abortController = controller
  runToken += 1
  paused = false
  pauseWaiters = []

  return {
    controller,
    runToken,
  }
}

export function isIndexerRunStale(
  controller: AbortController,
  currentRunToken: number
) {
  return controller.signal.aborted || currentRunToken !== runToken
}

export function scheduleIndexerCompletePhaseReset(
  currentRunToken: number,
  callback: () => void
) {
  clearIndexerCompletePhaseTimeout()
  completePhaseTimeout = setTimeout(() => {
    if (currentRunToken !== runToken) {
      return
    }

    callback()
    completePhaseTimeout = null
  }, 3000)
}

export function clearIndexerCompletePhaseTimeout() {
  if (!completePhaseTimeout) {
    return
  }

  clearTimeout(completePhaseTimeout)
  completePhaseTimeout = null
}

function flushPauseWaiters() {
  if (pauseWaiters.length === 0) {
    return
  }

  const waiters = pauseWaiters
  pauseWaiters = []
  waiters.forEach((resolve) => resolve())
}

export function finishIndexerRunRuntime(controller: AbortController) {
  if (abortController === controller) {
    abortController = null
    paused = false
    flushPauseWaiters()
  }
}

export function consumeQueuedIndexerRun(
  controller: AbortController,
  currentRunToken: number
) {
  const shouldRunQueuedScan =
    queuedRun.requested &&
    currentRunToken === runToken &&
    !controller.signal.aborted

  if (!shouldRunQueuedScan) {
    return null
  }

  const nextQueuedRun = {
    forceFullScan: queuedRun.forceFullScan,
    showProgress: queuedRun.showProgress,
  }
  queuedRun = {
    requested: false,
    forceFullScan: false,
    showProgress: false,
  }

  return nextQueuedRun
}

export function stopIndexerRunRuntime() {
  runToken += 1
  paused = false
  queuedRun = {
    requested: false,
    forceFullScan: false,
    showProgress: false,
  }
  clearIndexerCompletePhaseTimeout()
  flushPauseWaiters()

  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

export function isIndexerRunPaused() {
  return paused
}

export function pauseIndexerRunRuntime() {
  if (!abortController || paused) {
    return false
  }

  paused = true
  return true
}

export function resumeIndexerRunRuntime() {
  if (!paused) {
    return false
  }

  paused = false
  flushPauseWaiters()
  return true
}

export async function waitForIndexerResume(signal?: AbortSignal) {
  if (!paused) {
    return
  }

  if (signal?.aborted) {
    return
  }

  await new Promise<void>((resolve) => {
    const waiter = () => {
      if (signal) {
        signal.removeEventListener("abort", handleAbort)
      }
      resolve()
    }

    const handleAbort = () => {
      pauseWaiters = pauseWaiters.filter((candidate) => candidate !== waiter)
      resolve()
    }

    if (signal) {
      signal.addEventListener("abort", handleAbort, { once: true })
    }

    pauseWaiters.push(waiter)
  })
}
