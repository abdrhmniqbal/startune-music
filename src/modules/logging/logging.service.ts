/* eslint-disable no-console */
import { File, Paths } from "expo-file-system"
import { Share } from "react-native"

import {
  ensureLoggingConfigLoaded,
  getLoggingConfigState,
} from "./logging.store"

type LogSeverity = "debug" | "info" | "warn" | "error" | "critical"

const CRASH_LOG_FILE = new File(Paths.document, "crash-logs.txt")
const MAX_LOG_FILE_BYTES = 1_000_000
const MAX_SHARED_LOG_CHARS = 30_000

const originalConsole = {
  log: console.log.bind(console),
  info: console.info.bind(console),
  warn: console.warn.bind(console),
  error: console.error.bind(console),
  debug: console.debug.bind(console),
}

let writeQueue: Promise<void> = Promise.resolve()
let isConsoleBridgeInstalled = false
let isGlobalErrorHandlerInstalled = false

interface ErrorUtilsLike {
  getGlobalHandler?: () =>
    | ((error: unknown, isFatal?: boolean) => void)
    | undefined
  setGlobalHandler?: (
    handler: (error: unknown, isFatal?: boolean) => void
  ) => void
}

function shouldPersistLog(severity: LogSeverity): boolean {
  const config = getLoggingConfigState()
  if (config.level === "extra") {
    return true
  }

  return severity === "error" || severity === "critical"
}

function stringifyLogPayload(payload: unknown): string {
  if (payload instanceof Error) {
    const stack = payload.stack ? `\n${payload.stack}` : ""
    return `${payload.name}: ${payload.message}${stack}`
  }

  if (typeof payload === "string") {
    return payload
  }

  try {
    return JSON.stringify(payload)
  } catch {
    return String(payload)
  }
}

function formatLogEntry(
  severity: LogSeverity,
  message: string,
  context?: unknown
): string {
  const timestamp = new Date().toISOString()
  const contextText =
    context === undefined ? "" : `\ncontext: ${stringifyLogPayload(context)}`
  return `[${timestamp}] [${severity.toUpperCase()}] ${message}${contextText}\n`
}

function ensureLogFileExists() {
  if (!CRASH_LOG_FILE.exists) {
    CRASH_LOG_FILE.create({
      intermediates: true,
      overwrite: true,
    })
  }
}

async function appendToLogFile(content: string): Promise<void> {
  if (!content) {
    return
  }

  ensureLogFileExists()

  let previous = ""
  try {
    previous = await CRASH_LOG_FILE.text()
  } catch {
    previous = ""
  }

  let next = `${previous}${content}`
  if (next.length > MAX_LOG_FILE_BYTES) {
    next = next.slice(next.length - MAX_LOG_FILE_BYTES)
  }

  CRASH_LOG_FILE.write(next, { encoding: "utf8" })
}

function enqueueFileLog(
  severity: LogSeverity,
  message: string,
  context?: unknown
): void {
  if (!shouldPersistLog(severity)) {
    return
  }

  const entry = formatLogEntry(severity, message, context)
  writeQueue = writeQueue
    .then(() => appendToLogFile(entry))
    .catch(() => {
      // Intentionally swallow logging failures to avoid recursive crashes.
    })
}

function normalizeErrorMessage(message: string, error?: unknown): string {
  if (!error) {
    return message
  }

  if (error instanceof Error) {
    return `${message}: ${error.message}`
  }

  return `${message}: ${stringifyLogPayload(error)}`
}

function installConsoleBridge() {
  if (isConsoleBridgeInstalled) {
    return
  }

  isConsoleBridgeInstalled = true

  console.log = (...args: unknown[]) => {
    originalConsole.log(...args)
    enqueueFileLog("info", args.map(stringifyLogPayload).join(" "))
  }
  console.info = (...args: unknown[]) => {
    originalConsole.info(...args)
    enqueueFileLog("info", args.map(stringifyLogPayload).join(" "))
  }
  console.debug = (...args: unknown[]) => {
    originalConsole.debug(...args)
    enqueueFileLog("debug", args.map(stringifyLogPayload).join(" "))
  }
  console.warn = (...args: unknown[]) => {
    originalConsole.warn(...args)
    enqueueFileLog("warn", args.map(stringifyLogPayload).join(" "))
  }
  console.error = (...args: unknown[]) => {
    originalConsole.error(...args)
    enqueueFileLog("error", args.map(stringifyLogPayload).join(" "))
  }
}

function installGlobalErrorHandler() {
  if (isGlobalErrorHandlerInstalled) {
    return
  }

  const maybeErrorUtils = (globalThis as { ErrorUtils?: ErrorUtilsLike })
    ?.ErrorUtils
  if (
    !maybeErrorUtils?.getGlobalHandler ||
    !maybeErrorUtils?.setGlobalHandler
  ) {
    return
  }

  isGlobalErrorHandlerInstalled = true
  const previousHandler = maybeErrorUtils.getGlobalHandler()
  const activeGlobalErrorHandler = (error: unknown, isFatal?: boolean) => {
    const label = isFatal ? "Fatal JS error" : "Unhandled JS error"
    const message = normalizeErrorMessage(label, error)
    enqueueFileLog("critical", message, error)
    previousHandler?.(error, Boolean(isFatal))
  }

  maybeErrorUtils.setGlobalHandler(activeGlobalErrorHandler)
}

export async function initializeLogging(): Promise<void> {
  await ensureLoggingConfigLoaded()
  installConsoleBridge()
  installGlobalErrorHandler()
}

export function logInfo(message: string, context?: unknown): void {
  originalConsole.info(message, context)
  enqueueFileLog("info", message, context)
}

export function logWarn(message: string, context?: unknown): void {
  originalConsole.warn(message, context)
  enqueueFileLog("warn", message, context)
}

export function logError(
  message: string,
  error?: unknown,
  context?: unknown
): void {
  originalConsole.error(message, error, context)
  const fullMessage = normalizeErrorMessage(message, error)
  const mergedContext =
    context === undefined
      ? error
      : { error: stringifyLogPayload(error), context }
  enqueueFileLog("error", fullMessage, mergedContext)
}

export function logCritical(
  message: string,
  error?: unknown,
  context?: unknown
): void {
  originalConsole.error(message, error, context)
  const fullMessage = normalizeErrorMessage(message, error)
  const mergedContext =
    context === undefined
      ? error
      : { error: stringifyLogPayload(error), context }
  enqueueFileLog("critical", fullMessage, mergedContext)
}

export async function shareCrashLogs(): Promise<{
  shared: boolean
  reason?: string
}> {
  try {
    ensureLogFileExists()
    const raw = CRASH_LOG_FILE.exists ? await CRASH_LOG_FILE.text() : ""
    const trimmed = raw.trim()
    const logPayload = trimmed
      ? trimmed.slice(Math.max(0, trimmed.length - MAX_SHARED_LOG_CHARS))
      : "No crash logs captured yet."

    await Share.share({
      title: "Startune Music crash logs",
      message: logPayload,
    })

    return { shared: true }
  } catch (error) {
    logError("Failed to share crash logs", error)
    return { shared: false, reason: "Failed to open share sheet." }
  }
}
