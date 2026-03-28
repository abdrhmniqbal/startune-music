interface QueuedIndexerRun {
  requested: boolean
  forceFullScan: boolean
  showProgress: boolean
}

let abortController: AbortController | null = null
let runToken = 0
let completePhaseTimeout: ReturnType<typeof setTimeout> | null = null
let queuedRun: QueuedIndexerRun = {
  requested: false,
  forceFullScan: false,
  showProgress: false,
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

export function finishIndexerRunRuntime(controller: AbortController) {
  if (abortController === controller) {
    abortController = null
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
  queuedRun = {
    requested: false,
    forceFullScan: false,
    showProgress: false,
  }
  clearIndexerCompletePhaseTimeout()

  if (abortController) {
    abortController.abort()
    abortController = null
  }
}
